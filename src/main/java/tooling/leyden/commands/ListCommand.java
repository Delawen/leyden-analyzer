package tooling.leyden.commands;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.*;

@Command(name = "ls", mixinStandardHelpOptions = true, version = "1.0", description = {
        "List what is on the cache. By default, it lists everything on the cache." }, subcommands = {
                CommandLine.HelpCommand.class })
class ListCommand extends BaseCommand {

    @CommandLine.ParentCommand
    DefaultCommand parent;

    @CommandLine.Mixin
    protected CommonParameters parameters;

    public void execution() {
        final var counter = new AtomicInteger();
        for (Element e : findElements(counter).toList()) {
            e.toAttributedString().println(parent.getTerminal());
            if (!isRunning()) {
                break;
            }
        }
        if (isRunning()) {
            parent.getOut().println("Found " + counter.get() + " elements.");
        }
    }

    protected Stream<Element> findElements(AtomicInteger counter) {
        return Information.getMyself().getElements(parameters).peek(item -> counter.incrementAndGet());
    }
}
