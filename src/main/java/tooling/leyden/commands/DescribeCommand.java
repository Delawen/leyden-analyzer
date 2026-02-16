package tooling.leyden.commands;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.ReferencingElement;

import java.util.Comparator;
import java.util.List;

@Command(name = "describe", mixinStandardHelpOptions = true,
		version = "1.0",
		description = {"Describe an asset, showing all related info."},
		subcommands = {CommandLine.HelpCommand.class})
class DescribeCommand extends BaseCommand {

	@CommandLine.ParentCommand
	DefaultCommand parent;

	@CommandLine.Mixin
	private CommonParameters parameters;

	public void execution() {

		List<Element> elements = parent.getInformation().getElements(parameters).toList();

		AttributedStringBuilder sb = new AttributedStringBuilder();
		if (!elements.isEmpty()) {
            for (Element e : elements) {
				if (!isRunning()) {
					break;
				}
                var leftPadding = "  ";
                sb.append("-----");
                sb.append(AttributedString.NEWLINE);
                sb.append(e.getDescription(leftPadding, parameters.verbose, parameters.hints));
                sb.append(AttributedString.NEWLINE);
                if (parameters.verbose) {
                    sb.append(AttributedString.NEWLINE);
                    sb.append(leftPadding + "References: ");
                    var customLeftPadding = "  " + leftPadding;
                    if (e instanceof ReferencingElement re) {
                        if (!re.getReferences().isEmpty()) {
                            sb.append(AttributedString.NEWLINE);
                            sb.append(customLeftPadding + "Assets referenced from this asset: ");
                            sb.append(AttributedString.NEWLINE);
                            re.getReferences().forEach(refer -> {
                                sb.append(customLeftPadding + "   ");
                                sb.append(refer.toAttributedString());
                                sb.append(AttributedString.NEWLINE);
                            });
                        } else {
                            sb.append(AttributedString.NEWLINE);
                            sb.append(customLeftPadding + "There are no assets referenced from this one.");
                            sb.append(AttributedString.NEWLINE);
                        }
                    }

                    var referring = getElementsReferencingThisOne(e);
                    if (!referring.isEmpty()) {
                        sb.append(customLeftPadding + "Assets that refer to this one: ");
                        sb.append(AttributedString.NEWLINE);
                        referring.forEach(refer -> {
                            sb.append(customLeftPadding + "   ");
                            sb.append(refer.toAttributedString());
                            sb.append(AttributedString.NEWLINE);
                        });
                    } else {
                        sb.append(AttributedString.NEWLINE);
                        sb.append(customLeftPadding + "There are no assets that refer to this one.");
                        sb.append(AttributedString.NEWLINE);
                    }

                    if (!e.getWhereDoesItComeFrom().isEmpty()) {
                        sb.append(leftPadding);
                        sb.append(AttributedString.NEWLINE);
                        sb.append(leftPadding + "Where does this element come from: ");
                        sb.append(AttributedString.NEWLINE);
                        e.getWhereDoesItComeFrom().forEach(s -> {
                            sb.append(leftPadding + "  > ");
                            sb.append(s);
                            sb.append(AttributedString.NEWLINE);
                        });
                    }

                    if (!e.getSources().isEmpty()) {
                        sb.append(leftPadding);
                        sb.append(AttributedString.NEWLINE);
                        sb.append(leftPadding + "This information comes from: ");
                        sb.append(AttributedString.NEWLINE);
                        e.getSources().forEach(s -> {
                            sb.append(leftPadding + "  > ");
                            sb.append(s);
                            sb.append(AttributedString.NEWLINE);
                        });
                    }
                }
                sb.append("-----");
                sb.append(AttributedString.NEWLINE);
            }
        } else {
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
			sb.append("ERROR: Element not found. Try looking for it with ls.");
		}
		sb.toAttributedString().println(parent.getTerminal());
	}

	protected List<Element> getElementsReferencingThisOne(Element element) {
		if (!isRunning()) {
			return List.of();
		}
		return parent.getInformation().getAll().parallelStream()
				.filter(e -> (e instanceof ReferencingElement))
				.filter(e -> ((ReferencingElement) e).getReferences().contains(element))
				.sorted(Comparator.comparing(Element::getType))
				.toList();
	}
}