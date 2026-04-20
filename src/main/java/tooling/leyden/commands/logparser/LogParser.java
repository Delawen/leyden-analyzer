package tooling.leyden.commands.logparser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tooling.leyden.commands.LoadFileCommand;

/**
 * This class is capable of parsing (certain) Java logs.
 */
public abstract class LogParser extends Parser {
    final Pattern linePattern = Pattern
            .compile("(?<timestamp>\\[(?:\\d|,)+s\\])?\\[(?<level>\\w+)\\s*\\]\\[(?<tags>[^,\\]\\s]+(,[^,\\]\\s]+)*)\\s*\\](?<message>.*)");

    public LogParser(LoadFileCommand loadFile) {
        super(loadFile);
    }

    @Override
    public void accept(String content) {
        processLine(extractLineInformation(content));
    }

    abstract void processLine(Line line);

    Line extractLineInformation(String content) {
        String[] tags = new String[] {};
        String level = "unknown";
        String message = "";

        Matcher m = linePattern.matcher(content);
        if (m.matches()) {
            level = m.group("level");
            message = m.group("message");
            tags = m.group("tags")
                    .trim()
                    .split(",");
        }

        return new Line(content, tags, level, message, message.trim());
    }

    protected boolean containsTags(String[] tags, String... wantedTags) {
        return new HashSet<>(Arrays.asList(tags)).containsAll(Arrays.asList(wantedTags));
    }

    protected record Line(String content, String[] tags, String level, String message, String trimmedMessage) {
    }
}
