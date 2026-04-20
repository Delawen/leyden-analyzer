package tooling.leyden.commands;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.Configuration;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.MethodObject;
import tooling.leyden.commands.autocomplete.InfoCommandTypes;
import tooling.leyden.commands.autocomplete.WhichRun;

@Command(name = "info", mixinStandardHelpOptions = true, version = "1.0", description = {
        "Show information and statistics on the AOT Cache based on the information we have loaded." }, subcommands = {
                CommandLine.HelpCommand.class })
class InfoCommand extends BaseCommand {

    @CommandLine.ParentCommand
    DefaultCommand parent;

    @CommandLine.Option(names = { "--type",
            "-t" }, arity = "0..*", description = "What type of statistics we want to show. By default, Summary.", defaultValue = "Summary", completionCandidates = InfoCommandTypes.class)
    private String[] whatToShow;

    @CommandLine.Option(names = {
            "-v" }, arity = "0..1", description = "Display inline information about each element.", defaultValue = "false")
    private Boolean verbose;

    @CommandLine.Option(names = {
            "-hints" }, arity = "0..1", description = "Display hints on how to explore further.", defaultValue = "false")
    private Boolean hints;

    private final NumberFormat intFormat = NumberFormat.getIntegerInstance();
    private final AttributedStyle greenFormat = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN);
    private final AttributedStyle blueFormat = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);
    private final AttributedStyle redFormat = AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);
    private final AttributedStyle infoFormat = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT);
    private final AttributedStyle tipFormat = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);

    public void execution() {
        if (shouldShow(InfoCommandTypes.Types.Configuration.name()))
            print(InfoCommandTypes.Types.Configuration.name(), parent.getInformation().getConfiguration());
        if (shouldShow(InfoCommandTypes.Types.Summary.name())) {
            printSummary();
        }
        if (shouldShow(InfoCommandTypes.Types.Count.name())) {
            count();
        }
    }

    public void count() {
        CommonParameters params = new CommonParameters();
        params.setUse(CommonParameters.ElementsToUse.both);
        params.setUseArrays(true);
        Stream<Element> elements = parent.getInformation().getElements(params);
        final var counts = new HashMap<String, AtomicInteger>();

        for (Element item : elements.toList()) {
            var value = counts.putIfAbsent(item.getType(), new AtomicInteger(1));
            if (value != null) {
                value.incrementAndGet();
            }
        }

        counts.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> parent.getOut()
                        .println(String.format("%1$25s", entry.getKey()) + " => " + entry.getValue().get()));
    }

    private boolean shouldShow(String s) {
        return this.whatToShow.length == 0
                || Arrays.asList(this.whatToShow).contains(s);
    }

    private void print(String title, Configuration configuration) {
        parent.getOut().println('\n' + title + ": ");
        parent.getOut().println("  _________");
        configuration.getKeys().stream().sorted()
                .forEachOrdered(key -> parent.getOut().println("  | " + key + " -> " +
                        configuration.getValue(key)));
        parent.getOut().println("  |________");
    }

    private void printSummary() {
        var stats = parent.getInformation().getStatistics();

        //Some formatting output utilities
        final var percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(2);
        percentFormat.setMinimumFractionDigits(2);

        //Get information from log
        var extClasses = Double.valueOf(stats.getValue("[LOG] Classes not loaded from AOT Cache", -1).toString());
        var extLambdas = Double.valueOf(stats.getValue("[LOG] Lambda Methods not loaded from AOT Cache", 0).toString());
        var classesLog = Double.valueOf(stats.getValue("[LOG] Classes loaded from AOT Cache", -1).toString());

        CommonParameters params = new CommonParameters();
        params.setUse(CommonParameters.ElementsToUse.cached);
        params.setUseArrays(false);
        params.setTypes(new String[] { "Class" });
        var futureClasses = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setLoaded(WhichRun.training);
        params.setUse(CommonParameters.ElementsToUse.cached);
        params.setUseArrays(false);
        params.setTypes(new String[] { "Class" });
        var futureClassesCachedNotUsed = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setUseArrays(true);
        params.setTypes(new String[] { "Object" });
        params.setUse(CommonParameters.ElementsToUse.cached);
        var futureObjectCount = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setUseArrays(true);
        params.setTypes(new String[] { "Object" });
        params.setUse(CommonParameters.ElementsToUse.cached);
        params.setShowAOTInited(true);
        var futureAotInited = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setUseArrays(true);
        params.setTypes(new String[] { "Object" });
        params.setUse(CommonParameters.ElementsToUse.cached);
        params.setShowAOTInited(null);
        params.setInstanceOf("java.lang.Class");
        var futureClassInstances = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setUseArrays(true);
        params.setTypes(new String[] { "Object" });
        params.setUse(CommonParameters.ElementsToUse.cached);
        params.setShowAOTInited(null);
        params.setInstanceOf("java.lang.String");
        var futureStringInstances = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setTypes(new String[] { "Method" });
        params.setUse(CommonParameters.ElementsToUse.cached);
        var futureMethodsSize = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setUse(CommonParameters.ElementsToUse.cached);
        params.setTypes(new String[] { "KlassTrainingData" });
        var futureKlassTrainingData = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setUse(CommonParameters.ElementsToUse.cached);
        params.setTypes(new String[] { "MethodCounters" });
        var futureMethodCounters = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setUse(CommonParameters.ElementsToUse.cached);
        params.setTypes(new String[] { "MethodData" });
        var futureMethodData = parent.getInformation().getFutureElements(params);
        params = new CommonParameters();
        params.setUse(CommonParameters.ElementsToUse.cached);
        params.setTypes(new String[] { "MethodTrainingData" });
        var futureMethodTrainingData = parent.getInformation().getFutureElements(params);

        var lambdas = Double.valueOf(stats.getValue("[LOG] Lambda Methods loaded from AOT Cache", 0).toString());
        final double methodsSize = getFutureDouble(futureMethodsSize);

        (new AttributedString("PRODUCTION RUN: ", blueFormat)).println(parent.getTerminal());
        printVerboseInfo("This section reflects how the production run performed.");
        double classes = getFutureDouble(futureClasses);
        if (extClasses < 0) {
            (new AttributedString(
                    "Production run information is missing.", redFormat)).println(parent.getTerminal());
            printVerboseTip("Please, load a log that represents a production run using the AOT Cache.");
            printVerboseTip("To create the log, you should at least use the following tags: " +
                    "-Xlog:class+load=info,aot+resolve*=trace,aot+codecache+exit=debug,aot=warning:file=./.....aot.log:level,tags");
        } else {
            (new AttributedString("Classes loaded: ", AttributedStyle.DEFAULT)).println(parent.getTerminal());
            printVerboseInfo("Classes used during production run.");
            printPercentage("  -> Cached:", classesLog + extClasses, percentFormat, greenFormat, classesLog);
            printVerboseInfo("Cached classes are the ones that were loaded from the AOT Cache. " +
                    "The higher the percentage here, the better.");
            printPercentage("  -> Not Cached:", classesLog + extClasses, percentFormat, redFormat, extClasses);
            printVerboseInfo("Classes that were used during production that were not loaded from the AOT Cache. ");
            printVerboseTip("You can find them with the command 'ls --loaded=production --use=notCached'. ");

            if (classes > 0) {
                printPercentage("  -> Cached and not used:", classes, percentFormat, greenFormat,
                        getFutureDouble(futureClassesCachedNotUsed));
                printVerboseInfo("These are classes that were used during training and stored on the AOT cache, but " +
                        "the production run didn't use them. They probably shouldn't have been stored. Check your " +
                        "training and look for testing classes or classes that don't belong to production configuration.");
                printVerboseTip("You can find them with the command 'ls --loaded=training --use=cached'. ");
            }

            printPercentage("Lambda Methods: ", methodsSize, percentFormat, greenFormat, lambdas + extLambdas);
            printVerboseInfo("How many lambda methods your production run used.");
            printVerboseInfo("Each lambda method is represented on the JVM with an inner class.");
            printPercentage("  -> Cached:", lambdas + extLambdas, percentFormat, greenFormat, lambdas);
            printVerboseInfo("Cached lambda methods are the ones that were loaded from the AOT Cache. " +
                    "The higher the percentage here, the better.");
            printPercentage("  -> Not Cached:", lambdas + extLambdas, percentFormat, redFormat, extLambdas);
            printVerboseInfo("Lambda methods used in production but not cached. Sometimes there are issues storing " +
                    "specific classes and methods. Check for warnings with the warning command.");

            Integer aotCodeEntries = Integer
                    .valueOf(stats.getValue("[LOG] [CodeCache] Loaded AOT code entries", -1).toString());
            printVerboseInfo("Summarized information of what has been used from the Code Cache.");
            if (aotCodeEntries > 0) {
                (new AttributedString(
                        "Code Entries: " + aotCodeEntries, AttributedStyle.DEFAULT)).println(parent.getTerminal());
                printVerboseInfo("Total number of entries loaded from the Code Cache.");
                printPercentage("  -> Adapters: ", aotCodeEntries.doubleValue(), percentFormat, greenFormat,
                        Double.valueOf(stats.getValue("[LOG] [CodeCache] Loaded Adapters", 0).toString()));
                printPercentage("  -> Shared Blobs: ", aotCodeEntries.doubleValue(), percentFormat,
                        greenFormat,
                        Double.valueOf(stats.getValue("[LOG] [CodeCache] Loaded Shared Blobs", 0).toString()));
                printPercentage("  -> C1 Blobs: ", aotCodeEntries.doubleValue(), percentFormat,
                        greenFormat, Double.valueOf(stats.getValue("[LOG] [CodeCache] Loaded C1 Blobs", 0).toString()));
                printPercentage("  -> C2 Blobs: ", aotCodeEntries.doubleValue(), percentFormat,
                        greenFormat, Double.valueOf(stats.getValue("[LOG] [CodeCache] Loaded C2 Blobs", 0).toString()));
                (new AttributedString(
                        "AOT code cache size: " + stats.getValue("[LOG] [CodeCache] AOT code cache size", 0),
                        AttributedStyle.DEFAULT)).println(parent.getTerminal());
            }
        }

        (new AttributedString("AOT CACHE: ", blueFormat)).println(parent.getTerminal());
        printVerboseInfo("Information on this section reflects what is inside the AOT Cache and how did the training " +
                "run perform.");

        if (classes < 1) {
            (new AttributedString("Classes information is missing.", redFormat))
                    .println(parent.getTerminal());
            printVerboseTip(
                    "Please, load an aot map file generated with -Xlog:aot+map=trace,aot+map+oops=trace:file=[...]aot.map:none:filesize=0 .");
        } else {
            (new AttributedString("Metadata: ", AttributedStyle.DEFAULT)).println(parent.getTerminal());
            printVerboseInfo(
                    "This is the information derived from the bytecode about classes, methods, and their relationships.");
            printVerboseInfo("It could be those classes and methods were used, or just referenced from, during training run.");
            (new AttributedString(" - Classes in AOT Cache: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
            (new AttributedString(intFormat.format(classes), greenFormat)).println(parent.getTerminal());
            printVerboseInfo("These are classes that were loaded during training run and stored on the AOT cache.");
            printVerboseTip("You can find them with the command 'ls --use=cached -t=Class'. ");
            printPercentage("    -> KlassTrainingData: ", classes, percentFormat, greenFormat,
                    getFutureDouble(futureKlassTrainingData));
            printVerboseInfo("This reflects how many classes have been trained.");
            (new AttributedString(" - Objects in AOT Cache: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
            var objectCount = getFutureDouble(futureObjectCount);
            (new AttributedString(intFormat.format(objectCount), greenFormat)).println(parent.getTerminal());
            printVerboseInfo("Objects are instances from classes that we were able to store on the AOT Cache.");
            printVerboseTip("You can find them with the command 'ls -t=Object'. ");
            if (objectCount > 0) {
                printPercentage("    -> AOT-inited: ", objectCount, percentFormat, greenFormat,
                        getFutureDouble(futureAotInited));
                printVerboseInfo("Instances that are already cl-inited, like setting up static fields.");
                printVerboseTip("You can find them with the command 'ls --showAOTInited=true -t=Object'. ");
                printPercentage("    -> java.lang.Class instances: ", objectCount, percentFormat, greenFormat,
                        getFutureDouble(futureClassInstances));
                printVerboseTip("You can find them with the command 'ls --instanceOf=java.lang.Class'. ");
                printPercentage("    -> java.lang.String instances: ", objectCount, percentFormat, greenFormat,
                        getFutureDouble(futureStringInstances));
                printVerboseTip("You can find them with the command 'ls --instanceOf=java.lang.String'. ");
            }
        }

        var methodCounters = getFutureDouble(futureMethodCounters);
        if (methodCounters < 1) {
            (new AttributedString(
                    "Method training information is missing. " +
                            AttributedString.NEWLINE +
                            "If you are using JDK26+, please load an aot map. " +
                            AttributedString.NEWLINE +
                            "If you are using JDK25, please upgrade your JDK to get this information.",
                    redFormat))
                    .println(parent.getTerminal());
        } else {
            (new AttributedString(" - Methods in AOT Cache: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
            (new AttributedString(intFormat.format(methodsSize), greenFormat)).println(parent.getTerminal());
            printVerboseInfo("Methods whose metadata has been stored on the Cache.");

            printPercentage("    -> MethodCounters: ", methodsSize, percentFormat, greenFormat,
                    methodCounters);
            printVerboseInfo("How many of those methods were run a significant amount of times.");
            printPercentage("    -> MethodData: ", methodsSize, percentFormat, greenFormat,
                    getFutureDouble(futureMethodData));
            printPercentage("    -> MethodTrainingData: ", methodsSize, percentFormat, greenFormat,
                    getFutureDouble(futureMethodTrainingData));
            printVerboseInfo("MethodData and MethodTrainingData shows how many of those methods were profiled " +
                    "and to what extent.");

            Map<Integer, Integer> compilationLevels = new HashMap<>();
            parent.getInformation().getElements(null, null, null, true, false, "Method")
                    .forEach(e -> {
                        MethodObject method = (MethodObject) e;
                        for (Map.Entry<Integer, Element> entry : method.getCompileTrainingData().entrySet()) {
                            compilationLevels.putIfAbsent(entry.getKey(), 0);
                            compilationLevels.replace(entry.getKey(), compilationLevels.get(entry.getKey()) + 1);
                        }
                    });

            (new AttributedString("  -> CompileTrainingData: ",
                    AttributedStyle.DEFAULT)).println(parent.getTerminal());
            printVerboseInfo("Information to compile methods to different levels/tiers of compilation.");
            for (Integer level : compilationLevels.keySet().stream().sorted().toList()) {
                printPercentage("      -> Level " + level + ": ", methodsSize, percentFormat,
                        greenFormat, Double.valueOf(compilationLevels.get(level)));
            }
            printVerboseInfo("The same method could store information to compile on more than one level. " +
                    "Percentages reflect the percentage of methods compiled to this level.");
            printVerboseTip("You can find compilation levels of any method with the command " +
                    "'describe -t=Method -i=\"void com.example.Class.method(com.example.Argument, com.example.Argument2)\"'.");

        }

        var aotCodeEntries = (Double) stats.getValue("[CodeCache] AOT Code Entries", -1.0);
        if (aotCodeEntries > 0) {

            (new AttributedString("Code Cache: ", AttributedStyle.DEFAULT)).println(parent.getTerminal());

            printPercentage("  -> None: ", aotCodeEntries,
                    percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] None", 0.0));
            printPercentage("  -> Adapter: ", aotCodeEntries,
                    percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] Adapter", 0.0));
            printPercentage("  -> Stub: ", aotCodeEntries,
                    percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] Stub", 0.0));
            printPercentage("  -> SharedBlob: ", aotCodeEntries,
                    percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] SharedBlob", 0.0));
            printPercentage("  -> C1Blob: ", aotCodeEntries,
                    percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] C1Blob", 0.0));
            printPercentage("  -> C2Blob: ", aotCodeEntries,
                    percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] C2Blob", 0.0));
            var nmethod = (Double) stats.getValue("[CodeCache] Nmethod", 0.0);
            printPercentage("  -> Nmethod: ", aotCodeEntries, percentFormat, greenFormat, nmethod);
            if (nmethod > 0) {
                printPercentage("     - Tier 0: ", nmethod,
                        percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] Nmethod Tier 0", 0.0));
                printPercentage("     - Tier 1: ", nmethod,
                        percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] Nmethod Tier 1", 0.0));
                printPercentage("     - Tier 2: ", nmethod,
                        percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] Nmethod Tier 2", 0.0));
                printPercentage("     - Tier 3: ", nmethod,
                        percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] Nmethod Tier 3", 0.0));
                printPercentage("     - Tier 4: ", nmethod,
                        percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] Nmethod Tier 4", 0.0));
                printPercentage("     - Tier 5: ", nmethod,
                        percentFormat, greenFormat, (Double) stats.getValue("[CodeCache] Nmethod Tier 5", 0.0));
            }
            (new AttributedString("  -> Entries: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
            (new AttributedString(intFormat.format(aotCodeEntries), greenFormat)).println(parent.getTerminal());

            (new AttributedString("  -> Cache Size: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
            (new AttributedString(
                    stats.getValue("[CodeCache] Cache Size", "unknown").toString(), greenFormat))
                    .println(parent.getTerminal());
        }
    }

    private static double getFutureDouble(Future<Stream<Element>> future) {
        double count;
        try {
            count = (double) future.get().count();
        } catch (Exception e) {
            count = -1;
        }
        return count;
    }

    private void printVerboseInfo(String info) {
        if (verbose) {
            (new AttributedString("  ℹ\uFE0F   " + info, infoFormat)).println(parent.getTerminal());
        }
    }

    private void printVerboseTip(String info) {
        if (hints) {
            (new AttributedString("  \uD83D\uDCA1  " + info, tipFormat)).println(parent.getTerminal());
        }
    }

    private void printPercentage(String title, Double total, NumberFormat percentFormat,
            AttributedStyle numStyle, Double partial) {
        (new AttributedString(title, AttributedStyle.DEFAULT)).print(parent.getTerminal());
        (new AttributedString(intFormat.format(partial), numStyle)).print(parent.getTerminal());
        (new AttributedString(" (", AttributedStyle.DEFAULT)).print(parent.getTerminal());
        (new AttributedString(percentFormat.format(partial / total), numStyle)).print(parent.getTerminal());
        (new AttributedString(")", AttributedStyle.DEFAULT)).println(parent.getTerminal());
    }
}
