package tooling.leyden.commands.logparser;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.jline.utils.AttributedString;
import tooling.leyden.aotcache.ClassObject;
import tooling.leyden.aotcache.Configuration;
import tooling.leyden.aotcache.ElementFactory;
import tooling.leyden.aotcache.ReferencingElement;
import tooling.leyden.aotcache.Warning;
import tooling.leyden.aotcache.WarningType;
import tooling.leyden.commands.CommonParameters;

import java.util.List;

@Dependent
public class TrainingLogParser extends LogParser {

	@Inject
	private ElementFactory elementFactory;

	@Override
	void processLine(Line line) {
		if (containsTags(line.tags(), "aot")) {
			processAOT(line);
		}
	}

	@Override
	String getSource() {
		return "Training log";
	}

	@Override
	public void postProcessing() {

	}

	private void processAOT(Line line) {
		if (containsTags(line.tags(), "resolve")) {
			if (line.level().equals("trace")) {
				if (line.trimmedMessage().startsWith("archived")) {
					processAotTraceResolve(line.trimmedMessage());
				} else if (line.trimmedMessage().startsWith("reverted")) {
					processAOTReverted(line.trimmedMessage());
				}
			}
		}
		if (line.trimmedMessage().startsWith("Skipping ")) {
			processSkipping(line.message());
		} else if (line.level().equals("warning")) {
			processWarning(line.trimmedMessage());
		} else if (line.level().equals("error")) {
			processError(line.trimmedMessage());
		} else if (line.level().equals("info")) {
			processInfo(line.trimmedMessage());
		}
	}


	private void processAOTReverted(String trimmedMessage) {
		final var splitMessage = trimmedMessage.substring(trimmedMessage.indexOf("]: ") + 2).trim().split("\\s+");

		//First we find the Symbol related to
		final var parentClassName = splitMessage[0];
		ReferencingElement parentSymbol = assignClassToSymbol(findSymbol(parentClassName));

		if (trimmedMessage.startsWith("reverted klass")) {
//	reverted klass  CP entry [102]: io/reactivex/rxjava3/internal/subscribers/InnerQueuedSubscriber unreg => io/reactivex/rxjava3/internal/util/QueueDrainHelper
			information.getWarnings().add(
					new Warning(
							List.of(parentSymbol, assignClassToSymbol(findSymbol(splitMessage[3]))),
							new AttributedString(trimmedMessage), WarningType.CacheCreationRevertedKlass));
			findOrCreateSymbolAndLinkToParent(parentSymbol,
					"Used by " + parentSymbol.getIdentifier() + ".", splitMessage[3]);
		} else if (trimmedMessage.startsWith("reverted field")) {
// reverted field  CP entry [ 45]: io/netty/channel/AbstractChannelHandlerContext => io/netty/channel/DefaultChannelPipeline.head:Lio/netty/channel/DefaultChannelPipeline$HeadContext;

			final var names = splitMessage[2].split(":");
			information.getWarnings().add(
					new Warning(
							List.of(parentSymbol,
									assignClassToSymbol(findSymbol(names[1])),
									assignClassToSymbol(findSymbol(names[0].substring(0, names[0].lastIndexOf(".")))),
									assignClassToSymbol(findSymbol(names[0].substring(names[0].lastIndexOf(
											".") + 1)))),
							new AttributedString(trimmedMessage), WarningType.CacheCreationRevertedField));
		} else if (trimmedMessage.startsWith("reverted method")
				|| trimmedMessage.startsWith("reverted interface method")) {
// reverted method CP entry [ 16]: io/reactivex/rxjava3/internal/jdk8/FlowableStageSubscriber java/util/concurrent/CompletableFuture.complete:(Ljava/lang/Object;)Z
			final var names = splitMessage[1].split(":");
			information.getWarnings().add(
					new Warning(
							List.of(parentSymbol, assignClassToSymbol(findSymbol(names[1])),
									assignClassToSymbol(findSymbol(names[0].substring(0, names[0].lastIndexOf(".")))),
									assignClassToSymbol(findSymbol(names[0].substring(names[0].lastIndexOf(".") + 1))),
									assignClassToSymbol(findSymbol(names[1]))),
							new AttributedString(trimmedMessage), WarningType.CacheCreationRevertedMethod));
			final String source = "Used by a field in " + names[0] + ".";
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[1]);
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(0, names[0].lastIndexOf(".")));
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(names[0].lastIndexOf(".") + 1));
		} else if (trimmedMessage.startsWith("reverted indy ")) {
// reverted indy   CP entry [294]: jdk/jfr/internal/dcmd/DCmdDump (0) => java/lang/invoke/LambdaMetafactory.metafactory:
// (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
			final var names = splitMessage[2].split(":");
			information.getWarnings().add(
					new Warning(
							List.of(parentSymbol, assignClassToSymbol(findSymbol(names[1])),
									assignClassToSymbol(findSymbol(names[0].substring(0, names[0].lastIndexOf(".")))),
									assignClassToSymbol(findSymbol(names[0].substring(names[0].lastIndexOf(".") + 1))),
									assignClassToSymbol(findSymbol(names[1]))),
							new AttributedString(trimmedMessage), WarningType.CacheCreationRevertedIndy));
			final String source = "Used by indy " + splitMessage[0] + ".";
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(0, names[0].lastIndexOf(".")));
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(names[0].lastIndexOf(".") + 1));
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[1]);
		}
	}


	private void processAotTraceResolve(String trimmedMessage) {
		final var splitMessage = trimmedMessage.substring(trimmedMessage.indexOf("]: ") + 2).trim().split("\\s+");

		//First we find the Symbol related to
		final var parentClassName = splitMessage[0];
		ReferencingElement parentSymbol = findSymbol(parentClassName);
		assignClassToSymbol(parentSymbol);

		if (trimmedMessage.startsWith("archived klass")) {
//	archived klass  CP entry [  2]: org/infinispan/rest/framework/impl/InvocationImpl unreg => java/lang/Object boot
			findOrCreateSymbolAndLinkToParent(parentSymbol,
					"Used by " + parentSymbol.getIdentifier() + " " + splitMessage[4] + ".", splitMessage[3]);
		} else if (trimmedMessage.startsWith("archived field")) {
// archived field  CP entry [ 20]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.action:Ljava/lang/String;
			final var names = splitMessage[2].split(":");
			final String source = "Used by a field in " + names[0] + ".";
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[1]);
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(0, names[0].lastIndexOf(".")));
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(names[0].lastIndexOf(".") + 1));
		} else if (trimmedMessage.startsWith("archived method")
				|| trimmedMessage.startsWith("archived interface method")) {
// archived interface method CP entry [ 13]: jdk/jfr/internal/jfc/model/XmlNot java/util/List.size:()I => java/util/List
// archived method CP entry [338]: jdk/jfr/internal/dcmd/DCmdStart jdk/jfr/internal/dcmd/Argument.<init>
// :(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;Z)V => jdk/jfr/internal/dcmd/Argument
			final var names = splitMessage[1].split(":");
			final String source = "Used by method " + names[0] + ".";
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(0, names[0].lastIndexOf(".")));
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(names[0].lastIndexOf(".") + 1));
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[1]);
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, splitMessage[3]);
		} else if (trimmedMessage.startsWith("archived indy ")) {
// archived indy   CP entry [294]: jdk/jfr/internal/dcmd/DCmdDump (0) => java/lang/invoke/LambdaMetafactory.metafactory:
// (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
			final var names = splitMessage[3].split(":");
			final String source = "Used by indy " + splitMessage[0] + ".";
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(0, names[0].lastIndexOf(".")));
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[0].substring(names[0].lastIndexOf(".") + 1));
			findOrCreateSymbolAndLinkToParent(parentSymbol, source, names[1]);
		}
	}

	private ReferencingElement assignClassToSymbol(ReferencingElement symbol) {
		// If a class already exists with this Symbol, link it. If not, create it but don't add it to the cache yet
		// We will do the heavy creation work on AOT Parser, if any is loaded
		// because at this point, we don't know anything about the class... except the name
		final var className = symbol.getIdentifier().replaceAll("/", ".");
		var classObj = information.getElement(className, null, null, CommonParameters.ElementsToUse.both,
				"Class");
		ClassObject classObject;
		if (classObj.isPresent()) {
			classObject = (ClassObject) classObj.get();
		} else if (className.startsWith("L") && className.endsWith(";") && !className.contains("(")) {
			classObj = this.information.getElement(className.substring(1, className.length() - 1),
					null,
					null, CommonParameters.ElementsToUse.both,
					"Class");

			classObject = (ClassObject) classObj
					.orElse(elementFactory.getOrCreate(className, "Class", null));
		} else if (className.contains(".") && !className.contains("(")) {
			classObject = (ClassObject) elementFactory.getOrCreate(className, "Class", null);
		} else {
			classObject = null;
		}

		if (classObject != null) {
			classObject.addSymbol(symbol);
			symbol.addReference(classObject);
		}
		return symbol;

	}

	private ReferencingElement findOrCreateSymbolAndLinkToParent(ReferencingElement parentSymbol, String source,
																 String symbolName) {
		ReferencingElement referencedSymbol = findSymbol(symbolName);

		// If a class already exists with this Symbol, link it. If not, ignore it.
		// We will fill it when an AOT Cache loads, if it loads
		// (maybe it is not even a class, so don't care if this fails)
		var classObj = information.getElement(
				symbolName.replaceAll("/", "."), null, null,
				CommonParameters.ElementsToUse.both,
				"Class");
		if (classObj.isPresent()) {
			((ClassObject) classObj.get()).addSymbol(referencedSymbol);
			referencedSymbol.addReference(classObj.get());
		} else if (symbolName.startsWith("L") && symbolName.endsWith(";")) {
			classObj =
					this.information.getElement(symbolName.replaceAll("/", ".").substring(1, symbolName.length() - 1),
							null,
							null, CommonParameters.ElementsToUse.both,
							"Class");
			if (classObj.isPresent()) {
				((ClassObject) classObj.get()).addSymbol(referencedSymbol);
				referencedSymbol.addReference(classObj.get());
			}
		}

		referencedSymbol.addWhereDoesItComeFrom(source);
		parentSymbol.addReference(referencedSymbol);

		return referencedSymbol;
	}

	private ReferencingElement findSymbol(String symbolName) {
		ReferencingElement referencedSymbol = (ReferencingElement) elementFactory.getOrCreate(symbolName, "Symbol", null);
		referencedSymbol.addSource(getSource());
		return referencedSymbol;
	}


	//[warning][aot] Skipping java/lang/invoke/BoundMethodHandle$Species_LI because it is dynamically generated
	private void processSkipping(String message) {
		String[] msg = message.trim().split("\\s+");
		String className = msg[1].replace("/", ".").replace(":", "").trim();
		information.addWarning(elementFactory.getOrCreate(className, "Class", null), message, WarningType.CacheCreation);
	}


	private void processWarning(String trimmedMessage) {
		if (trimmedMessage.startsWith("Preload Warning: Verification failed for ")) {
			var className = trimmedMessage.substring(trimmedMessage.indexOf("for ") + 4);
			if (className.contains(" ")) {
				className = className.substring(0, className.indexOf(" "));
			}
			this.information.addWarning(elementFactory.getOrCreate(className, "Class", null), trimmedMessage,
					WarningType.CacheCreation);
		} else {
			//Very generic, but at least catch things
			information.getWarnings().add(new Warning(trimmedMessage));
		}
	}

	private void processError(String trimmedMessage) {
		//Very generic, but at least catch things
		information.getWarnings().add(new Warning(trimmedMessage));
	}

	private void processInfo(String trimmedMessage) {
		if (trimmedMessage.startsWith("Core region alignment:")) {
//	[info][aot] Core region alignment: 4096
			information.getConfiguration().addValue("Core region alignment", trimmedMessage.substring(23));
		} else if (trimmedMessage.startsWith("The AOT configuration file was created with ")) {
//[info][aot] The AOT configuration file was created with UseCompressedOops = 1, UseCompressedClassPointers = 1, UseCompactObjectHeaders = 0
			String[] config = trimmedMessage.split(" ");
			for (int i = 8; i < config.length - 1; i++) {
				if (config[i].equals("=")) {
					information.getConfiguration().addValue(config[i - 1], config[i + 1].replace(",", ""));
				}
			}
		} else if (trimmedMessage.startsWith("ArchiveRelocationMode:")) {
//[info][aot] ArchiveRelocationMode: 1 # always map archive(s) at an alternative address
			information.getConfiguration().addValue("ArchiveRelocationMode", trimmedMessage.substring(22).trim());
		} else if (trimmedMessage.startsWith("archived module property")) {
//[info][aot] archived module property jdk.module.main: (null)
//[info][aot] archived module property jdk.module.addexports: java.naming/com.sun.jndi.ldap=ALL-UNNAMED
//[info][aot] archived module property jdk.module.enable.native.access: ALL-UNNAMED
			storeConfigurationSplitByCharacter(information.getConfiguration(), trimmedMessage, ":");
		} else if (trimmedMessage.startsWith("initial ") && trimmedMessage.indexOf(":") > 0) {
//[info][aot] initial optimized module handling: enabled
//[info][aot] initial full module graph: disabled
			storeConfigurationSplitByCharacter(information.getConfiguration(), trimmedMessage, ":");
		} else if (trimmedMessage.startsWith("Using AOT-linked classes: ")) {
//[info][aot] Using AOT-linked classes: false (static archive: no aot-linked classes)
			//Maybe we should be more explicit on the info command about this
			storeConfigurationSplitByCharacter(information.getConfiguration(), trimmedMessage, ":");
		} else if (trimmedMessage.startsWith("JVM_StartThread() ignored:")) {
//[info][aot       ] JVM_StartThread() ignored: java.lang.ref.Reference$ReferenceHandler
			var className = trimmedMessage.substring(trimmedMessage.indexOf("ignored: ") + 9);
			this.information.addWarning(elementFactory.getOrCreate(className, "Class", null), trimmedMessage, WarningType.CacheCreation);
		}
	}

	private void storeConfigurationSplitByCharacter(Configuration config, String msg, String character) {
		var key = msg.substring(0, msg.indexOf(character));
		var value = msg.substring(msg.indexOf(character) + character.length() + 1).trim();
		config.addValue(key, value);
	}
}
