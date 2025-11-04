package tooling.leyden.commands.logparser;

import tooling.leyden.aotcache.Information;
import tooling.leyden.commands.LoadFileCommand;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * This class is capable of parsing (certain) Java logs.
 */
public abstract class LogParser extends Parser {

	public LogParser(LoadFileCommand loadFile) {
		super(loadFile);
	}

	@Override
	public void accept(String content) {
		processLine(extractLineInformation(content));
	}

	abstract void processLine(Line line);

	private Line extractLineInformation(String content) {
		String[] tags = new String[]{};

		String level = null;
		if (content.indexOf("[") >= 0 && content.indexOf("]") > 0) {
			level = content.substring(content.indexOf("[") + 1, content.indexOf("]")).trim();
			content = content.substring(content.indexOf("]") + 1);
		}

		if (content.indexOf("[") >= 0 && content.indexOf("]") > 0) {
			tags = content.substring(content.indexOf("[") + 1, content.indexOf("]"))
					.trim()
					.split("\\s*,\\s*");
		}

		final String message = content.substring(content.indexOf("]") + 1);

		final var trimmedMessage = message.trim();
		return new Line(content, tags, level, message, trimmedMessage);
	}

	protected boolean containsTags(String[] tags, String... wantedTags) {
		return Arrays.asList(tags).containsAll(Arrays.asList(wantedTags));
	}

	protected record Line(String content, String[] tags, String level, String message, String trimmedMessage) {
	}
}
