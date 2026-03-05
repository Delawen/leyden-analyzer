package tooling.leyden.commands.logparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import tooling.leyden.aotcache.*;
import tooling.leyden.commands.DefaultTest;
import tooling.leyden.commands.LoadFileCommand;

@QuarkusTest
class TrainingLogParserTest extends DefaultTest {

    static TrainingLogParser parser;
    static AOTMapParser aotParser;

    @BeforeAll
    static void init() {
        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        parser = new TrainingLogParser(loadFile);
        aotParser = new AOTMapParser(loadFile);
    }

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

        assertEquals(11, Information.getMyself().getWarnings().size());

        assertTrue(Information.getMyself().getWarnings().stream().noneMatch(w -> w.getType() == WarningType.CacheLoad));
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

        assertEquals("4096", Information.getMyself().getConfiguration().getValue("Core region alignment"));
        assertEquals("1", Information.getMyself().getConfiguration().getValue("UseCompressedOops"));
        assertEquals("1", Information.getMyself().getConfiguration().getValue("UseCompressedClassPointers"));
        assertEquals("0", Information.getMyself().getConfiguration().getValue("UseCompactObjectHeaders"));
        assertEquals("1 # always map archive(s) at an alternative address",
                Information.getMyself().getConfiguration().getValue("ArchiveRelocationMode"));
        assertEquals("(null)", Information.getMyself().getConfiguration().getValue("archived module property jdk.module.main"));
        assertEquals("java.naming/com.sun.jndi.ldap=ALL-UNNAMED",
                Information.getMyself().getConfiguration().getValue("archived module property jdk.module.addexports"));
        assertEquals("ALL-UNNAMED", Information.getMyself().getConfiguration()
                .getValue("archived module property jdk.module.enable.native.access"));
        assertEquals("enabled", Information.getMyself().getConfiguration().getValue("initial optimized module handling"));
        assertEquals("disabled", Information.getMyself().getConfiguration().getValue("initial full module graph"));
    }

    @Test
    void acceptAOTResolve() {
        parser.accept(
                "[info ][aot,resolve              ] Archiving CP entries for org/infinispan/rest/framework/impl/InvocationImpl");
        parser.accept(
                "[trace][aot,resolve              ] archived klass  CP entry [  2]: org/infinispan/rest/framework/impl/InvocationImpl unreg => java/lang/Object boot");
        parser.accept(
                "[trace][aot,resolve              ] archived klass  CP entry [  8]: org/infinispan/rest/framework/impl/InvocationImpl unreg => org/infinispan/rest/framework/impl/InvocationImpl unreg");
        parser.accept(
                "[trace][aot,resolve              ] archived field  CP entry [  7]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.methods:Ljava/util/Set;");
        parser.accept(
                "[trace][aot,resolve              ] archived field  CP entry [ 13]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.paths:Ljava/util/Set;");
        parser.accept(
                "[trace][aot,resolve              ] archived field  CP entry [ 16]: org/infinispan/rest/framework/impl/InvocationImpl => org/infinispan/rest/framework/impl/InvocationImpl.handler:Ljava/util/function/Function;");
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
        aotParser.accept(
                "0x00000008007cd538: @@ Symbol             44 io/reactivex/rxjava3/internal/subscribers/InnerQueuedSubscriber");
        aotParser.accept("0x00000008007cd538: @@ Symbol             48 java/util/concurrent/atomic/AtomicReference");
        parser.accept("[trace][aot,resolve              ] archived klass  CP entry [ 23]: " +
                "io/reactivex/rxjava3/internal/subscribers/InnerQueuedSubscriber unreg => java/util/concurrent/atomic/AtomicReference boot");

        var parentSymbol = (ReferencingElement) Information.getMyself()
                .getElements("org/infinispan/rest/framework/impl/InvocationImpl",
                        null, null, true, true, "Symbol")
                .findAny().get();
        assertEquals(17, parentSymbol.getReferences().size());
        assertEquals(2,
                ((ReferencingElement) Information.getMyself()
                        .getElements("io/reactivex/rxjava3/internal/subscribers/InnerQueuedSubscriber",
                                null, null, true, true, "Symbol")
                        .findAny().get())
                        .getReferences().size());

        //Now try adding methods
        parser.accept("[trace][aot,resolve              ] archived method CP entry [194]: " +
                "jdk/jfr/internal/jfc/model/XmlSelection jdk/jfr/internal/jfc/model/XmlSelection.getDefault:()Ljava/lang/String; => jdk/jfr/internal/jfc/model/XmlSelection");
        parentSymbol = (ReferencingElement) Information.getMyself().getElements("jdk/jfr/internal/jfc/model/XmlSelection",
                null, null, true, true, "Symbol").findAny().get();
        assertEquals(3, parentSymbol.getReferences().size());

        parser.accept("[trace][aot,resolve              ] archived method CP entry [  8]: " +
                "jdk/jfr/internal/dcmd/DCmdStart$$Lambda+0x80000010e java/lang/Object.<init>:()V => java/lang/Object");
        parentSymbol = (ReferencingElement) Information.getMyself()
                .getElements("jdk/jfr/internal/dcmd/DCmdStart$$Lambda+0x80000010e",
                        null, null, true, true, "Symbol")
                .findAny().get();
        assertEquals(4, parentSymbol.getReferences().size());

        parser.accept("[trace][aot,resolve              ] archived method CP entry [338]: " +
                "jdk/jfr/internal/dcmd/DCmdStart jdk/jfr/internal/dcmd/Argument.<init>" +
                ":(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;Z)V => jdk/jfr/internal/dcmd/Argument");
        parentSymbol = (ReferencingElement) Information.getMyself().getElements("jdk/jfr/internal/dcmd/DCmdStart",
                null, null, true, true, "Symbol").findAny().get();
        assertEquals(4, parentSymbol.getReferences().size());
        assertTrue(parentSymbol.getReferences().stream()
                .anyMatch(symbol -> symbol.getKey().equals("jdk/jfr/internal/dcmd/Argument")));
        assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getKey().equals("<init>")));
        assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getKey()
                .equals("(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;Z)V")));

        //interface method
        parser.accept("[trace][aot,resolve              ] archived interface method CP entry [ 18]: " +
                "jdk/internal/module/ModuleBootstrap$$Lambda+0x80000000c java/util/Collection.stream:()Ljava/util/stream/Stream; => java/util/Collection");
        parentSymbol = (ReferencingElement) Information.getMyself()
                .getElements("jdk/internal/module/ModuleBootstrap$$Lambda+0x80000000c",
                        null, null, true, true, "Symbol")
                .findAny().get();
        assertEquals(4, parentSymbol.getReferences().size());

        //With Symbols from the AOT Map file
        aotParser.accept("0x00000008007cd538: @@ Symbol             14 jdk/jfr/internal/jfc/model/XmlNot");
        aotParser.accept("0x00000008007cd538: @@ Symbol             14 size");
        aotParser.accept("0x00000008007cd538: @@ Symbol             14 java/util/List");
        aotParser.accept("0x00000008007cd538: @@ Symbol             14 ()I");
        parser.accept("[trace][aot,resolve              ] archived interface method CP entry [ 13]: " +
                "jdk/jfr/internal/jfc/model/XmlNot java/util/List.size:()I => java/util/List");
        parentSymbol = (ReferencingElement) Information.getMyself().getElements("jdk/jfr/internal/jfc/model/XmlNot",
                null, null, true, true, "Symbol").findAny().get();
        assertEquals(4, parentSymbol.getReferences().size());
        assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getKey().equals("java/util/List")));
        assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getKey().equals("size")));
        assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getKey().equals("()I")));

        //Add indy archive
        parser.accept("[trace][aot,resolve              ] archived indy   CP entry [294]: " +
                "jdk/jfr/internal/dcmd/DCmdDump (0) => java/lang/invoke/LambdaMetafactory.metafactory:" +
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");
        parentSymbol = (ReferencingElement) Information.getMyself().getElements("jdk/jfr/internal/dcmd/DCmdDump",
                null, null, true, true, "Symbol").findAny().get();
        assertEquals(4, parentSymbol.getReferences().size());

        final var signature = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
        parser.accept("[trace][aot,resolve              ] archived indy   CP entry [263]: " +
                "jdk/jfr/internal/dcmd/DCmdCheck (1) => java/lang/invoke/LambdaMetafactory.metafactory:" +
                signature);
        parentSymbol = (ReferencingElement) Information.getMyself().getElements("jdk/jfr/internal/dcmd/DCmdCheck",
                null, null, true, true, "Symbol").findAny().get();
        assertEquals(4, parentSymbol.getReferences().size());
        assertTrue(parentSymbol.getReferences().stream()
                .anyMatch(symbol -> symbol.getKey().equals("java/lang/invoke/LambdaMetafactory")));
        assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getKey().equals("metafactory")));
        assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getKey().equals(signature)));

        assertTrue(Information.getMyself().getAll().parallelStream().allMatch(e -> e.getType().equals("Symbol")));

        //If a class exists already, the Symbol must be linked there:
        aotParser.accept("0x0000000800a8efe8: @@ Class             536 sun.util.locale.BaseLocale");
        aotParser.accept("0x0000000800a8efe8: @@ Class             536 sun.util.locale.LocaleUtils");
        aotParser.accept("0x0000000800a92ef0: @@ ConstantPoolCache 64 sun.util.locale.BaseLocale");
        aotParser.accept("0x0000000802f8c710: @@ ConstantPool      2456 sun.util.locale.BaseLocale");
        parser.accept(
                "[trace][aot,resolve              ] archived klass  CP entry [  2]: sun/util/locale/BaseLocale boot => java/lang/Object boot");
        parser.accept(
                "[trace][aot,resolve              ] archived klass  CP entry [  8]: sun/util/locale/BaseLocale boot => sun/util/locale/BaseLocale boot");
        parser.accept(
                "[trace][aot,resolve              ] archived klass  CP entry [ 28]: sun/util/locale/BaseLocale boot => sun/util/locale/LocaleUtils boot (not supertype)");

        var classObj = (ClassObject) Information.getMyself().getElements("sun.util.locale.BaseLocale",
                null, null, true, true, "Class").findAny().get();
        assertEquals(1, classObj.getSymbols().size());
        parentSymbol = classObj.getSymbols().getFirst();
        assertEquals(classObj.getKey(), parentSymbol.getKey().replaceAll("/", "."));
        assertEquals(3, parentSymbol.getReferences().size());
        assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getKey().equals("java/lang/Object")));
        assertTrue(parentSymbol.getReferences().stream()
                .anyMatch(symbol -> symbol.getKey().equals("sun/util/locale/LocaleUtils")));
        assertTrue(parentSymbol.getReferences().stream().anyMatch(symbol -> symbol.getType().equals("Class")));

        var cp = (ConstantPoolObject) Information.getMyself().getElements("sun.util.locale.BaseLocale",
                null, null, true, true, "ConstantPool").findAny().get();
        assertNotNull(cp.getConstantPoolCacheAddress());
        assertEquals(cp.getPoolHolder(), classObj);

        classObj = (ClassObject) Information.getMyself().getElements("sun.util.locale.LocaleUtils",
                null, null, true, true, "Class").findAny().get();
        assertEquals(1, classObj.getSymbols().size());
        parentSymbol = classObj.getSymbols().getFirst();
        assertEquals(classObj.getKey(), parentSymbol.getKey().replaceAll("/", "."));
    }

    @Test
    void classCreationFromSymbol() {
        parser.accept(
                "[trace][aot,resolve] reverted field  CP entry [157]: io/quarkus/vertx/http/runtime/VertxHttpRecorder => io/quarkus/dev/spi/DevModeType.REMOTE_SERVER_SIDE:Lio/quarkus/dev/spi/DevModeType;");

        assertTrue(Information.getMyself().getElements("io.quarkus.vertx.http.runtime" +
                ".VertxHttpRecorder", null, null, true, true, "Class")
                .findAny().isPresent());
        assertTrue(Information.getMyself().getElements("io.quarkus.dev.spi.DevModeType", null, null, true, true,
                "Class").findAny().isPresent());
        assertEquals(2, Information.getMyself().getElements(null, null, null, true, true, "Class")
                .count());
    }

    @Test
    void codeCacheSummary() {
        String log = """
                [debug  ][aot,codecache,exit]   None: total=0
                [debug  ][aot,codecache,exit]   Adapter: total=728
                [debug  ][aot,codecache,exit]   Stub: total=10
                [debug  ][aot,codecache,exit]   SharedBlob: total=1
                [debug  ][aot,codecache,exit]   C1Blob: total=2
                [debug  ][aot,codecache,exit]   C2Blob: total=3
                [debug  ][aot,codecache,exit]   Nmethod: total=9142
                [debug  ][aot,codecache,exit]     Tier 0: total=5
                [debug  ][aot,codecache,exit]     Tier 1: total=1251
                [debug  ][aot,codecache,exit]     Tier 2: total=3379
                [debug  ][aot,codecache,exit]     Tier 3: total=4
                [debug  ][aot,codecache,exit]     Tier 4: total=2256
                [debug  ][aot,codecache,exit]     Tier 5: total=2257
                [debug  ][aot,codecache,exit]   AOT code cache size: 31332312 bytes, max entry's size: 136328 bytes
                [info   ][aot,codecache,exit] Wrote 9870 AOT code entries to AOT Code Cache
                			""";

        BufferedReader reader = new BufferedReader(new StringReader(log));
        reader.lines().forEach(parser::accept);
        parser.postProcessing();

        var statistics = Information.getMyself().getStatistics();

        assertEquals(0.0, statistics.getValue("[CodeCache] None"));
        assertEquals(728.0, statistics.getValue("[CodeCache] Adapter"));
        assertEquals(10.0, statistics.getValue("[CodeCache] Stub"));
        assertEquals(1.0, statistics.getValue("[CodeCache] SharedBlob"));
        assertEquals(2.0, statistics.getValue("[CodeCache] C1Blob"));
        assertEquals(3.0, statistics.getValue("[CodeCache] C2Blob"));
        assertEquals(9142.0, statistics.getValue("[CodeCache] Nmethod"));
        assertEquals(5.0, statistics.getValue("[CodeCache] Nmethod Tier 0"));
        assertEquals(1251.0, statistics.getValue("[CodeCache] Nmethod Tier 1"));
        assertEquals(3379.0, statistics.getValue("[CodeCache] Nmethod Tier 2"));
        assertEquals(4.0, statistics.getValue("[CodeCache] Nmethod Tier 3"));
        assertEquals(2256.0, statistics.getValue("[CodeCache] Nmethod Tier 4"));
        assertEquals(2257.0, statistics.getValue("[CodeCache] Nmethod Tier 5"));
        assertEquals("31332312 bytes, max entry's size: 136328 bytes", statistics.getValue("[CodeCache] Cache Size"));
        assertEquals(9870.0, statistics.getValue("[CodeCache] AOT Code Entries"));
    }

    @Test
    void rejectionReasons() {
        String log = """
                [warning][aot] class org/postgresql/util/LazyCleanerImpl$CleanableWrapper cannot be archived because it was not defined from /home/delawen/git/leyden-perf-test/builds/gqaot/quarkus-hibernate-orm-spacefox/quarkus-hibernate-orm-spacefox/lib/main/org.postgresql.postgresql-42.7.9.jar as claimed
                [trace][aot,resolve] java/time/format/TextStyle CP entry [ 26] => method [Ljava/time/format/TextStyle;.clone:()Ljava/lang/Object; can't be archived because its resolution is not deterministic.
                			""";

        BufferedReader reader = new BufferedReader(new StringReader(log));
        reader.lines().forEach(parser::accept);
        parser.postProcessing();

        List<Warning> warnings = Information.getMyself().getWarnings();
        assertEquals(2, warnings.size());

        Warning w = warnings.getFirst();
        assertEquals(WarningType.CacheCreationRevertedKlass, w.getType());
        assertTrue(w.affects("org/postgresql/util/LazyCleanerImpl$CleanableWrapper"));
        assertTrue(w.affects("org.postgresql.util.LazyCleanerImpl$CleanableWrapper"));

        w = warnings.getLast();
        assertEquals(WarningType.CacheCreationRevertedMethod, w.getType());
        assertTrue(w.affects("java.time.format.TextStyle"));
        assertTrue(w.affects("java/time/format/TextStyle"));
    }
}
