package tooling.leyden.commands;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.ClassObject;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.MethodObject;
import tooling.leyden.aotcache.Warning;
import tooling.leyden.aotcache.WarningType;
import tooling.leyden.commands.autocomplete.Identifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Command(name = "warning", mixinStandardHelpOptions = true,
		version = "1.0",
		description = {"Help detect and clarify warnings found. By default, it lists incidents detected on the logs."},
		subcommands = {CommandLine.HelpCommand.class})
class WarningCommand extends BaseCommand {

	@CommandLine.ParentCommand
	DefaultCommand parent;

	@CommandLine.Option(
			names = {"-t", "--type"},
			arity = "0..*",
			split = ",",
			paramLabel = "<type>",
			description = "Restrict the command to this type of warnings",
			completionCandidates = Types.class)
	protected String[] types;

	@CommandLine.Option(
			names = {"-l", "--limit"},
			arity = "0..1",
			paramLabel = "<limit>",
			description = "Limit number of incidents per type listed"
	)
	protected Integer limit;

	@CommandLine.Option(names = {"--identifier", "-i"},
			description ={"The object identifier. Use the full qualified name."},
			defaultValue = "",
			arity = "0..1",
			paramLabel = "<id>",
			completionCandidates = Identifiers.class)
	protected String name;

	// Packages that are usually not part of the user application
	// so we skip them on our auto checks
	private String[] excludedPackages = new String[]{"java", "jdk", "sun", "com.sun"};

	public void execution() {
		printWarnings();
	}

	private void printWarnings() {
		final var wa = getWarnings();
		wa.forEach(element -> element.getDescription().println(parent.getTerminal()));
		parent.getOut().println("Found " + wa.size() + " warnings.");
	}

	protected List<Warning> getWarnings() {
		List<Warning> wa = new ArrayList<>();
		wa.addAll(parent.getInformation().getWarnings());
		wa.addAll(parent.getInformation().getAutoWarnings());

		if (types != null && types.length > 0) {
			wa.removeIf(warning -> Arrays.stream(types).noneMatch(t -> t.equalsIgnoreCase(warning.getType().name())));
		}

		if (name != null && !name.isBlank()) {
			wa.removeIf(w -> !w.affects(name));
		}

		if (limit != null) {
			final Map<WarningType, Integer> count = new HashMap<>();
			wa.removeIf(warning -> {
				count.putIfAbsent(warning.getType(), 0);
				count.replace(warning.getType(), count.get(warning.getType()) + 1);
				return count.get(warning.getType()) > limit;
			});
		}

		wa.sort(Comparator.comparing(Warning::getId));
		return wa;
	}

	@Command(mixinStandardHelpOptions = true,
			version = "1.0",
			subcommands = {CommandLine.HelpCommand.class},
			description = {"Mark a warning as safe, clearing it from the list of warnings.",
					"You can't recover it afterwards without doing a new check or loading the log file."})
	public void rm(@CommandLine.Parameters(paramLabel = "<id>",
			description = "warning to clear") String id) {
		parent.getInformation().getAutoWarnings().removeIf(w -> w.getId().equalsIgnoreCase(id));
		parent.getInformation().getWarnings().removeIf(w -> w.getId().equalsIgnoreCase(id));
	}


	@Command(mixinStandardHelpOptions = true,
			version = "1.0",
			subcommands = {CommandLine.HelpCommand.class},
			description = {"Automatically try to detect inconsistencies and potential defects in your run.",
					"The auto-detected issues may or may not be problematic.",
					"It is up to the developer to decide that."
			})
	public void check() {
		setupHandle();

		parent.getOut().println("Trying to detect problems...");

		List<Warning> warnings = parent.getInformation().getAutoWarnings();
		warnings.clear();

		if (!isRunning()) {
			return;
		}
		warnings.addAll(getTopPackagesNotCached());
		if (!isRunning()) {
			return;
		}
		warnings.addAll(getTopPackagesUsedAndNotTrained());
		if (!isRunning()) {
			return;
		}

		printWarnings();
		parent.getOut().println("The auto-detected issues may or may not be problematic.");
		parent.getOut().println("It is up to the developer to decide that.");
	}

	// Get packages with most methods called but not trained
	protected List<Warning> getTopPackagesUsedAndNotTrained() {
		var result = new ArrayList<Warning>();
		var packages = new HashMap<String, Integer>();

		parent.getInformation().getElements(null, null, excludedPackages, false, false, "Method")
				.map(MethodObject.class::cast)
				.filter(e ->  e.getMethodCounters() != null && e.getCompileTrainingData().isEmpty())
				.forEach(method -> {
					final var classObject = method.getClassObject();
					addToPackageList(classObject, packages);
				});

		String warningString = " methods that were called several times during training run but lack full training (don't have" +
				" some of the CompileTrainingData objects associated to them).";
		getTopPackages(packages, warningString, result, WarningType.MethodTraining);

		return result;
	}

	// Get packages with most classes not cached
	private List<Warning> getTopPackagesNotCached() {
		var result = new ArrayList<Warning>();
		var packages = new HashMap<String, Integer>();

		for (Element e : parent.getInformation().getExternalElements().values()) {
			if (e instanceof ClassObject classObject) {
				if (Arrays.stream(excludedPackages).noneMatch(p -> classObject.getPackageName().startsWith(p))) {
					addToPackageList(classObject, packages);
				}
			}
		}

		//Remove packages with less than 10 classes
		packages.entrySet().removeIf(entry -> entry.getValue() < 10);

		getTopPackages(packages, " classes loaded and not cached.", result, WarningType.Training);

		return result;
	}

	private static void getTopPackages(HashMap<String, Integer> packages, String warningString,
									   ArrayList<Warning> result, WarningType warningType) {
		packages.entrySet().stream().sorted((e1, e2) -> e2.getValue() - e1.getValue())
				.forEach(entry -> {
					AttributedStringBuilder sb = new AttributedStringBuilder();
					sb.append("Package '");
					sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
					sb.append(entry.getKey());
					sb.style(AttributedStyle.DEFAULT);
					sb.append("' contains ");
					sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
					sb.append(entry.getValue().toString());
					sb.style(AttributedStyle.DEFAULT);
					sb.append(warningString);
					result.add(new Warning(List.of(), sb.toAttributedString(), warningType));
				});
	}

	private static void addToPackageList(ClassObject classObject, HashMap<String, Integer> packages) {
		//Cut package name to just three levels (dots)
		//because we are looking for base packages
		String packageName = "";
		int i = 1;
		for (String s : classObject.getPackageName().split("\\.")) {
			if (packageName.isBlank()) {
				packageName += s;
			} else {
				packageName += "." + s;
			}
			if (i++ > 2) {
				break;
			}
		}
		packages.putIfAbsent(packageName, 0);
		packages.compute(packageName, (key, v) -> {
			v++;
			return v;
		});
	}

	static class Types implements Iterable<String> {
		@Override
		public Iterator<String> iterator() {
			return Arrays.stream(WarningType.values()).map(warningType -> warningType.name()).iterator();
		}
	}
}
