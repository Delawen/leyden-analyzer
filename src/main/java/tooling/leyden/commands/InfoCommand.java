package tooling.leyden.commands;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.TransactionScoped;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.CompileTrainingData;
import tooling.leyden.aotcache.Configuration;
import tooling.leyden.aotcache.ConstantPoolObject;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.Information;
import tooling.leyden.aotcache.MethodObject;
import tooling.leyden.aotcache.ReferencingElement;
import tooling.leyden.commands.autocomplete.InfoCommandTypes;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Command(name = "info", mixinStandardHelpOptions = true,
		version = "1.0",
		description = {"Show information and statistics on the AOT Cache based on the information we have loaded."},
		subcommands = {CommandLine.HelpCommand.class})
class InfoCommand implements Runnable {

	@CommandLine.ParentCommand
	DefaultCommand parent;

	@CommandLine.Option(names = {"--type", "-t"},
			arity = "0..*",
			description = "What type of statistics we want to show. By default, Summary.",
			defaultValue = "Summary",
			completionCandidates = InfoCommandTypes.class)
	private String[] whatToShow;

	public InfoCommand(DefaultCommand parent) {
		this.parent = parent;
	}

	public void run() {
		if (shouldShow(InfoCommandTypes.Types.Configuration.name()))
			print(InfoCommandTypes.Types.Configuration.name(), parent.getInformation().getConfiguration());
		if (shouldShow(InfoCommandTypes.Types.Summary.name())) {
			printSummary();
		} if (shouldShow(InfoCommandTypes.Types.Count.name())) {
			count();
		}
	}

	public void count() {
		Stream<Element> elements = parent.getInformation().getElements(null, null, null, CommonParameters.ElementsToUse.both, null);
		final var counts = new HashMap<String, AtomicInteger>();

		elements.forEach(item -> {
			counts.putIfAbsent(item.getType(), new AtomicInteger());
			counts.get(counts.get(item.getType()).incrementAndGet());
		});

		counts.entrySet().
				stream().
				sorted(Map.Entry.comparingByKey())
				.forEach(entry ->
						parent.getOut().
								println("There are " + entry.getValue().get()
										+ " elements of type " + entry.getKey() + "."));
	}

	private boolean shouldShow(String s) {
		return this.whatToShow.length == 0
				|| Arrays.stream(this.whatToShow).anyMatch(wts -> wts.equals(s));
	}

	private void print(String title, Configuration configuration) {
		parent.getOut().println('\n' + title + ": ");
		parent.getOut().println("  _________");
		configuration.getKeys().stream().sorted()
				.forEachOrdered(key ->
						parent.getOut().println("  | " + key + " -> " +
								configuration.getValue(key)));
		parent.getOut().println("  |________");
	}

	private void printSummary() {
		var stats = parent.getInformation().getStatistics();

		//Some formatting output utilities
		final var percentFormat = NumberFormat.getPercentInstance();
		percentFormat.setMaximumFractionDigits(2);
		percentFormat.setMinimumFractionDigits(2);
		final var intFormat = NumberFormat.getIntegerInstance();
		final var greenFormat =
				AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN);
		final var blueFormat =
				AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.BLUE);
		final var redFormat =
				AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED);

		//Get information from log
		var extClasses = Double.valueOf(stats.getValue("[LOG] Classes not loaded from AOT Cache", -1).toString());
		var extLambdas = Double.valueOf(stats.getValue("[LOG] Lambda Methods not loaded from AOT Cache", 0).toString());
		var classes = Double.valueOf(stats.getValue("[LOG] Classes loaded from AOT Cache", -1).toString());
		if (classes < 0) {
			classes = (double) parent.getInformation().getElements(null, null, null, CommonParameters.ElementsToUse.cached, "Class").count();
		}
		var lambdas = Double.valueOf(stats.getValue("[LOG] Lambda Methods loaded from AOT Cache", 0).toString());
		final double methodsSize = parent.getInformation().getElements(null, null, null, CommonParameters.ElementsToUse.cached, "Method").count();

		(new AttributedString("RUN SUMMARY: ", blueFormat)).println(parent.getTerminal());
		if (extClasses < 0) {
			(new AttributedString(
					"Loading app information is missing. Please, load a log that represents the loading of the app using the AOT Cache.",
					redFormat))
					.println(parent.getTerminal());
		} else {
			(new AttributedString("Classes loaded: ", AttributedStyle.DEFAULT)).println(parent.getTerminal());
			printPercentages(intFormat, classes, percentFormat, extClasses, greenFormat, redFormat);

			(new AttributedString(
					"Lambda Methods loaded: ",
					AttributedStyle.DEFAULT)).println(parent.getTerminal());
			printPercentages(intFormat, lambdas, percentFormat, extLambdas, greenFormat, redFormat);


			if (methodsSize > 0) {
				printPercentage("  -> Portion of methods that are lambda: ", methodsSize, percentFormat,
						intFormat,
						greenFormat, lambdas + extLambdas);
			}

			Integer aotCodeEntries =
					Integer.valueOf(stats.getValue("[LOG] [CodeCache] Loaded AOT code entries", -1).toString());
			if (aotCodeEntries > 0) {
				(new AttributedString(
						"Code Entries: " + aotCodeEntries, AttributedStyle.DEFAULT)).println(parent.getTerminal());
				printPercentage("  -> Adapters: ", aotCodeEntries.doubleValue(), percentFormat, intFormat, greenFormat,
						Double.valueOf(stats.getValue("[LOG] [CodeCache] Loaded Adapters", 0).toString()));
				printPercentage("  -> Shared Blobs: ", aotCodeEntries.doubleValue(), percentFormat, intFormat,
						greenFormat,
						Double.valueOf(stats.getValue("[LOG] [CodeCache] Loaded Shared Blobs", 0).toString()));
				printPercentage("  -> C1 Blobs: ", aotCodeEntries.doubleValue(), percentFormat, intFormat,
						greenFormat, Double.valueOf(stats.getValue("[LOG] [CodeCache] Loaded C1 Blobs", 0).toString()));
				printPercentage("  -> C2 Blobs: ", aotCodeEntries.doubleValue(), percentFormat, intFormat,
						greenFormat, Double.valueOf(stats.getValue("[LOG] [CodeCache] Loaded C2 Blobs", 0).toString()));
				(new AttributedString(
						"AOT code cache size: " + stats.getValue("[LOG] [CodeCache] AOT code cache size", 0),
						AttributedStyle.DEFAULT)).println(parent.getTerminal());
			}
		}

		(new AttributedString("AOT CACHE SUMMARY: ", blueFormat)).println(parent.getTerminal());

		if (classes < 1) {
			(new AttributedString("Classes information is missing. Please, load an aot map.", redFormat))
					.println(parent.getTerminal());
		} else {

			Long trainingData =
					parent.getInformation().getElements(null, null, null, CommonParameters.ElementsToUse.cached, "KlassTrainingData")
							//Remove the training data that is not linked to anything
							.filter(ktd -> !((ReferencingElement) ktd).getReferences().isEmpty())
							.count();

			(new AttributedString("Classes in AOT Cache: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
			(new AttributedString(intFormat.format(classes), greenFormat)).println(parent.getTerminal());
			printPercentage("  -> KlassTrainingData: ", classes.doubleValue(), percentFormat, intFormat, greenFormat,
					trainingData.doubleValue());
			(new AttributedString("Objects in AOT Cache: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
			(new AttributedString(intFormat.format(parent.getInformation().getElements(null, null, null, CommonParameters.ElementsToUse.cached,
					"Object").count()), greenFormat)).println(parent.getTerminal());
		}

		Long methodCounters =
				parent.getInformation().getElements(null, null, null, CommonParameters.ElementsToUse.cached, "MethodCounters")                        //Remove the training data that is not linked to anything
						.filter(ktd -> !((ReferencingElement) ktd).getReferences().isEmpty())
						.count();

		if (methodCounters < 1) {
			(new AttributedString("Method training information is missing. Please, load an aot map.", redFormat))
					.println(parent.getTerminal());
		} else {
			Long methodData =
					parent.getInformation().getElements(null, null, null, CommonParameters.ElementsToUse.cached, "MethodData")                        //Remove the training data that is not linked to anything
							.filter(ktd -> !((ReferencingElement) ktd).getReferences().isEmpty())
							.count();
			Long methodTrainingData =
					parent.getInformation().getElements(null, null, null, CommonParameters.ElementsToUse.cached, "MethodTrainingData")                        //Remove the training data that is not linked to anything
							.filter(ktd -> !((ReferencingElement) ktd).getReferences().isEmpty())
							.count();

			(new AttributedString("Methods in AOT Cache: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
			(new AttributedString(intFormat.format(methodsSize), greenFormat)).println(parent.getTerminal());

			printPercentage("  -> MethodCounters: ", methodsSize, percentFormat, intFormat, greenFormat,
					methodCounters.doubleValue());
			printPercentage("  -> MethodData: ", methodsSize, percentFormat, intFormat, greenFormat,
					methodData.doubleValue());
			printPercentage("  -> MethodTrainingData: ", methodsSize, percentFormat, intFormat, greenFormat,
					methodTrainingData.doubleValue());

			Map<Integer, Integer> compilationLevels = new HashMap<>();
			parent.getInformation().getElements(null, null, null, CommonParameters.ElementsToUse.cached, "Method")
					.forEach(e -> {
						MethodObject method = (MethodObject) e;
						for (CompileTrainingData ctd : method.getCompileTrainingData()) {
							compilationLevels.putIfAbsent(ctd.getLevel(), 0);
							compilationLevels.replace(ctd.getLevel(), compilationLevels.get(ctd.getLevel()) + 1);
						}
					});

			(new AttributedString("  -> CompileTrainingData: ",
					AttributedStyle.DEFAULT)).println(parent.getTerminal());
			for (Integer level : compilationLevels.keySet()) {
				printPercentage("      -> Level " + level + ": ", methodsSize, percentFormat, intFormat,
						greenFormat, Double.valueOf(compilationLevels.get(level)));
			}
		}


//		(new AttributedString("Symbol: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
//		(new AttributedString(intFormat.format(
//				parent.getInformation().getElements(null, null, null, true, false, "Symbol").size()), greenFormat)).println(parent.getTerminal());

//		Integer constantPool =
//				parent.getInformation().getElements(null, null, null, true, false, "ConstantPool").size();
//
//		if (constantPool > 0) {
//			(new AttributedString("ConstantPool elements: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
//			(new AttributedString(intFormat.format(constantPool), greenFormat)).println(parent.getTerminal());
//		} else {
//			(new AttributedString("ConstantPool information is missing. Please, load an aot map.", redFormat))
//					.println(parent.getTerminal());
//		}


//		(new AttributedString("Type Arrays: ", AttributedStyle.DEFAULT)).println(parent.getTerminal());
//		parent.getInformation().getAllTypes().stream().filter(t -> t.startsWith("TypeArray")).sorted().forEachOrdered(type -> {
//			(new AttributedString("  -> " + type + ": ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
//			(new AttributedString(
//					intFormat.format(
//							parent.getInformation()
//									.getElements(null, null, null, true, false, type)
//									.size()), greenFormat)).println(parent.getTerminal());
//		});


//		(new AttributedString("Adapters: ", AttributedStyle.DEFAULT)).println(parent.getTerminal());
//		(new AttributedString("  -> AdapterFingerPrint: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
//		(new AttributedString(
//				intFormat.format(
//						parent.getInformation()
//								.getElements(null, null, null, true, false, "AdapterFingerPrint")
//								.count()), greenFormat)).println(parent.getTerminal());
//		(new AttributedString("  -> AdapterHandlerEntry: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
//		(new AttributedString(
//				intFormat.format(
//						parent.getInformation()
//								.getElements(null, null, null, true, false, "AdapterHandlerEntry")
//								.count()), greenFormat)).println(parent.getTerminal());

//		(new AttributedString("RecordComponent: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
//		(new AttributedString(intFormat.format(
//				parent.getInformation().getElements(null, null, null, true, false, "RecordComponent").count()), greenFormat)).println(parent.getTerminal());

//		(new AttributedString("Annotations: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
//		(new AttributedString(intFormat.format(
//				parent.getInformation().getElements(null, null, null, true, false, "Annotations").size()),
//				greenFormat)).println(parent.getTerminal());


//		(new AttributedString("Misc Data: ", AttributedStyle.DEFAULT)).print(parent.getTerminal());
//		(new AttributedString(intFormat.format(parent.getInformation()
//				.getElements(null, null, null, true, false, "Misc-data")
//				.count()), greenFormat)).println(parent.getTerminal());

	}

	private void printPercentage(String title, Double total, NumberFormat percentFormat, NumberFormat intFormat,
								 AttributedStyle numStyle, Double partial) {
		(new AttributedString(title, AttributedStyle.DEFAULT)).print(parent.getTerminal());
		(new AttributedString(intFormat.format(partial), numStyle)).print(parent.getTerminal());
		(new AttributedString(" (", AttributedStyle.DEFAULT)).print(parent.getTerminal());
		(new AttributedString(percentFormat.format(partial / total), numStyle)).print(parent.getTerminal());
		(new AttributedString(")", AttributedStyle.DEFAULT)).println(parent.getTerminal());
	}

	private void printPercentages(NumberFormat intFormat, Double cached, NumberFormat percentFormat, Double notCached, AttributedStyle greenFormat, AttributedStyle redFormat) {
		printPercentage("  -> Cached:", cached + notCached, percentFormat, intFormat, greenFormat, cached);
		printPercentage("  -> Not Cached:", cached + notCached, percentFormat, intFormat, redFormat, notCached);
	}
}