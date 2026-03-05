package tooling.leyden.commands;

import java.io.PrintWriter;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import tooling.leyden.aotcache.Information;

/**
 * Top-level command. All other commands should be subcommands of this.
 */
@CommandLine.Command(name = "", version = "1.0", description = {
        "Interactive shell to explore the contents of the AOT cache. Start by loading an AOT map file." }, footer = { "",
                "Press Ctrl-D to exit." }, subcommands = {
                        CleanCommand.class,
                        DescribeCommand.class,
                        WarningCommand.class,
                        InfoCommand.class,
                        ListCommand.class,
                        LoadFileCommand.class,
                        PicocliCommands.ClearScreen.class,
                        TreeCommand.class,
                        CommandLine.HelpCommand.class })
public class DefaultCommand implements Runnable {
    private PrintWriter out;
    private Information information = new Information();
    private Terminal terminal;

    public DefaultCommand() {
    }

    public void setReader(LineReader reader) {
        out = reader.getTerminal().writer();
        terminal = reader.getTerminal();
    }

    public void run() {
        out.println(new CommandLine(this).getUsageMessage());
    }

    public Information getInformation() {
        return information;
    }

    public PrintWriter getOut() {
        return out;
    }

    public Terminal getTerminal() {
        return terminal;
    }
}
