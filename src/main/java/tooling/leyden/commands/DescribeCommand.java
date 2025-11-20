package tooling.leyden.commands;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.Information;
import tooling.leyden.aotcache.ReferencingElement;

import java.util.Comparator;
import java.util.List;

@Command(name = "describe", mixinStandardHelpOptions = true,
		version = "1.0",
		description = {"Describe an object, showing all related info."},
		subcommands = {CommandLine.HelpCommand.class})
class DescribeCommand implements Runnable {

	@CommandLine.ParentCommand
	DefaultCommand parent;

	@CommandLine.Option(names = {"-v", "--verbose"},
			description = {"Show the extended information, like the full list of related elements."},
			arity = "0..*",
			defaultValue = "false",
			paramLabel = "<verbose>")
	protected Boolean verbose;

	@CommandLine.Mixin
	private CommonParameters parameters;

	Information information;

	public DescribeCommand(Information information, DefaultCommand parent) {
		this.information = information;
		this.parent = parent;
	}

	public void run() {

		List<Element> elements = parent.getInformation().getElements(parameters.getName(), parameters.packageName,
					parameters.excludePackageName, parameters.use, parameters.types).toList();

		AttributedStringBuilder sb = new AttributedStringBuilder();
		if (!elements.isEmpty()) {
			elements.forEach(e -> {
				var leftPadding = "|  ";
				sb.append("-----");
				sb.append(AttributedString.NEWLINE);
				sb.append(e.getDescription(leftPadding));
				sb.append(AttributedString.NEWLINE);
				if (verbose) {
					if (e instanceof ReferencingElement re) {
						if (!re.getReferences().isEmpty()) {
							sb.append(leftPadding + "Elements referenced from this element: ");
							sb.append(AttributedString.NEWLINE);
							sb.append(leftPadding + "  _____");
							sb.append(AttributedString.NEWLINE);
							re.getReferences().forEach(refer -> {
								sb.append(leftPadding + "  | ");
								sb.append(refer.toAttributedString());
								sb.append(AttributedString.NEWLINE);
							});
							sb.append(leftPadding + "  _____");
							sb.append(AttributedString.NEWLINE);
						} else {
							sb.append(leftPadding + "There are no elements referenced from this element.");
							sb.append(AttributedString.NEWLINE);
						}
					}

					var referring = getElementsReferencingThisOne(e);
					if (!referring.isEmpty()) {
						sb.append(leftPadding + "Elements that refer to this element: ");
						sb.append(AttributedString.NEWLINE);
						sb.append(leftPadding + "  _____");
						sb.append(AttributedString.NEWLINE);
						referring.forEach(refer -> {
							sb.append(leftPadding + "  | ");
							sb.append(refer.toAttributedString());
							sb.append(AttributedString.NEWLINE);
						});
						sb.append(leftPadding + "  _____");
						sb.append(AttributedString.NEWLINE);
					} else {
						sb.append(leftPadding + "There are no other elements of the cache that refer " +
								"to this element.");
						sb.append(AttributedString.NEWLINE);
					}

					if (!e.getWhereDoesItComeFrom().isEmpty()) {
						sb.append(leftPadding);
						sb.append(AttributedString.NEWLINE);
						sb.append(leftPadding + "Where does this element come from: ");
						sb.append(AttributedString.NEWLINE);
						sb.append(leftPadding + "  _____");
						sb.append(AttributedString.NEWLINE);
						e.getWhereDoesItComeFrom().forEach(s -> {
							sb.append(leftPadding + "  > ");
							sb.append(s);
							sb.append(AttributedString.NEWLINE);
						});
						sb.append(leftPadding + "  _____");
						sb.append(AttributedString.NEWLINE);
					}
				}
				sb.append("-----");
				sb.append(AttributedString.NEWLINE);
			});
		} else {
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
			sb.append("ERROR: Element not found. Try looking for it with ls.");
		}
		sb.toAttributedString().println(parent.getTerminal());
	}

	protected List<Element> getElementsReferencingThisOne(Element element) {
		return parent.getInformation().getAll().parallelStream()
				.filter(e -> (e instanceof ReferencingElement))
				.filter(e -> ((ReferencingElement) e).getReferences().contains(element))
				.sorted(Comparator.comparing(Element::getType))
				.toList();
	}
}