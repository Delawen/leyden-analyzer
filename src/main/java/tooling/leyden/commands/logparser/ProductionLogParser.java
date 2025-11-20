package tooling.leyden.commands.logparser;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.ElementFactory;
import tooling.leyden.aotcache.Warning;
import tooling.leyden.aotcache.WarningType;
import tooling.leyden.commands.CommonParameters;

@Dependent
public class ProductionLogParser extends LogParser {

	@Inject
	private ElementFactory elementFactory;

	@Override
	protected void processLine(Line line) {
		if (containsTags(line.tags(), "class", "load")) {
			processClassLoad(line);
		} else if (containsTags(line.tags(), "aot", "codecache")) {
			processCodeCache(line);
		} else if (line.level().equals("warning")) {
			processWarning(line.trimmedMessage());
		} else if (line.level().equals("error")) {
			processError(line.trimmedMessage());
		}
	}

	private void processWarning(String trimmedMessage) {
		if (trimmedMessage.startsWith("The AOT cache was created by a different")) {
			information.addWarning(null, trimmedMessage, WarningType.CacheLoad);
		} else {
			//Very generic, but at least catch things
			information.getWarnings().add(new Warning(trimmedMessage));
		}
	}

	private void processError(String trimmedMessage) {
		if (trimmedMessage.startsWith("An error has occurred while processing the AOT cache")
				|| trimmedMessage.equals("Loading static archive failed.")
				|| trimmedMessage.equals("Unable to map shared spaces")) {
			information.addWarning(null, trimmedMessage, WarningType.CacheLoad);
		} else {
			//Very generic, but at least catch things
			information.getWarnings().add(new Warning(trimmedMessage));
		}
	}

	@Override
	String getSource() {
		return "Production log";
	}

	@Override
	public void postProcessing() {

	}

	private void processClassLoad(Line line) {
		if (line.message().contains(" source: ")) {
			String className = line.message().substring(0, line.message().indexOf("source: ")).trim();
			Element e;
			if (line.message().indexOf("source: shared objects file") > 0) {
				var classes = information.getElements(className, null, null, CommonParameters.ElementsToUse.both, "Class").findAny();
				if (classes.isEmpty()) {
					//WARNING: this should be covered by the aot map file
					//we are assuming no aot map file was loaded at this point
					//so we create a basic placeholder
					e = elementFactory.getOrCreate(className, "Class", null);
				} else {
					e = classes.get();
				}
				this.information.getStatistics().incrementValue("[LOG] Classes loaded from AOT Cache");
				if (className.contains("$$Lambda/")) {
					//This is a lambda
					this.information.getStatistics().incrementValue("[LOG] Lambda Methods loaded from AOT Cache");
				}
				information.addAOTCacheElement(e, getSource());

			} else {
				e = elementFactory.getOrCreate(className, "Class", null);
				e.addSource(getSource());
				information.addExternalElement(e);
				// else this wasn't loaded from the aot cache
				if (className.contains("$$Lambda/")) {
					this.information.getStatistics().incrementValue("[LOG] Lambda Methods not loaded from AOT Cache");
				}
				this.information.getStatistics().incrementValue("[LOG] Classes not loaded from AOT Cache");
			}
			e.addWhereDoesItComeFrom("Loaded from " + line.content().substring(line.content().indexOf("source: ")));
		}
	}


	private void processCodeCache(Line line) {
		if (containsTags(line.tags(), "codecache")) {
			if (containsTags(line.tags(), "init")) {
				if (line.trimmedMessage().startsWith("Loaded ")
						&& line.trimmedMessage().endsWith("AOT code entries from AOT Code Cache")) {
					//[info ][aot,codecache,init] Loaded 493 AOT code entries from AOT Code Cache
					information.getStatistics().addValue("[LOG] [CodeCache] Loaded AOT code entries",
							line.trimmedMessage().substring(7, line.trimmedMessage().substring(7).indexOf(" ") + 7));
				} else if (line.trimmedMessage().contains(" total=")) {
					//[debug][aot,codecache,init]   Adapters:  total=493
					//[debug][aot,codecache,init]   Shared Blobs: total=0
					//[debug][aot,codecache,init]   C1 Blobs: total=0
					//[debug][aot,codecache,init]   C2 Blobs: total=0
					information.getStatistics().addValue("[LOG] [CodeCache] Loaded " + line.trimmedMessage().substring(0,
							line.trimmedMessage().indexOf(":")).trim(), line.trimmedMessage().substring(line.trimmedMessage().indexOf(
							"total=") + 6));
				} else if (line.trimmedMessage().startsWith("AOT code cache size:")) {
					//[debug][aot,codecache,init]   AOT code cache size: 598432 bytes
					information.getStatistics().addValue("[LOG] [CodeCache] AOT code cache size", line.trimmedMessage().substring(20).trim());
				}
			}
		}
	}

}
