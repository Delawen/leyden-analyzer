package tooling.leyden.commands.logparser;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tooling.leyden.aotcache.ClassObject;
import tooling.leyden.aotcache.ConstantPoolObject;
import tooling.leyden.aotcache.ReferencingElement;
import tooling.leyden.aotcache.WarningType;
import tooling.leyden.commands.CommonParameters;
import tooling.leyden.commands.DefaultTest;
import tooling.leyden.commands.LoadFileCommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TrainingLogParserTest extends DefaultTest {

	@Inject
	TrainingLogParser parser;
	@Inject
	AOTMapParser aotParser;

	@Test
	void warnings() {
		parser.accept("[warning][aot] Skipping org/apache/logging/log4j/core/async/AsyncLoggerContext: Failed " +
				"verification");
		parser.accept("[warning][aot] Skipping org/apache/logging/slf4j/Log4jLoggerFactory$$Lambda+0x800000258: " +
						"nest_host class org/apache/logging/slf4j/Log4jLoggerFactory is excluded");
		parser.accept("[warning][aot] Skipping jdk/proxy1/$Proxy29: Unsupported location");
		parser.accept("[warning][aot] Skipping org/slf4j/ILoggerFactory: Old class has been linked");
		parser.accept("[warning][aot] Skipping jdk/internal/event/SecurityProviderServiceEvent: JFR event class");
		parser.accept("[warning][aot] Skipping com/thoughtworks/xstream/security/ForbiddenClassException: Unlinked " +
				"class not supported by AOTConfiguration");
		parser.accept("[warning][aot] Skipping java/lang/invoke/BoundMethodHandle$Species_LI because it is " +
				"dynamically generated");
		parser.accept("[info][aot       ] JVM_StartThread() ignored: java.lang.ref.Reference$ReferenceHandler");
		parser.accept("[warning][aot       ] Preload Warning: Verification failed for org.infinispan.remoting" +
				".transport.jgroups.JGroupsRaftManager");
		parser.accept("[warning][aot       ] Preload Warning: Verification failed for org.apache.logging.log4j.core" +
				".async.AsyncLoggerContext");
		parser.accept("[13,198s][warning][aot] Preload Warning: Verification failed for " +
				"org.apache.logging.log4j.core.async.AsyncLoggerContext because a java.lang.NoClassDefFoundError was " +
				"thrown: com/lmax/disruptor/EventTranslatorVararg");

		assertEquals(11, getInformation().getWarnings().size());

		assertTrue(getInformation().getWarnings().stream().noneMatch(w -> w.getType() == WarningType.CacheLoad));
	}

	@Test
	void acceptConfiguration() {
		parser.accept("[info][aot] Core region alignment: 4096");
		parser.accept("[info][aot] The AOT configuration file was created with UseCompressedOops = 1, " +
				"UseCompressedClassPointers = 1, UseCompactObjectHeaders = 0");
		parser.accept("[info][aot] ArchiveRelocationMode: 1 # always map archive(s) at an alternative address");
		parser.accept("[info][aot] archived module property jdk.module.main: (null)");
		parser.accept("[info][aot] archived module property jdk.module.addexports: java.naming/com.sun.jndi.ldap=ALL-UNNAMED");
		parser.accept("[info][aot] archived module property jdk.module.enable.native.access: ALL-UNNAMED");
		parser.accept("[info][aot] initial optimized module handling: enabled");
		parser.accept("[info][aot] initial full module graph: disabled");

		assertEquals("4096", getInformation().getConfiguration().getValue("Core region alignment"));
		assertEquals("1", getInformation().getConfiguration().getValue("UseCompressedOops"));
		assertEquals("1", getInformation().getConfiguration().getValue("UseCompressedClassPointers"));
		assertEquals("0", getInformation().getConfiguration().getValue("UseCompactObjectHeaders"));
		assertEquals("1 # always map archive(s) at an alternative address",
				getInformation().getConfiguration().getValue("ArchiveRelocationMode"));
		assertEquals("(null)", getInformation().getConfiguration().getValue("archived module property jdk.module.main"));
		assertEquals("java.naming/com.sun.jndi.ldap=ALL-UNNAMED", getInformation().getConfiguration().getValue("archived module property jdk.module.addexports"));
		assertEquals("ALL-UNNAMED", getInformation().getConfiguration().getValue("archived module property jdk.module.enable.native.access"));
		assertEquals("enabled", getInformation().getConfiguration().getValue("initial optimized module handling"));
		assertEquals("disabled", getInformation().getConfiguration().getValue("initial full module graph"));
	}

	@Test
	void acceptAOTResolve() {
		parser.accept("[info ][aot,resolve              ] Archiving CP entries for org/infinispan/rest/framework/impl/InvocationImpl");
		parser.accept("[trace][aot,resolve              ] archived klass  CP entry [  2]: org/infinispan/rest/framework/impl/InvocationImpl unreg => java/lang/Object boot");
		parser.accept("[trace][aot,resolve              ] archived klass  CP entry [  8]: org/infinispan/rest/framework/impl/InvocationImpl unreg => org/infinispan/rest/framework/impl/InvocationImpl unreg");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [  7]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.methods:Ljava/util/Set;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 13]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.paths:Ljava/util/Set;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 16]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.handler:Ljava/util/function/Function;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 20]: " +
				"org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.action:Ljava/lang/String;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 24]: " +
				"org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.name:Ljava/lang/String;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 27]: " +
				"org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.anonymous:Z");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 31]: " +
				"org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl" +
				".permission:Lorg/infinispan/security/AuthorizationPermission;");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 35]: " +
				"org/infinispan/rest/framework/impl/InvocationImpl => " +
				"org/infinispan/rest/framework/impl/InvocationImpl.deprecated:Z");

		//Make sure we reuse classes from aot map file
		aotParser.accept("0x00000008007cd538: @@ Symbol             14 org/infinispan/security/AuditContext");
		parser.accept("[trace][aot,resolve              ] archived field  CP entry [ 38]: " +
				"org/infinispan/rest/framework/impl/InvocationImpl => " +
				"org/infinispan/rest/framework/impl/InvocationImpl.auditContext:Lorg/infinispan/security/AuditContext;");
		aotParser.accept("0x00000008007cd538: @@ Symbol             44 io/reactivex/rxjava3/internal/subscribers/InnerQueuedSubscriber");
		aotParser.accept("0x00000008007cd538: @@ Symbol             48 java/util/concurrent/atomic/AtomicReference");
		parser.accept("[trace][aot,resolve              ] archived klass  CP entry [ 23]: " +
				"io/reactivex/rxjava3/internal/subscribers/InnerQueuedSubscriber unreg => java/util/concurrent/atomic/AtomicReference boot");

		var parentSymbol = (ReferencingElement) getInformation().getElement("org/infinispan/rest/framework/impl/InvocationImpl",
				null, null, CommonParameters.ElementsToUse.both, "Symbol").get();
		assertEquals(17, parentSymbol.getReferences().size());
		assertEquals(2,
				((ReferencingElement) getInformation().getElement("io/reactivex/rxjava3/internal/subscribers/InnerQueuedSubscriber",
						null, null, CommonParameters.ElementsToUse.both, "Symbol").get())
						.getReferences().size());

		//Now try adding methods
		parser.accept("[trace][aot,resolve              ] archived method CP entry [194]: " +
				"jdk/jfr/internal/jfc/model/XmlSelection jdk/jfr/internal/jfc/model/XmlSelection.getDefault:()Ljava/lang/String; => jdk/jfr/internal/jfc/model/XmlSelection");
		parentSymbol = (ReferencingElement) getInformation().getElement("jdk/jfr/internal/jfc/model/XmlSelection",
				null, null, CommonParameters.ElementsToUse.both, "Symbol").get();
		assertEquals(3, parentSymbol.getReferences().size());

		parser.accept("[trace][aot,resolve              ] archived method CP entry [  8]: " +
				"jdk/jfr/internal/dcmd/DCmdStart$$Lambda+0x80000010e java/lang/Object.<init>:()V => java/lang/Object");
		parentSymbol = (ReferencingElement) getInformation().getElement("jdk/jfr/internal/dcmd/DCmdStart$$Lambda+0x80000010e",
				null, null, CommonParameters.ElementsToUse.both, "Symbol").get();
		assertEquals(4, parentSymbol.getReferences().size());

		parser.accept("[trace][aot,resolve              ] archived method CP entry [338]: " +
				"jdk/jfr/internal/dcmd/DCmdStart jdk/jfr/internal/dcmd/Argument.<init>" +
				":(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;Z)V => jdk/jfr/internal/dcmd/Argument");
		parentSymbol = (ReferencingElement) getInformation().getElement("jdk/jfr/internal/dcmd/DCmdStart",
				null, null, CommonParameters.ElementsToUse.both, "Symbol").get();
		assertEquals(4, parentSymbol.getReferences().size());
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals("jdk/jfr/internal/dcmd/Argument")));
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals("<init>")));
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier()
				.equals("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;Z)V")));

		//interface method
		parser.accept("[trace][aot,resolve              ] archived interface method CP entry [ 18]: " +
				"jdk/internal/module/ModuleBootstrap$$Lambda+0x80000000c java/util/Collection.stream:()Ljava/util/stream/Stream; => java/util/Collection");
		parentSymbol = (ReferencingElement) getInformation().getElement("jdk/internal/module/ModuleBootstrap$$Lambda+0x80000000c",
				null, null, CommonParameters.ElementsToUse.both, "Symbol").get();
		assertEquals(4, parentSymbol.getReferences().size());

		//With Symbols from the AOT Map file
		aotParser.accept("0x00000008007cd538: @@ Symbol             14 jdk/jfr/internal/jfc/model/XmlNot");
		aotParser.accept("0x00000008007cd538: @@ Symbol             14 size");
		aotParser.accept("0x00000008007cd538: @@ Symbol             14 java/util/List");
		aotParser.accept("0x00000008007cd538: @@ Symbol             14 ()I");
		parser.accept("[trace][aot,resolve              ] archived interface method CP entry [ 13]: " +
				"jdk/jfr/internal/jfc/model/XmlNot java/util/List.size:()I => java/util/List");
		parentSymbol = (ReferencingElement) getInformation().getElement("jdk/jfr/internal/jfc/model/XmlNot",
				null, null, CommonParameters.ElementsToUse.both, "Symbol").get();
		assertEquals(4, parentSymbol.getReferences().size());
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals("java/util/List")));
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals("size")));
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals("()I")));

		//Add indy archive
		parser.accept("[trace][aot,resolve              ] archived indy   CP entry [294]: " +
				"jdk/jfr/internal/dcmd/DCmdDump (0) => java/lang/invoke/LambdaMetafactory.metafactory:" +
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");
		parentSymbol = (ReferencingElement) getInformation().getElement("jdk/jfr/internal/dcmd/DCmdDump",
				null, null, CommonParameters.ElementsToUse.both, "Symbol").get();
		assertEquals(4, parentSymbol.getReferences().size());

		final var signature = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
		parser.accept("[trace][aot,resolve              ] archived indy   CP entry [263]: " +
				"jdk/jfr/internal/dcmd/DCmdCheck (1) => java/lang/invoke/LambdaMetafactory.metafactory:" +
				signature);
		parentSymbol = (ReferencingElement) getInformation().getElement("jdk/jfr/internal/dcmd/DCmdCheck",
				null, null, CommonParameters.ElementsToUse.both, "Symbol").get();
		assertEquals(4, parentSymbol.getReferences().size());
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals("java/lang/invoke/LambdaMetafactory")));
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals("metafactory")));
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals(signature)));

		assertTrue(getInformation().getAll().parallelStream().allMatch(e -> e.getType().equals("Symbol")));

		//If a class exists already, the Symbol must be linked there:
		aotParser.accept("0x0000000800a8efe8: @@ Class             536 sun.util.locale.BaseLocale");
		aotParser.accept("0x0000000800a8efe8: @@ Class             536 sun.util.locale.LocaleUtils");
		aotParser.accept("0x0000000800a92ef0: @@ ConstantPoolCache 64 sun.util.locale.BaseLocale");
		aotParser.accept("0x0000000802f8c710: @@ ConstantPool      2456 sun.util.locale.BaseLocale");
		parser.accept("[trace][aot,resolve              ] archived klass  CP entry [  2]: sun/util/locale/BaseLocale boot => java/lang/Object boot");
		parser.accept("[trace][aot,resolve              ] archived klass  CP entry [  8]: sun/util/locale/BaseLocale boot => sun/util/locale/BaseLocale boot");
		parser.accept("[trace][aot,resolve              ] archived klass  CP entry [ 28]: sun/util/locale/BaseLocale boot => sun/util/locale/LocaleUtils boot (not supertype)");

		var classObj = (ClassObject) getInformation().getElement("sun.util.locale.BaseLocale",
				null, null, CommonParameters.ElementsToUse.both, "Class").get();
		assertEquals(1, classObj.getSymbols().size());
		parentSymbol = classObj.getSymbols().iterator().next();
		assertEquals(classObj.getIdentifier(), parentSymbol.getIdentifier().replaceAll("/", "."));
		assertEquals(3, parentSymbol.getReferences().size());
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals("java/lang/Object")));
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getIdentifier().equals("sun/util/locale/LocaleUtils")));
		assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getType().equals("Class")));

		var cp = (ConstantPoolObject) getInformation().getElement("sun.util.locale.BaseLocale",
				null, null, CommonParameters.ElementsToUse.both, "ConstantPool").get();
		assertNotNull(cp.getConstantPoolCacheAddress());
		assertEquals(cp.getPoolHolder(), classObj);

		classObj = (ClassObject) getInformation().getElement("sun.util.locale.LocaleUtils",
				null, null, CommonParameters.ElementsToUse.both, "Class").get();
		assertEquals(1, classObj.getSymbols().size());
		parentSymbol = classObj.getSymbols().iterator().next();
		assertEquals(classObj.getIdentifier(), parentSymbol.getIdentifier().replaceAll("/", "."));
	}
}