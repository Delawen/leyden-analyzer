package tooling.leyden;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.fusesource.jansi.AnsiConsole;
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
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;
import tooling.leyden.aotcache.Information;
import tooling.leyden.commands.DefaultCommand;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;

@QuarkusMain
@CommandLine.Command(name = "leyden-analyzer", mixinStandardHelpOptions = true)
public class QuarkusPicocliLineApp implements Runnable, QuarkusApplication {

	@Inject
	CommandLine.IFactory factory;

	private static Status status;
	private static Information information;

	public static void updateStatus() {
		if (status != null && information != null) {
			AttributedStringBuilder asb = new AttributedStringBuilder();
			// Update the status line
			asb.append("Our Playground contains: ");

			asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
					.append(information.getAll().size() + " elements");
			asb.style(AttributedStyle.DEFAULT).append(" | ");
			asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
					.append(information.getAllPackages().size() + " packages");
			asb.style(AttributedStyle.DEFAULT).append(" | ");
			asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
					.append(information.getAllTypes().size() + " element types");
			asb.style(AttributedStyle.DEFAULT).append(" | ");
			asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
					.append(information.getWarnings().size() + " warnings")
					.toAttributedString();

			status.update(Collections.singletonList(asb.toAttributedString()));
		}
	}

	@Override
	public void run() {
		AnsiConsole.systemInstall();
		try {
			Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
			// set up JLine built-in commands
			Builtins builtins = new Builtins(workDir, new ConfigurationPath(workDir.get(), workDir.get()), null);
			builtins.rename(Builtins.Command.TTOP, "top");

			DefaultCommand commands = new DefaultCommand();
			information = commands.getInformation();
			PicocliCommandsFactory factory = new PicocliCommandsFactory();

			CommandLine cmd = new CommandLine(commands, factory);
			PicocliCommands picocliCommands = new PicocliCommands(cmd);

			Parser parser = new DefaultParser();
			try (Terminal terminal = TerminalBuilder.builder().build()) {
				// Display banner
				printBanner(terminal);

				SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
				systemRegistry.setCommandRegistries(builtins, picocliCommands);
				systemRegistry.register("help", picocliCommands);

				status = Status.getStatus(terminal);
				new Thread(() ->
						Executors.newScheduledThreadPool(1)
								.scheduleAtFixedRate(() -> updateStatus(), 0, 1, SECONDS)).start();

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
				while (true) {
					try {
						systemRegistry.cleanUp();
						line = reader.readLine(prompt, null, (MaskingCallback) null, null);
						systemRegistry.execute(line);
					} catch (UserInterruptException e) {
						// Ignore
					} catch (EndOfFileException e) {
						return;
					} catch (Exception e) {
						systemRegistry.trace(e);
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			AnsiConsole.systemUninstall();
		}
	}

	private void printBanner(Terminal terminal) {
		AttributedStringBuilder builder = new AttributedStringBuilder();
		builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).bold())
				.append("\n")
				.append("◢◤ Leyden Analyzer ")
				.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
				.append("__________________________________________")
				.append("\n\n");

		terminal.writer().println(builder.toAnsi());
		terminal.flush();
	}

	@Override
	public int run(String... args) throws Exception {
		return new CommandLine(this, factory).execute(args);
	}

}