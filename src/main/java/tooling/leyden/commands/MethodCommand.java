package tooling.leyden.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.MethodObject;
import tooling.leyden.aotcache.NMethodObject;
import tooling.leyden.aotcache.ReferencingElement;
import tooling.leyden.commands.autocomplete.Packages;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Command(name = "method", mixinStandardHelpOptions = true, version = "1.0", description = {
        "Actions to discover information related to methods." }, subcommands = {
                CommandLine.HelpCommand.class })
class MethodCommand extends BaseCommand {

    @CommandLine.ParentCommand
    DefaultCommand parent;

    @CommandLine.Mixin
    protected CommonParameters parameters;

    @CommandLine.Option(names = { "--tier" }, description = {
            "Filter methods by compilation tier." }, arity = "0..*", split = ",", paramLabel = "<0..4>")
    protected Integer tier;

    @CommandLine.Option(names = { "--trainingtier" }, description = {
            "Filter methods by training tier." }, arity = "0..*", split = ",", paramLabel = "<0..4>")
    protected Integer trainingTier;

    @CommandLine.Option(names = { "--compiled" }, description = {
            "Filter methods that has been compiled (true) or not compiled (false)" }, arity = "0..1", paramLabel = "<true>")
    protected Boolean hasBeenCompiled;

    @Command(mixinStandardHelpOptions = true, version = "1.0", subcommands = { CommandLine.HelpCommand.class }, description = {
            "List methods available on the cache." })
    public void ls() {
        final var counter = new AtomicInteger();
        var methods = findElements(counter);
        final var elements = methods.iterator();
        while (isRunning() && elements.hasNext()) {
            elements.next().toAttributedString().println(parent.getTerminal());
        }
        parent.getOut().println("Found " + counter.get() + " elements.");
    }

    protected Stream<MethodObject> findElements(AtomicInteger counter) {
        parameters.setTypes(new String[]{"Method"});
        Stream<MethodObject> methods = parent.getInformation().getElements(parameters)
                .map(e -> (MethodObject) e);
        methods = methods.sorted(Comparator.comparing(Element::getKey)
                .thenComparing(Element::getType));
        if (tier != null) {
            methods = methods.filter(m -> m.getWhoReferencesMe().parallelStream()
                    .anyMatch(e -> e.getType().equalsIgnoreCase("NMethod")
                            && ((NMethodObject)e).getCompilationLevel().equals(tier)));
        }
        if (trainingTier != null) {
            methods = methods.filter(m -> m.getCompileTrainingData().containsKey(trainingTier));
        }
        if (hasBeenCompiled != null) {
            if (hasBeenCompiled) {
                methods = methods.filter(m -> m.getWhoReferencesMe().parallelStream()
                        .anyMatch(e -> e.getType().equalsIgnoreCase("NMethod")));
            } else {
                methods = methods.filter(m -> m.getWhoReferencesMe().parallelStream()
                        .noneMatch(e -> e.getType().equalsIgnoreCase("NMethod")));
            }
        }

        methods = methods.peek(item -> counter.incrementAndGet());
        return methods;
    }

    @Override
    public void execution() {
        ls();
    }
}
