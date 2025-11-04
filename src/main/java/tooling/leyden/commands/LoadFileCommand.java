package tooling.leyden.commands;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.commands.logparser.AOTMapParser;
import tooling.leyden.commands.logparser.Parser;
import tooling.leyden.commands.logparser.ProductionLogParser;
import tooling.leyden.commands.logparser.TrainingLogParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Commands to load information about the AOT Cache into memory. This can be for example in the form of logs.
 */
@Command(name = "load", mixinStandardHelpOptions = true,
		version = "1.0",
		description = {"Load a file to extract information."},
		subcommands = {CommandLine.HelpCommand.class})
public class LoadFileCommand implements Runnable {

	final private String logParameters = "-Xlog:class+load,aot+training=trace,aot+codecache*=trace," +
			"aot+resolve*=trace," +
			"aot=warning:file=aot.log:level,tags";

	@CommandLine.ParentCommand
	DefaultCommand parent;

	@CommandLine.Option(names = {"--background"},
			description = {"Run this load in the background.",
					"This allows you to continue working while the file gets parsed."},
			defaultValue = "false",
			arity = "0..1",
			scope = CommandLine.ScopeType.INHERIT)
	protected Boolean background= false;

	public void run() {
	}

	private void load(Parser consumer, Path... files) {

		if (files != null) {
			for (Path file : files) {
				if (background) {
					new Thread(() -> load(file, consumer)).start();
				} else {
					load(file, consumer);
				}
			}
		}
	}

	private void load(Path path, Parser consumer) {
		long time = System.currentTimeMillis();
		parent.getOut().println("Adding " + path.toAbsolutePath().getFileName()
				+ (background ? " in background " : " ")
				+ "to our analysis...");

		long megabytes = Math.round((double) path.toFile().length() / 1024 / 1024);
		if (megabytes > 100) {
			new AttributedString("This is a big file. The size of this file is "
					+ megabytes + " MB. This may take a while.",
					AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
					.println(parent.getTerminal());
			if (!background) {
				new AttributedString("Consider using the `--background` option to load this file.",
						AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED))
						.println(parent.getTerminal());
			}
		}
		if (background) {
			new AttributedString("A message will be displayed when the loading finishes.",
					AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE))
					.println(parent.getTerminal());
		}

		try (Scanner scanner = new Scanner(Files.newInputStream(path), StandardCharsets.UTF_8)) {
			while (scanner.hasNextLine()) {
				try {
					consumer.accept(scanner.nextLine());
				} catch (Exception e) {
					//Silently fails, we don't care about weirdly formatted log lines and seem similar
					//to other loglines that we know how to process
				}
			}
			consumer.postProcessing();
		} catch (Exception e) {
			(new AttributedString("ERROR: Loading " + path.getFileName(),
					AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED))).println(parent.getTerminal());
			(new AttributedString(e.getMessage(),
					AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED))).println(parent.getTerminal());
		}

		AttributedString attributedString = new AttributedString("File " + path.toAbsolutePath().getFileName()
				+ " added in " + (System.currentTimeMillis() - time) + "ms.",
				AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
		attributedString.println(parent.getTerminal());
	}

	@Command(
			version = "1.0",
			subcommands = {CommandLine.HelpCommand.class},
			description = "Load an AOT Cache map file generated with -Xlog:aot+map=trace:file=aot.map:none:filesize=0")
	public void aotCache(
			@CommandLine.Parameters(
					arity = "1..*",
					paramLabel = "<file>",
					description = "files to load") Path[] files) {
		load(new AOTMapParser(this), files);
	}

	@Command(
			version = "1.0",
			subcommands = {CommandLine.HelpCommand.class},
			description = "Load a production log generated with " + logParameters
	)
	public void productionLog(
			@CommandLine.Parameters(
					arity = "1..*",
					paramLabel = "<file>",
					description = "files to load") Path[] files) {
		load(new ProductionLogParser(this), files);
	}

	@Command(
			version = "1.0",
			subcommands = {CommandLine.HelpCommand.class},
			description = "Load a training (and assembly) log generated with " + logParameters
	)
	public void trainingLog(
			@CommandLine.Parameters(
					arity = "1..*",
					paramLabel = "<file>",
					description = "files to load") Path[] files) {
		load(new TrainingLogParser(this), files);
	}

	public DefaultCommand getParent() {
		return parent;
	}

	public void setParent(DefaultCommand parent) {
		this.parent = parent;
	}
}