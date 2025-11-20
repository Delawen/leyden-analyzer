package tooling.leyden.commands.logparser;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import tooling.leyden.aotcache.Information;
import tooling.leyden.commands.LoadFileCommand;

import java.util.function.Consumer;

/**
 * This class is capable of parsing (certain) Java logs.
 */
public abstract class Parser implements Consumer<String> {

	@Inject
	Information information;

	public Parser() {
	}

	abstract String getSource();

	public abstract void postProcessing();
}
