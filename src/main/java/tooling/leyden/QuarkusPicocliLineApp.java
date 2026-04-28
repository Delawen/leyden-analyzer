package tooling.leyden;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;
import tooling.leyden.aotcache.Information;
import tooling.leyden.commands.DefaultCommand;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;

@QuarkusMain
@CommandLine.Command(name = "leyden-analyzer", mixinStandardHelpOptions = true)
public class QuarkusPicocliLineApp implements Runnable, QuarkusApplication {

    @Inject
    CommandLine.IFactory factory;

    @CommandLine.Option(arity = "1..*", paramLabel = "<file>", description = "files to load", names = {"productionLog"})
    private Path[] productionLog;
    @CommandLine.Option(arity = "1..*", paramLabel = "<file>", description = "files to load", names = {"trainingLog"})
    private Path[] trainingLog;
    @CommandLine.Option(arity = "1..*", paramLabel = "<file>", description = "files to load", names = {"aotCache"})
    private Path[] aotCache;
    @CommandLine.Option(arity = "1", paramLabel = "<file>", description = "file to load", names = {"instructions"})
    private Path instructions;

    private static Status status;
    private static Information information;
    public final static AtomicInteger loadingFiles = new AtomicInteger();

    private final static List<StatusMessage> statusMessages = Collections.synchronizedList(new ArrayList<>());

    public static void addStatusMessage(StatusMessage sm) {
        statusMessages.add(sm);
    }

    public static void updateStatus() {
        List<AttributedString> statusList = new ArrayList<>();
        statusMessages.removeIf(sm -> sm.timestamp() < System.currentTimeMillis() - 1000 * 10);
        for (StatusMessage sm : statusMessages) {
            statusList.add(sm.message());
        }

        if (loadingFiles.get() > 0) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.style(AttributedStyle.BOLD)
                    .append("Currently loading " + loadingFiles + " file(s) into the playground.");
            statusList.add(asb.toAttributedString());
        }

        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append("Playground contains: ");
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append(String.valueOf(information.getAll().size())).append(" assets");
        asb.style(AttributedStyle.DEFAULT).append(" | ");
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append(String.valueOf(information.getAllPackages().size())).append(" packages");
        asb.style(AttributedStyle.DEFAULT).append(" | ");
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append(String.valueOf(information.getAllTypes().size())).append(" asset types");
        asb.style(AttributedStyle.DEFAULT).append(" | ");
        asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                .append(String.valueOf(information.getWarnings().size())).append(" warnings")
                .toAttributedString();
        statusList.add(asb.toAttributedString());
        status.update(statusList, true);
    }

    @Override
    public void run() {
        try {
            Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
            // set up JLine built-in commands
            Builtins builtins = new Builtins(workDir, new ConfigurationPath(workDir.get(), workDir.get()), null);
            builtins.rename(Builtins.Command.TTOP, "top");

            DefaultCommand commands = new DefaultCommand();
            information = commands.getInformation();
            PicocliCommandsFactory factory = new PicocliCommandsFactory();

            CommandLine cmd = new CommandLine(commands, factory);
            cmd.setTrimQuotes(true);
            PicocliCommands picocliCommands = new PicocliCommands(cmd);

            Parser parser = new DefaultParser();
            try (Terminal terminal = TerminalBuilder.builder().nativeSignals(true)
                    .signalHandler(Terminal.SignalHandler.SIG_IGN).build()) {
                // Display banner
                printBanner(terminal);

                SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
                systemRegistry.setCommandRegistries(builtins, picocliCommands);
                systemRegistry.register("help", picocliCommands);

                status = Status.getStatus(terminal);
                status.setBorder(true);
                Executors.newSingleThreadScheduledExecutor()
                        .scheduleWithFixedDelay(QuarkusPicocliLineApp::updateStatus, 0, 1, SECONDS);

                final var historyFileName = ".leyden-analyzer.history";
                LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(systemRegistry.completer())
                        .history(new DefaultHistory())
                        .variable(LineReader.HISTORY_FILE,
                                Paths.get(workDir.get().resolve(
                                                historyFileName).toAbsolutePath().toString(),
                                        historyFileName))
                        .variable(LineReader.HISTORY_SIZE, 500) // Maximum entries in memory
                        .variable(LineReader.HISTORY_FILE_SIZE, 1000) // Maximum entries in file
                        .parser(parser)
                        .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
                        .build();

                // Don't add duplicate entries to history
                reader.setOpt(LineReader.Option.HISTORY_IGNORE_DUPS);

                // Don't add entries that start with space
                reader.setOpt(LineReader.Option.HISTORY_IGNORE_SPACE);
                builtins.setLineReader(reader);
                commands.setReader(reader);
                factory.setTerminal(terminal);

                String prompt = "> ";

                // start the shell and process input until the user quits with Ctrl-D
                String line;
                if (productionLog != null) {
                    for (Path p : productionLog) {
                        systemRegistry.execute("load --background productionLog " + p.toString());
                    }
                }
                if (trainingLog != null) {
                    for (Path p : trainingLog) {
                        systemRegistry.execute("load --background productionLog " + p.toString());
                    }
                }
                if (aotCache != null) {
                    for (Path p : aotCache) {
                        systemRegistry.execute("load --background aotCache " + p.toString());
                    }
                }
                boolean shouldContinue = true;
                if (instructions != null) {
                    AttributedStringBuilder builder = new AttributedStringBuilder();
                    builder.style(AttributedStyle.BOLD.italic());
                    builder.append("Waiting for files to be loaded");
                    builder.toAttributedString().println(terminal);
                    while (loadingFiles.get() > 0) {
                        Thread.sleep(500);
                    }
                    builder = new AttributedStringBuilder();
                    builder.style(AttributedStyle.BOLD.italic());
                    builder.append("Executing automated commands...");
                    builder.toAttributedString().println(terminal);
                    try (Scanner scanner = new Scanner(Files.newInputStream(instructions), StandardCharsets.UTF_8)) {
                        while (scanner.hasNextLine() && shouldContinue) {
                            String command = scanner.nextLine();
                            builder = new AttributedStringBuilder();
                            builder.style(AttributedStyle.BOLD.italic());
                            builder.append("[auto] > ").append(command);
                            builder.toAttributedString().println(terminal);
                            systemRegistry.execute(command);
                        }
                    } catch (EndOfFileException e) {
                        shouldContinue = false;
                    } catch (Exception e) {
                        builder = new AttributedStringBuilder();
                        builder.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
                        builder.append("[error] ").append(e.getMessage()).append(AttributedString.NEWLINE);
                        builder.toAttributedString().println(terminal);
                    }
                }
                while (shouldContinue) {
                    try {
                        systemRegistry.cleanUp();
                        line = reader.readLine(prompt, null, (MaskingCallback) null, null);
                        systemRegistry.execute(line);
                    } catch (UserInterruptException e) {
                        // Ignore
                    } catch (EndOfFileException e) {
                        shouldContinue = false;
                    } catch (Exception e) {
                        var builder = new AttributedStringBuilder();
                        builder.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
                        builder.append("[error] ").append(e.getMessage()).append(AttributedString.NEWLINE);
                        builder.toAttributedString().println(terminal);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void printBanner(Terminal terminal) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).bold())
                .append(AttributedString.NEWLINE);

        var banner = """
                            ██╗     ███████╗██╗   ██╗██████╗ ███████╗███╗   ██╗          \s
                            ██║     ██╔════╝╚██╗ ██╔╝██╔══██╗██╔════╝████╗  ██║          \s
                            ██║     █████╗   ╚████╔╝ ██║  ██║█████╗  ██╔██╗ ██║          \s
                            ██║     ██╔══╝    ╚██╔╝  ██║  ██║██╔══╝  ██║╚██╗██║          \s
                            ███████╗███████╗   ██║   ██████╔╝███████╗██║ ╚████║          \s
                            ╚══════╝╚══════╝   ╚═╝   ╚═════╝ ╚══════╝╚═╝  ╚═══╝          \s
                
                 █████╗  ██████╗ ████████╗     ██████╗ █████╗  ██████╗██╗  ██╗███████╗   \s
                ██╔══██╗██╔═══██╗╚══██╔══╝    ██╔════╝██╔══██╗██╔════╝██║  ██║██╔════╝   \s
                ███████║██║   ██║   ██║       ██║     ███████║██║     ███████║█████╗     \s
                ██╔══██║██║   ██║   ██║       ██║     ██╔══██║██║     ██╔══██║██╔══╝     \s
                ██║  ██║╚██████╔╝   ██║       ╚██████╗██║  ██║╚██████╗██║  ██║███████╗   \s
                ╚═╝  ╚═╝ ╚═════╝    ╚═╝        ╚═════╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚══════╝   \s
                
                     █████╗ ███╗   ██╗ █████╗ ██╗  ██╗   ██╗███████╗███████╗██████╗      \s
                    ██╔══██╗████╗  ██║██╔══██╗██║  ╚██╗ ██╔╝╚══███╔╝██╔════╝██╔══██╗     \s
                    ███████║██╔██╗ ██║███████║██║   ╚████╔╝   ███╔╝ █████╗  ██████╔╝     \s
                    ██╔══██║██║╚██╗██║██╔══██║██║    ╚██╔╝   ███╔╝  ██╔══╝  ██╔══██╗     \s
                    ██║  ██║██║ ╚████║██║  ██║███████╗██║   ███████╗███████╗██║  ██║     \s
                    ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚══════╝╚═╝   ╚══════╝╚══════╝╚═╝  ╚═╝     \s
                """;

        builder.append(banner).append(AttributedString.NEWLINE);

        builder.append("Use 'load' to add assets to the playground.")
                .append(AttributedString.NEWLINE);

        builder.append("Use 'help' to learn more commands.")
                .append(AttributedString.NEWLINE);

        builder.append("If you want to know more about the information shown, use the '-v' verbose argument.")
                .append(AttributedString.NEWLINE);

        builder.append("If you want to know how to use the information shown, use the '-hints' argument.")
                .append(AttributedString.NEWLINE);

        builder.toAttributedString().println(terminal);
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).execute(args);
    }

}
