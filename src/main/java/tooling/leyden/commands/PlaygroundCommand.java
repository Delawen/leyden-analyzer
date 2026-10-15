package tooling.leyden.commands;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jspecify.annotations.NonNull;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.*;
import tooling.leyden.commands.autocomplete.Playgrounds;
import tooling.leyden.commands.logparser.AOTMapParser;

import java.nio.file.Path;
import java.util.*;

@Command(name = "playground", mixinStandardHelpOptions = true, version = "1.0", description = {
        "Change, remove playgrounds" }, subcommands = { CommandLine.HelpCommand.class })
class PlaygroundCommand extends BaseCommand {

    @CommandLine.ParentCommand
    DefaultCommand parent;

    @CommandLine.Option(names = { "-v",
            "--verbose" }, arity = "0..1", negatable = true, description = "Display more information.", defaultValue = "false")
    protected Boolean verbose = false;


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
            @CommandLine.Parameters(arity = "1..1", paramLabel = "<playground>", description = "playground to move to",
                    completionCandidates = Playgrounds.class) String playground) {
        Information.getMyself().changePlayground(playground);
    }

    @Command(version = "1.0", subcommands = {
            CommandLine.HelpCommand.class }, description = "Show differences between playgrounds")
    public void diff(
            @CommandLine.Parameters(arity = "2..2", paramLabel = "<playground>", description = "playground to move to",
                    completionCandidates = Playgrounds.class) String[] playground) {
        setupHandle();

        io.quarkus.narayana.jta.QuarkusTransaction.joiningExisting().run(() -> {
            //Check playgrounds exist
            if (playground.length !=2 ) {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
                asb.append("This command needs two playgrounds to compare.");
                asb.toAttributedString().println(parent.getTerminal());
                return;
            }
            Playground playground1 = Information.getMyself().getPlayground(playground[0]);
            if (playground1 == null) {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
                asb.append("Couldn't find playground " + playground[0]);
                asb.toAttributedString().println(parent.getTerminal());
                return;
            }
            Playground playground2 = Information.getMyself().getPlayground(playground[1]);
            if (playground2 == null) {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
                asb.append("Couldn't find playground " + playground[1]);
                asb.toAttributedString().println(parent.getTerminal());
                return;
            }

            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.style(AttributedStyle::bold);
            asb.append(" COMPARING ");
            addPlayground(asb, playground[0]);
            asb.append(" WITH ");
            addPlayground(asb, playground[1]);
            asb.toAttributedString().println(parent.getTerminal());
            AttributedString.NEWLINE.println(parent.getTerminal());

            //Detailed Count
            printDetailedCountComparison(playground1, playground2).println(parent.getTerminal());
            AttributedString.NEWLINE.println(parent.getTerminal());

            //Classes comparison
            printClassesComparison(playground1, playground2).println(parent.getTerminal());
            AttributedString.NEWLINE.println(parent.getTerminal());

            //Methods compiled
            printMethodsCompiledComparison(playground1, playground2).println(parent.getTerminal());
            AttributedString.NEWLINE.println(parent.getTerminal());
        });
    }

    protected AttributedString printDetailedCountComparison(Playground playground1, Playground playground2) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        String format = "%20s";
        asb.style(AttributedStyle::bold);
        asb.append(String.format("%-20s", "Summary Count"));
        asb.append(String.format(format, playground1.getIdentifier()));
        asb.append(String.format(format, playground2.getIdentifier()));
        asb.append(AttributedString.NEWLINE);
        asb.style(AttributedStyle::boldOff);
        var detailedCount1 = Information.getMyself().getDetailedCount(playground1);
        var detailedCount2 = Information.getMyself().getDetailedCount(playground2);
        var detailedCount = new HashSet<>();
        detailedCount.addAll(detailedCount1.keySet());
        detailedCount.addAll(detailedCount2.keySet());
        detailedCount.stream().sorted().forEachOrdered(key -> {
            asb.append(String.format(format, key));
            Long value1 = detailedCount1.getOrDefault(key, 0l);
            Long value2 = detailedCount2.getOrDefault(key, 0l);
            printValues(value1, value2, asb, format);
            asb.append(AttributedString.NEWLINE);
        });
        return asb.toAttributedString();
    }

    private static void printValues(Long value1, Long value2, AttributedStringBuilder asb, String format) {
        if (value1 > value2) {
            asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
        } else if (value2 > value1){
            asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
        }
        asb.append(String.format(format, value1));
        if (value1 < value2) {
            asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
        } else if (value2 < value1){
            asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
        }
        asb.append(String.format(format, value2));
        asb.style(AttributedStyle.DEFAULT);
    }


    protected AttributedString printMethodsCompiledComparison(Playground playground1, Playground playground2) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        String format = "%1$25s";
        asb.style(AttributedStyle::bold);
        asb.append(String.format(format, "Compiled Methods by Level"));
        asb.append(String.format(format, playground1.getIdentifier()));
        asb.append(String.format(format, playground2.getIdentifier()));
        asb.style(AttributedStyle::boldOff);
        asb.append(AttributedString.NEWLINE);

        Map<Integer, Long> trainingCompilationLevels1 = parent.getInformation().getCompiledLevels(playground1);
        Map<Integer, Long> trainingCompilationLevels2 = parent.getInformation().getCompiledLevels(playground2);

        var trainingCompilationLevels = new HashSet<Integer>();
        trainingCompilationLevels.addAll(trainingCompilationLevels1.keySet());
        trainingCompilationLevels.addAll(trainingCompilationLevels2.keySet());

        trainingCompilationLevels.stream().sorted().forEachOrdered(key -> {
            asb.append(String.format(format, "Level " + key));
            var value1 = trainingCompilationLevels1.getOrDefault(key, 0l);
            var value2 = trainingCompilationLevels2.getOrDefault(key, 0l);
            printValues(value1, value2, asb, format);
            asb.append(AttributedString.NEWLINE);
        });

        return asb.toAttributedString();
    }


    protected AttributedString printClassesComparison(Playground playground1, Playground playground2) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        String format = "%20s";
        asb.style(AttributedStyle::bold);
        asb.append(String.format("%-20s", "Classes stored on the cache"));
        asb.append(String.format(format, playground1.getIdentifier()));
        asb.append(String.format(format, playground2.getIdentifier()));
        asb.style(AttributedStyle::boldOff);
        asb.append(AttributedString.NEWLINE);

        var params = new CommonParameters();
        params.setTypes(new String[] {"Class"});
        params.setUse(CommonParameters.ElementsToUse.cached);
        var classes1 = parent.getInformation().getElements(params, playground1)
                .map(e -> e.getKey()).toList();
        var classes2 = parent.getInformation().getElements(params, playground2)
                .map(e -> e.getKey()).toList();

        asb.append(String.format(format, ""));
        var value1 = (long) classes1.size();
        var value2 = (long) classes2.size();
        printValues(value1, value2, asb, format);
        asb.append(AttributedString.NEWLINE);
        asb.append(AttributedString.NEWLINE);

        if (this.verbose) {
            asb.style(AttributedStyle::bold);
            asb.append("Classes in ");
            asb.append(playground1.getIdentifier());
            asb.append(" and not in ");
            asb.append(playground2.getIdentifier());
            asb.append(":");
            asb.style(AttributedStyle::boldOff);
            asb.append(AttributedString.NEWLINE);
            diffBetweenLists(classes1, classes2, asb);
            asb.append(AttributedString.NEWLINE);

            asb.style(AttributedStyle::bold);
            asb.append("Classes in ");
            asb.append(playground2.getIdentifier());
            asb.append(" and not in ");
            asb.style(AttributedStyle::boldOff);
            asb.append(playground1.getIdentifier());
            asb.append(":");
            asb.append(AttributedString.NEWLINE);
            diffBetweenLists(classes2, classes1, asb);
            asb.append(AttributedString.NEWLINE);
        } else {
            asb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
            asb.append("  \uD83D\uDCA1  ");
            asb.append("Use the option --verbose to see the diff list of classes.");
            asb.style(AttributedStyle.DEFAULT);
            asb.append(AttributedString.NEWLINE);
        }

        return asb.toAttributedString();
    }

    private static void diffBetweenLists(List<String> list1, List<String> list2, AttributedStringBuilder asb) {
        var different = new HashSet<String>();
        different.addAll(list1);
        different.removeAll(list2);

        for (String s : different) {
            asb.append(s);
            asb.append(AttributedString.NEWLINE);
        }
    }


    private void addPlayground(AttributedStringBuilder asb, String identifier) {
        asb.append("'");
        if (identifier.equalsIgnoreCase(Information.getMyself().getPlayground())) {
            asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
        }
        asb.append(identifier);
        asb.style(AttributedStyle.DEFAULT);
        asb.append("'");
    }
}