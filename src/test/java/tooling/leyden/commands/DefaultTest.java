package tooling.leyden.commands;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import tooling.leyden.QuarkusPicocliLineApp;
import tooling.leyden.aotcache.Information;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

@QuarkusTest
public class DefaultTest {
	@Inject
	CommandLine.IFactory factory;

	@Inject
	QuarkusPicocliLineApp app;

	DefaultCommand defaultCommand;

	@Inject
	Information information;

	private CommandLine cmd;

	protected CleanCommand cleanCommand;
	protected DescribeCommand describeCommand;
	protected InfoCommand infoCommand;
	protected ListCommand listCommand;
	protected LoadFileCommand loadFileCommand;
	protected TreeCommand treeCommand;
	protected WarningCommand warningCommand;

	@BeforeEach
	void clear() throws Exception {
		cmd = new CommandLine(app, factory);

		defaultCommand = cmd.getFactory().create(DefaultCommand.class);

		cleanCommand = cmd.getFactory().create(CleanCommand.class);
		describeCommand = cmd.getFactory().create(DescribeCommand.class);
		infoCommand = cmd.getFactory().create(InfoCommand.class);
		listCommand = cmd.getFactory().create(ListCommand.class);
		loadFileCommand = cmd.getFactory().create(LoadFileCommand.class);
		treeCommand = cmd.getFactory().create(TreeCommand.class);
		warningCommand = cmd.getFactory().create(WarningCommand.class);
	}

	protected void execute(String... args) {
		cmd.execute(args);
	}

	public Information getInformation() {
		return information;
	}

	public DefaultCommand getDefaultCommand() {
		return defaultCommand;
	}
}