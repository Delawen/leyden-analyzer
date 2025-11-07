package tooling.leyden.commands;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.w3c.dom.Attr;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.ClassObject;
import tooling.leyden.aotcache.ConstantPoolObject;
import tooling.leyden.aotcache.Information;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.MethodObject;
import tooling.leyden.aotcache.ReferencingElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Command(name = "tree", mixinStandardHelpOptions = true,
		version = "1.0",
		description = {"Shows the dependency graph of a class.",
				"By default, only classes will be shown, as symbols associated will be resolved to classes.",
				"This means, elements that refer to/use the root element. ",
				"Blue italic elements have already been shown and will not be expanded."},
		subcommands = {CommandLine.HelpCommand.class})
class TreeCommand implements Runnable {

	@CommandLine.ParentCommand
	DefaultCommand parent;

	@CommandLine.Mixin
	CommonParameters parameters;

	@CommandLine.Option(names = {"-l", "--level"},
			description = {"Maximum number of tree levels to display."},
			defaultValue = "3",
			arity = "0..*",
			paramLabel = "<N>")
	Integer level;

	@CommandLine.Option(names = {"-max"},
			description = {"Maximum number of elements to display. By default, 100. If using -1, it shows all " +
					"elements. Note that on some cases this may mean showing thousands of elements."},
			defaultValue = "100",
			arity = "0..*",
			paramLabel = "<N>")
	Integer max;

	@CommandLine.Option(names = {"-r", "--reverse"},
			description = {
					"Show which classes are using this element instead of which classes are used by this element."},
			defaultValue = "false",
			arity = "0..1",
			paramLabel = "<true>")
	Boolean reverse;

	public void run() {

		if (parameters.getName() == null || parameters.getName().isBlank()) {
			(new AttributedString("ERROR: You must specify the name of the class to build the graph.",
					AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold())).println(parent.getTerminal());
			return;
		}

		//Always show classes (Objects are nice too, for traceability)
		if (parameters.types == null) {
			parameters.types = new String[]{"Class", "Object"};
		}

		//Really, always show classes
		if (Arrays.stream(parameters.types).noneMatch(t -> t.equalsIgnoreCase("Class"))) {
			ArrayList<String> types = new ArrayList<>();
			for (String t : parameters.types) {
				types.add(t);
			}
			types.add("Class");
			parameters.types = types.toArray(parameters.types);
		}

		List<Element> elements;

		switch (parameters.use) {
			case both -> elements = parent.getInformation().getElements(parameters.getName(), parameters.packageName,
					parameters.excludePackageName, parameters.showArrays, true, "Class").toList();
			case notCached -> elements = Information.getMyself().filterByParams(
					parameters.packageName, parameters.excludePackageName, parameters.showArrays,
					new String[]{"Class"},
					parent.getInformation().getExternalElements().entrySet().parallelStream()
							.filter(keyElementEntry -> keyElementEntry.getKey().identifier().equalsIgnoreCase(parameters.getName()))
							.map(keyElementEntry -> keyElementEntry.getValue())).toList();
			default -> elements = parent.getInformation().getElements(parameters.getName(), parameters.packageName,
					parameters.excludePackageName, parameters.showArrays, false, "Class").toList();
		}

		if (!elements.isEmpty()) {
			//Should be just one, but...
			elements.forEach(e -> {
				AttributedStringBuilder asb = new AttributedStringBuilder();
				asb.append("Showing which classes ");
				if (!reverse) {
					asb.append(e.toAttributedString());
					asb.append(" uses.");
				} else {
					asb.append("are used by ");
					asb.append(e.toAttributedString());
					asb.append(".");
				}
				asb.append(AttributedString.NEWLINE);
				asb.append("Calculating dependency graph... ");
				asb.toAttributedString().println(parent.getTerminal());
				parent.getTerminal().flush();
				(new AttributedString("+ ")).print(parent.getTerminal());
				e.toAttributedString().println(parent.getTerminal());
				try {
					printReferrals(e, "  ", Collections.synchronizedSet(new HashSet<>(List.of(e))), 0);
				} catch (Throwable except) {
					(new AttributedString("ERROR: Calculating the dependency graph:" + except.getLocalizedMessage(),
							AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold())).println(parent.getTerminal());
				}
			});
			parent.getTerminal().flush();
		} else {
			(new AttributedString("ERROR: Element not found. Try looking for it with ls.",
					AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold())).println(parent.getTerminal());
		}
	}

	private void printReferrals(Element root, String leftPadding, Set<Element> travelled, Integer level) {
		if (level > this.level || (max > 0 && travelled.size() > max))
			return;
		level++;

		boolean isFirst = true;
		for (Element refer : getElementsReferencingThisOne(root, Collections.synchronizedSet(new HashSet<>()))) {
			AttributedStringBuilder asb = new AttributedStringBuilder();

			if (isFirst) {
				asb.append(leftPadding.substring(0, leftPadding.length() - 1) + '\\');
				asb.append(AttributedString.NEWLINE);
			} else {
				asb.append(leftPadding + '|');
				asb.append(AttributedString.NEWLINE);
			}

			if (travelled.contains(refer)) {
				asb.style(AttributedStyle.DEFAULT.bold().italic().foreground(AttributedStyle.BLUE));
				asb.append(leftPadding + "- ");
			} else {
				asb.append(leftPadding + "+ ");
			}

			asb.append(refer.toAttributedString());
			asb.toAttributedString().println(parent.getTerminal());

			if (!travelled.contains(refer)) {
				travelled.add(refer);
				printReferrals(refer, leftPadding + "  ", travelled, level);
			}
			isFirst = false;

			if (max > 0 && travelled.size() > max) {
				break;
			}
		}
	}

	Set<Element> getElementsReferencingThisOne(Element element, Set<Element> walkedBy) {
		if (walkedBy.contains(element)) {
			// We have already been here, stop!
			return Set.of();
		}
		walkedBy.add(element);

		var referenced = Collections.synchronizedSet(new HashSet<Element>());

		if (reverse) {
			//TODO This is heavy. Heavy heavy heavy no please don't
			parent.getInformation().getAll().parallelStream()
							.forEach(e -> {
								if (e instanceof ReferencingElement re && re.getReferences().contains(element)) {
									referenced.add(e);
								}

								if (e instanceof ConstantPoolObject cp && cp.getPoolHolder() == element) {
									referenced.add(e);
								} else if (e instanceof MethodObject method && method.getClassObject() == element) {
									referenced.add(e);
								} else if (e instanceof ClassObject classObject
										//We don't jump from Symbol to Symbol
										&& !element.getType().equalsIgnoreCase("Symbol")
										&& classObject.getSymbols().contains(element)) {
									referenced.add(e);
								}
							});
		} else {
			if (element instanceof ClassObject classObject) {
				referenced.addAll(classObject.getSymbols());
				referenced.addAll(classObject.getMethods());
			} else if (element instanceof ConstantPoolObject cp) {
//it would be clearer if we could show the dependency connection as being to a specific Method or Field
// and maybe mark it in some way as a CPCache pre-link dependency rather than, say, a Method link that arises because
// of, say, a compilation dependency.
				referenced.add(cp.getPoolHolder());
			} else if (element instanceof MethodObject method) {
				referenced.add(method.getClassObject());
			}

			if (element instanceof ReferencingElement re) {
				referenced.addAll(re.getReferences());
			}
		}

		final Set<Element> elements = Collections.synchronizedSet(new HashSet<>());

		Set<Element> tmp = new HashSet<>();
		// If we are showing these elements (in parameters.type), add them to result:
		referenced.parallelStream()
				.filter(e -> Arrays.stream(parameters.types).anyMatch(t -> t.equalsIgnoreCase(e.getType())))
				.forEach(tmp::add);
		//filter in case we have more constraints from packages or something
		filter(tmp.stream()).forEach(elements::add);
		//remove parent node, if it is there
		elements.remove(element);
		walkedBy.addAll(elements);

		if (max > 0 && max < elements.size()) {
			// Do not continue looping recursively, this is already enough
			// because all elements here are going to be printed
			// this is the last call
			return elements;
		}

		// If we are not showing these elements (not in parameters.type), traverse them recursively:
		//TODO do we need to traverse all of them? Or only certain types?
		referenced.stream()
				.filter(e -> Arrays.stream(parameters.types).noneMatch(t -> t.equalsIgnoreCase(e.getType())))
				.filter(e -> !walkedBy.contains(e))
				.peek(e -> System.out.println("Recursively looping over " + e))
				.forEach(e -> elements.addAll(getElementsReferencingThisOne(e, walkedBy))
				);


		return elements;
	}

	//Delegate on Information for filtering
	private Stream<Element> filter(Stream<Element> elements) {
		return Information.filterByParams(parameters.packageName, parameters.excludePackageName, parameters.showArrays,
				parameters.types, elements);
	}

}