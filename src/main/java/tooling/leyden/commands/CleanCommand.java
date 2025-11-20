package tooling.leyden.commands;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.TransactionScoped;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.Information;

@Command(name = "clean", mixinStandardHelpOptions = true,
		version = "1.0",
		description = { "Empties the information loaded." },
		subcommands = { CommandLine.HelpCommand.class })
class CleanCommand implements Runnable {

	@CommandLine.ParentCommand
	DefaultCommand parent;

	Information information;

	public CleanCommand(Information information, DefaultCommand parent) {
		this.information = information;
		this.parent = parent;
	}

	public void run() {
		information.clear();
		parent.getOut().println("Cleaned the elements. Load again files to start a new analysis.");
	}
}