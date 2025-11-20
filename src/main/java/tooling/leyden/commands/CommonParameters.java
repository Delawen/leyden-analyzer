package tooling.leyden.commands;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.TransactionScoped;
import picocli.CommandLine;
import tooling.leyden.commands.autocomplete.Identifiers;
import tooling.leyden.commands.autocomplete.Packages;
import tooling.leyden.commands.autocomplete.Types;

@CommandLine.Command(synopsisHeading      = "%nUsage:%n%n",
		descriptionHeading   = "%nDescription:%n%n",
		parameterListHeading = "%nParameters:%n%n",
		optionListHeading    = "%nOptions:%n%n",
		commandListHeading   = "%nCommands:%n%n")
public class CommonParameters {

	public enum ElementsToUse {
		cached, notCached, both
	}

	@CommandLine.Option(names = {"-pn", "--packageName"},
			description = {"Restrict the command to elements inside this package. ",
					"Note that some elements don't belong to any particular package"},
			arity = "0..*",
			split=",",
			paramLabel = "<packageName>",
			completionCandidates = Packages.class)
	protected String[] packageName;

	@CommandLine.Option(names = {"-epn", "--excludePackageName"},
			description = {"Exclude the elements inside this package. ",
					"Note that some elements don't belong to any particular package."},
			arity = "0..*",
			split=",",
			paramLabel = "<exclude>",
			completionCandidates = Packages.class)
	protected String[] excludePackageName;

	@CommandLine.Option(names = {"--identifier", "-i"},
			description ={"The object identifier. If it is a class, use the full qualified name."},
			defaultValue = "",
			arity = "0..1",
			paramLabel = "<id>",
			completionCandidates = Identifiers.class)
	private String name;

	@CommandLine.Option(names = {"--use", "-u"},
			description = "What type of elements to use on this command: cached during AOT, not cached, or both. " +
					"By default, only included in the AOT cache.",
			defaultValue = "cached",
			arity = "0..1")
	protected ElementsToUse use = ElementsToUse.cached;

	@CommandLine.Option(
			names = {"-t", "--type"},
			arity = "0..*",
			split=",",
			paramLabel = "<type>",
			description = "Restrict the command to this type of element",
			completionCandidates = Types.class)
	protected String[] types;

	public String getName() {
		return cleanQuotes(this.name);
	}

	private String cleanQuotes(String string) {
		if(string != null
				&& ((string.startsWith("'") && string.endsWith("'"))
				|| (string.startsWith("\"") && string.endsWith("\"")))) {
			string = string.substring(1, string.length() - 1);
		}
		return string;
	}
}
