package tooling.leyden.commands;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.Information;
import tooling.leyden.aotcache.MethodObject;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Command(name = "ls", mixinStandardHelpOptions = true,
		version = "1.0",
		description = {"List what is on the cache. By default, it lists everything on the cache."},
		subcommands = {CommandLine.HelpCommand.class})
class ListCommand implements Runnable {
	@CommandLine.Mixin
	protected CommonParameters parameters;

	@CommandLine.Option(names = {"--trained"},
			description = {"Only displays elements with training information.",
					"This may restrict the types of elements shown, along with what was passed as parameters."},
			defaultValue = "false",
			arity = "0..1")
	protected Boolean trained;

	@CommandLine.Option(names = {"--run"},
			description = {"Only displays methods that were run on the training run."},
			defaultValue = "false",
			arity = "0..1")
	protected Boolean run;

	@CommandLine.ParentCommand
	DefaultCommand parent;


	public ListCommand(DefaultCommand parent) {
		this.parent = parent;
	}

	public void run() {
		final var counter = new AtomicInteger();
		final var elements = findElements(counter);

		elements.forEach(element -> element.toAttributedString().println(parent.getTerminal()));
		parent.getOut().println("Found " + counter.get() + " elements.");
	}

	@Transactional(Transactional.TxType.MANDATORY)
	protected Stream<Element> findElements(AtomicInteger counter) {
		Stream<Element> elements = parent.getInformation().getElements(parameters.getName(), parameters.packageName,
					parameters.excludePackageName, parameters.use, parameters.types);

		if (trained) {
			elements = elements.filter(e -> e.isTraineable() && e.isTrained());
		}

		if (run) {
			elements = elements
					.filter(e -> e.getType().equalsIgnoreCase("Method"))
					.filter(e -> ((MethodObject) e).getMethodCounters() != null);
		}

		elements = elements.sorted(Comparator.comparing(Element::getIdentifier));
		elements = elements.peek(item -> counter.incrementAndGet());

		return elements;
	}
}