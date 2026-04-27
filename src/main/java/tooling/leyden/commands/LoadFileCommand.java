package tooling.leyden.commands;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Stream;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.QuarkusPicocliLineApp;
import tooling.leyden.StatusMessage;
import tooling.leyden.commands.logparser.AOTMapParser;
import tooling.leyden.commands.logparser.Parser;
import tooling.leyden.commands.logparser.ProductionLogParser;
import tooling.leyden.commands.logparser.TrainingLogParser;

/**
 * Commands to load information about the AOT Cache into memory. This can be for example in the form of logs.
 */
@Command(name = "load", mixinStandardHelpOptions = true, version = "1.0", description = {
        "Load a file to extract information." }, subcommands = { CommandLine.HelpCommand.class })
public class LoadFileCommand extends BaseCommand {

    final private String logParameters = "-Xlog:class+load,aot+training=trace,aot+codecache*=trace," +
            "aot+resolve*=trace,aot=warning:file=aot.log:level,tags";

    @CommandLine.ParentCommand
    DefaultCommand parent;

    @CommandLine.Option(names = { "--background" }, description = { "Run this load in the background.",
            "This allows you to continue working while the file gets parsed." }, defaultValue = "false", arity = "0..1", scope = CommandLine.ScopeType.INHERIT)
    protected Boolean background = false;

    private final Thread.Builder builder = Thread.ofVirtual().name("loading-file-", 0);

    public void execution() {
        parent.getOut().println(new CommandLine(this).getUsageMessage());
    }

    private void load(Parser consumer, Path... files) {

        if (files != null) {
            for (Path file : files) {
                var filePath = file.toString();
                if (filePath.contains("*")) {
                    final var fileSeparator = File.separator;
                    //Are we sure there is no better way to do this??
                    var startPath = "";
                    if (!filePath.startsWith(fileSeparator)) {
                        startPath = Paths.get(System.getProperty("user.dir")).toString();
                    }
                    // Now, we don't want to get ALL the files in the startPath folder
                    // That may trigger access privilege exceptions
                    // and it can be stupidly long
                    while (filePath.contains(fileSeparator)
                            && filePath.indexOf("*") > filePath.indexOf(fileSeparator,
                                    filePath.indexOf(fileSeparator))) {
                        var nextFolder = filePath.substring(0, filePath.indexOf(fileSeparator));
                        startPath += (startPath.equals(fileSeparator) ? "" : fileSeparator) + nextFolder;
                        filePath = filePath.substring(filePath.indexOf(fileSeparator) + 1);
                    }

                    if (!startPath.endsWith(fileSeparator) && !filePath.startsWith(fileSeparator)) {
                        startPath += fileSeparator;
                    }

                    PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + startPath + filePath);
                    try (Stream<Path> pathStream = Files.find(Path.of(startPath), Integer.MAX_VALUE,
                            (path, f) -> pathMatcher.matches(path))) {
                        pathStream.forEach(p -> loadWithBackground(consumer, p));
                    } catch (Exception e) {
                        QuarkusPicocliLineApp.addStatusMessage(new StatusMessage(System.currentTimeMillis(),
                                new AttributedString("ERROR: Loading '" + file + "': " + e.getMessage(),
                                        AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED))));
                    }

                } else {
                    loadWithBackground(consumer, file);
                }
            }
        }
    }

    private void loadWithBackground(Parser consumer, Path p) {
        if (background) {
            builder.start(() -> load(p, consumer));
        } else {
            load(p, consumer);
        }
    }

    private void load(Path path, Parser consumer) {
        long time = System.currentTimeMillis();
        QuarkusPicocliLineApp.addStatusMessage(new StatusMessage(System.currentTimeMillis(),
                new AttributedString("Adding " + path.getFileName()
                        + "(" + path.getFileName() + ")"
                        + (background ? " in background " : " ")
                        + "to our analysis...",
                        AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN))));
        QuarkusPicocliLineApp.loadingFiles.incrementAndGet();

        long megabytes = Math.round((double) path.toFile().length() / 1024 / 1024);
        if (megabytes > 100 && !background) {
            new AttributedString("This is a big file. The size of this file is "
                    + megabytes + " MB. This may take a while.",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                    .println(parent.getTerminal());
            new AttributedString("Consider using the `--background` option to load this file.",
                    AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED))
                    .println(parent.getTerminal());
        }

        try (Scanner scanner = new Scanner(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine() && (background || isRunning())) {
                try {
                    consumer.accept(scanner.nextLine());
                } catch (Exception e) {
                    //Silently fails, we don't care about weirdly formatted log lines that seem similar
                    //to other loglines that we know how to process
                }
            }
            consumer.postProcessing();
            QuarkusPicocliLineApp.addStatusMessage(new StatusMessage(System.currentTimeMillis(),
                    new AttributedString("File " + path.getFileName()
                            + " added in " + (System.currentTimeMillis() - time) + "ms.",
                            AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN))));
        } catch (Exception e) {
            QuarkusPicocliLineApp.addStatusMessage(new StatusMessage(System.currentTimeMillis(),
                    new AttributedString("ERROR: Loading " + path.getFileName(),
                            AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED))));
        } finally {
            if(QuarkusPicocliLineApp.loadingFiles.decrementAndGet() < 0) {
                //In theory this should never happen because we are on the finally, BUT
                QuarkusPicocliLineApp.loadingFiles.set(0);
            }
        }
    }

    @Command(version = "1.0", subcommands = {
            CommandLine.HelpCommand.class }, description = "Load an AOT Cache map file generated with -Xlog:aot+map=trace:file=aot.map:none:filesize=0")
    public void aotCache(
            @CommandLine.Parameters(arity = "1..*", paramLabel = "<file>", description = "files to load") Path[] files) {
        setupHandle();
        load(new AOTMapParser(this), files);
    }

    @Command(version = "1.0", subcommands = {
            CommandLine.HelpCommand.class }, description = "Load a production log generated with " + logParameters)
    public void productionLog(
            @CommandLine.Parameters(arity = "1..*", paramLabel = "<file>", description = "files to load") Path[] files) {
        setupHandle();
        load(new ProductionLogParser(this), files);
    }

    @Command(version = "1.0", subcommands = {
            CommandLine.HelpCommand.class }, description = "Load a training (and assembly) log generated with " + logParameters)
    public void trainingLog(
            @CommandLine.Parameters(arity = "1..*", paramLabel = "<file>", description = "files to load") Path[] files) {
        setupHandle();
        load(new TrainingLogParser(this), files);
    }

    public DefaultCommand getParent() {
        return parent;
    }

    public void setParent(DefaultCommand parent) {
        this.parent = parent;
    }
}
