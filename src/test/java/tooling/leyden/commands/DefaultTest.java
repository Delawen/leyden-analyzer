package tooling.leyden.commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

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

import io.quarkus.test.junit.QuarkusTest;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

@QuarkusTest
public class DefaultTest {

    private static SystemRegistry systemRegistry;

    private static DefaultCommand defaultCommand;

    public static DefaultCommand getDefaultCommand() {
        return defaultCommand;
    }

    public static SystemRegistry getSystemRegistry() {
        return systemRegistry;
    }

    @BeforeEach
    void clear() {
        getDefaultCommand().getInformation().clear();
    }

    @BeforeAll
    static void readLog() {

        Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
        // set up JLine built-in commands
        Builtins builtins = new Builtins(workDir, new ConfigurationPath(workDir.get(), workDir.get()), null);
        builtins.rename(Builtins.Command.TTOP, "top");

        defaultCommand = new DefaultCommand();
        PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory();

        CommandLine cmd = new CommandLine(defaultCommand, factory);
        PicocliCommands picocliCommands = new PicocliCommands(cmd);

        Parser parser = new DefaultParser();
        try (Terminal terminal = TerminalBuilder.builder()
                .name("testTerminal")
                .encoding(StandardCharsets.UTF_8).build()) {
            systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
            systemRegistry.setCommandRegistries(builtins, picocliCommands);

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(systemRegistry.completer())
                    .parser(parser)
                    .build();

            builtins.setLineReader(reader);
            defaultCommand.setReader(reader);
            factory.setTerminal(terminal);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
