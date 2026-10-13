package tooling.leyden.commands;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.Information;
import tooling.leyden.commands.logparser.AOTMapParser;

import java.nio.file.Path;

@Command(name = "playground", mixinStandardHelpOptions = true, version = "1.0", description = {
        "Change, remove playgrounds" }, subcommands = { CommandLine.HelpCommand.class })
class PlaygroundCommand extends BaseCommand {

    @CommandLine.ParentCommand
    DefaultCommand parent;

    @CommandLine.Option(names = { "-v",
            "--verbose" }, arity = "0..1", negatable = true, description = "Display more information.", defaultValue = "false")
    protected Boolean verbose;


    public void execution() {
        for (String identifier : Information.getMyself().getPlaygrounds()) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.append(" - ");
            addPlayground(asb, identifier);
            asb.toAttributedString().println(parent.getTerminal());
            if (this.verbose) {
                asb = new AttributedStringBuilder();
                for (String s : Information.getMyself().getPlayground(identifier).getSources()) {
                    asb.append("   -> ");
                    asb.append(s);
                    asb.append(AttributedString.NEWLINE);
                }
                asb.toAttributedString().print(parent.getTerminal());
            }
        }
    }

    @Command(version = "1.0", subcommands = {
            CommandLine.HelpCommand.class }, description = "Change playground")
    public void change(
            @CommandLine.Parameters(arity = "1..1", paramLabel = "<playground>", description = "playground to move to") String playground) {
        Information.getMyself().changePlayground(playground);
    }

    @Command(version = "1.0", subcommands = {
            CommandLine.HelpCommand.class }, description = "Show differences between playgrounds")
    public void diff(
            @CommandLine.Parameters(arity = "2..2", paramLabel = "<playground>", description = "playground to move to") String[] playground) {
        setupHandle();
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(" Comparing ");
        addPlayground(asb, playground[0]);
        asb.append(" with ");
        addPlayground(asb, playground[1]);
        asb.append(" : ");
        asb.toAttributedString().println(parent.getTerminal());

        //TODO
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
        asb.append(" NOT IMPLEMENTED YET ");
        asb.toAttributedString().println(parent.getTerminal());
    }

    private void addPlayground(AttributedStringBuilder asb, String identifier) {
        if (identifier.equalsIgnoreCase(Information.getMyself().getPlayground())) {
            asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
        }
        asb.append(identifier);
        asb.style(AttributedStyle.DEFAULT);
    }
}