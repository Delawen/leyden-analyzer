package tooling.leyden.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.*;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


@Command(name = "ls", mixinStandardHelpOptions = true,
		version = "1.0",
		description = {"List what is on the cache. By default, it lists everything on the cache."},
		subcommands = {CommandLine.HelpCommand.class})
class ListCommand extends BaseCommand {

	@CommandLine.ParentCommand
	DefaultCommand parent;

	@CommandLine.Mixin
	protected CommonParameters parameters;

	public void execution() {
		final var counter = new AtomicInteger();
		final var elements = findElements(counter).iterator();
		while (isRunning() && elements.hasNext()) {
			elements.next().toAttributedString().println(parent.getTerminal());
        }
		if (isRunning()) {
			parent.getOut().println("Found " + counter.get() + " elements.");
		}
	}

	protected Stream<Element> findElements(AtomicInteger counter) {
		Stream<Element> elements = parent.getInformation().getElements(parameters);
		elements = elements.sorted(Comparator.comparing(Element::getKey).thenComparing(Element::getType));
		elements = elements.peek(item -> counter.incrementAndGet());

		return elements;
	}
}