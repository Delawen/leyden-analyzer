package tooling.leyden.commands.logparser;

import tooling.leyden.aotcache.Information;
import tooling.leyden.commands.LoadFileCommand;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * This class is capable of parsing (certain) Java logs.
 */
public abstract class Parser implements Consumer<String> {

	protected final Information information;
	protected final LoadFileCommand loadFile;

	public Parser(LoadFileCommand loadFile) {
		this.information = loadFile.getParent().getInformation();
		this.loadFile = loadFile;
	}

	abstract String getSource();

	public abstract void postProcessing();
}
