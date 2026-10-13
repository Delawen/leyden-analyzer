package tooling.leyden.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "clean", mixinStandardHelpOptions = true, version = "1.0", description = {
        "Empties the information loaded." }, subcommands = { CommandLine.HelpCommand.class })
class CleanCommand extends BaseCommand {

    @CommandLine.ParentCommand
    DefaultCommand parent;

    @CommandLine.Option(names = { "--playground" }, description = {
            "Only remove information related to this playground" }, paramLabel = "<playground>")
    String playground;

    public void execution() {
        if (playground == null || playground.isBlank()) {
            parent.getInformation().clear();
        } else {
            parent.getInformation().clear(playground);
        }
        parent.getOut().println("Cleaned the elements. Load again files to start a new analysis.");
    }
}