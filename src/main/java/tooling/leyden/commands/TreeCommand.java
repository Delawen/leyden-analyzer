package tooling.leyden.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.ClassObject;
import tooling.leyden.aotcache.ConstantPoolObject;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.Information;
import tooling.leyden.aotcache.MethodObject;
import tooling.leyden.aotcache.ReferencingElement;

@Command(name = "tree", mixinStandardHelpOptions = true, version = "1.0", description = {
        "Shows the dependency graph of a class.",
        "By default, only classes will be shown, as symbols associated will be resolved to classes.",
        "This means, elements that refer to/use the root element. ",
        "Blue italic elements have already been shown and will not be expanded." }, subcommands = {
                CommandLine.HelpCommand.class })
class TreeCommand extends BaseCommand {

    @CommandLine.ParentCommand
    DefaultCommand parent;

    @CommandLine.Mixin
    CommonParameters parameters;

    @CommandLine.Option(names = { "-l", "--level" }, description = {
            "Maximum number of tree levels to display." }, defaultValue = "3", arity = "0..*", paramLabel = "<N>")
    Integer level;

    @CommandLine.Option(names = { "-max" }, description = {
            "Maximum number of elements to display. By default, 100. If using -1, it shows all " +
                    "elements. Note that on some cases this may mean showing thousands of elements." }, defaultValue = "100", arity = "0..*", paramLabel = "<N>")
    Integer max;

    @CommandLine.Option(names = { "-r", "--reverse" }, description = {
            "Show which classes are using this element instead of which classes are used by this element." }, defaultValue = "false", arity = "0..1", paramLabel = "<true>")
    Boolean reverse;

    public void execution() {

        if (parameters.getName() == null || parameters.getName().isBlank()) {
            (new AttributedString("ERROR: You must specify the name of the class to build the graph.",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold())).println(parent.getTerminal());
            return;
        }

        //Always show classes (Objects are nice too, for traceability)
        if (parameters.types == null) {
            parameters.types = new String[] { "Class", "Object" };
        }

        //Really, always show classes
        if (Arrays.stream(parameters.types).noneMatch(t -> t.equalsIgnoreCase("Class"))) {
            ArrayList<String> types = new ArrayList<>();
            types.addAll(Arrays.asList(parameters.types));
            types.add("Class");
            parameters.types = types.toArray(parameters.types);
        }

        List<Element> elements = parent.getInformation().getElements(parameters).toList();

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

        if (!isRunning()) {
            return;
        }

        boolean isFirst = true;
        for (Element refer : getElementsReferencingThisOne(root, Collections.synchronizedSet(new HashSet<>()))) {
            AttributedStringBuilder asb = new AttributedStringBuilder();

            if (isFirst) {
                asb.append(leftPadding.substring(0, leftPadding.length() - 1))
                        .append(String.valueOf('\\'));
                asb.append(AttributedString.NEWLINE);
            } else {
                asb.append(leftPadding).append(String.valueOf('|'));
                asb.append(AttributedString.NEWLINE);
            }

            if (travelled.contains(refer)) {
                asb.style(AttributedStyle.DEFAULT.bold().italic().foreground(AttributedStyle.BLUE));
                asb.append(leftPadding).append("- ");
            } else {
                asb.append(leftPadding).append("+ ");
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
        if (walkedBy.contains(element) || !isRunning()) {
            // We have already been here, stop!
            return Set.of();
        }
        walkedBy.add(element);

        var referenced = Collections.synchronizedSet(new HashSet<Element>());

        if (reverse) {
            referenced.addAll(element.getWhoReferencesMe());
            if (element.getType().equalsIgnoreCase("Object")) {
                var classes = ((ReferencingElement) element).getReferences().stream()
                        .filter(e -> e instanceof ClassObject).iterator();
                while (isRunning() && classes.hasNext()) {
                    referenced.add(classes.next());
                }
            }
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

        if (!isRunning()) {
            return Set.of();
        }

        Set<Element> tmp = new HashSet<>();
        // If we are showing these elements (in parameters.type), add them to result:
        var referencedIterator = referenced.parallelStream()
                .filter(e -> Arrays.stream(parameters.types).anyMatch(t -> t.equalsIgnoreCase(e.getType())))
                .iterator();
        while (referencedIterator.hasNext() && isRunning()) {
            tmp.add(referencedIterator.next());
        }
        //filter in case we have more constraints from packages or something
        var filteredIterator = filter(tmp.stream()).iterator();
        while (filteredIterator.hasNext() && isRunning()) {
            elements.add(filteredIterator.next());
        }
        //remove parent node, if it is there
        elements.remove(element);
        walkedBy.addAll(elements);
        if (!isRunning()) {
            return Set.of();
        }

        if (max > 0 && max < elements.size()) {
            // Do not continue looping recursively, this is already enough
            // because all elements here are going to be printed
            return elements;
        }

        // If we are not showing these elements (not in parameters.type), traverse them recursively:
        var traversedIterator = referenced.parallelStream()
                .filter(e -> Arrays.stream(parameters.types).noneMatch(t -> t.equalsIgnoreCase(e.getType())))
                //Do not loop infinitely
                .filter(e -> !walkedBy.contains(e))
                .iterator();
        while (traversedIterator.hasNext() && isRunning()) {
            elements.addAll(getElementsReferencingThisOne(traversedIterator.next(), walkedBy));
        }

        //remove parent node, again, if it is there
        //(it is usually there, because when traversing elements we usually find circular references)
        elements.remove(element);
        if (!isRunning()) {
            return Set.of();
        }

        return filter(elements.stream()).collect(Collectors.toSet());
    }

    //Delegate on Information for filtering
    private Stream<Element> filter(Stream<Element> elements) {
        return Information.filterByParams(parameters.packageName, parameters.excludePackageName, parameters.arrays,
                parameters.types, parameters.isHeapRoot, elements);
    }

}
