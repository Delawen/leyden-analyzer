package tooling.leyden.commands.logparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import tooling.leyden.aotcache.Element;
import tooling.leyden.aotcache.ElementFactory;
import tooling.leyden.aotcache.Information;
import tooling.leyden.aotcache.WarningType;
import tooling.leyden.commands.DefaultTest;
import tooling.leyden.commands.LoadFileCommand;

@QuarkusTest
class ProductionLogParserTest extends DefaultTest {

    static ProductionLogParser parser;

    @BeforeAll
    static void init() {
        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        parser = new ProductionLogParser(loadFile);
    }

    @Test
    void acceptGenericLine() {
        var line = parser.extractLineInformation("[info ][class,load         ] java.lang.Object source: shared objects file");

        assertEquals("info", line.level());
        assertEquals("java.lang.Object source: shared objects file", line.trimmedMessage());
        assertTrue(Arrays.stream(line.tags()).anyMatch(t -> t.equalsIgnoreCase("class")));
        assertTrue(Arrays.stream(line.tags()).anyMatch(t -> t.equalsIgnoreCase("load")));
        assertEquals(2, line.tags().length);

        line = parser.extractLineInformation(
                "[32,382s][warning][aot] Skipping org/hibernate/type/format/jakartajson/JsonBJsonFormatMapper: Failed verification");

        assertEquals("warning", line.level());
        assertEquals("Skipping org/hibernate/type/format/jakartajson/JsonBJsonFormatMapper: Failed verification",
                line.trimmedMessage());
        assertTrue(Arrays.stream(line.tags()).anyMatch(t -> t.equalsIgnoreCase("aot")));
        assertEquals(1, line.tags().length);
    }

    @Test
    void productionLogCheckClassLoad() {
        parser.accept("[info ][class,load         ] java.lang.Object source: shared objects file");
        parser.accept("[info ][class,load         ] java.io.Serializable source: shared objects file");
        parser.accept("[info ][class,load         ] java.lang.Comparable source: shared objects file");
        parser.accept("[info ][class,load         ] java.lang.CharSequence source: shared objects file");
        parser.accept("[info ][class,load         ] java.lang.constant.Constable source: shared objects file");
        parser.accept("[info ][class,load         ] java.lang.Class$$Lambda/0x800000019 source: shared objects file");
        parser.accept("[info ][class,load         ] java.util.ResourceBundle$Control$$Lambda/0x8000000ca source: " +
                "shared objects file");
        parser.accept("[info ][class,load         ] java.util.ResourceBundle$Control$$Lambda/0x8000000cb source: " +
                "shared objects file");
        parser.accept("[info ][class,load         ] java.net.InetAddress$$Lambda/0x800000063 source: shared objects " +
                "file");
        parser.accept("[info ][class,load         ] org.apache.logging.log4j.util" +
                ".PropertiesUtil$Environment$$Lambda/0x0000000805003450 source: org.apache.logging.log4j.util.PropertiesUtil$Environment");
        parser.accept("[info ][class,load         ] org.apache.logging.log4j.util" +
                ".PropertiesUtil$Environment$$Lambda/0x0000000805003678 source: org.apache.logging.log4j.util.PropertiesUtil$Environment");
        parser.accept("[info ][class,load         ] org.apache.logging.log4j.util" +
                ".ServiceLoaderUtil$$Lambda/0x00000008050038a8 source: org.apache.logging.log4j.util.ServiceLoaderUtil");
        parser.accept("[info ][class,load         ] java.lang.invoke.LambdaForm$MH/0x0000000805000800 source: " +
                "__JVM_LookupDefineClass__");
        parser.accept("[info ][class,load         ] org.apache.logging.log4j.spi" +
                ".AbstractLogger$$Lambda/0x0000000805000c00 source: org.apache.logging.log4j.spi.AbstractLogger");
        parser.accept("[info ][class,load         ] org.apache.logging.log4j.util.Strings$$Lambda/0x0000000805001000 " +
                "source: org.apache.logging.log4j.util.Strings");
        parser.accept("[info ][class,load         ] java.lang.invoke.LambdaForm$DMH/0x0000000805001800 source: " +
                "__JVM_LookupDefineClass__");
        parser.accept("[info ][class,load         ] org.apache.logging.log4j.status" +
                ".StatusLogger$PropertiesUtilsDouble$$Lambda/0x0000000805001228 source: org.apache.logging.log4j" +
                ".status.StatusLogger$PropertiesUtilsDouble");

        assertFalse(Information.getMyself().getStatistics().getKeys().isEmpty());
        assertFalse(Information.getMyself().getAll().isEmpty());
        assertFalse(Information.getMyself().getExternalElements().isEmpty());

        final int extClasses = Integer.parseInt(
                Information.getMyself().getStatistics().getValue("[LOG] Classes not loaded from AOT Cache").toString());
        final int extLambdas = Integer.parseInt(
                Information.getMyself().getStatistics().getValue("[LOG] Lambda Methods not loaded from AOT Cache").toString());
        final int classes = Integer
                .parseInt(Information.getMyself().getStatistics().getValue("[LOG] Classes loaded from AOT Cache").toString());
        final int lambdas = Integer.parseInt(
                Information.getMyself().getStatistics().getValue("[LOG] Lambda Methods loaded from AOT Cache").toString());

        assertEquals(extClasses, Information.getMyself().getExternalElements().size());
        assertEquals(classes, Information.getMyself().getElements(null, null, null, true, false, "Class").count());
        assertEquals(extClasses + classes, Information.getMyself().getElements(null, null, null, true, true, "Class").count());

        assertEquals(8, extClasses);
        assertEquals(6, extLambdas);
        assertEquals(9, classes);
        assertEquals(4, lambdas);

        //Now check individual values
        for (Element e : Information.getMyself().getAll()) {
            assertNull(e.getAddress()); //Log doesn't provide this
            assertNotNull(e.getKey());
            assertNull(e.getSize()); //Log doesn't provide this
            assertEquals("Class", e.getType());
            assertEquals(1, e.getWhereDoesItComeFrom().size());
            //Sometimes due to the order of the log,
            //we will have more than one source here
            assertFalse(e.getSources().isEmpty());
        }

        //Just check we didn't create something unexpectedly
        assertTrue(Information.getMyself().getWarnings().isEmpty());
    }

    @Test
    void warnings() {
        parser.accept("[error][aot] Loading static archive failed.");
        parser.accept("[warning][aot       ] Preload Warning: Verification failed for org.apache.logging.log4j.core" +
                ".async.AsyncLoggerContext");

        assertEquals(2, Information.getMyself().getWarnings().size());

        assertTrue(Information.getMyself().getWarnings().stream().noneMatch(w -> w.getType() == WarningType.CacheCreation));
    }

    @Test
    void statistics() {
        parser.accept("[info ][aot,codecache,init] Loaded 553 AOT code entries from AOT Code Cache");
        parser.accept("[debug][aot,codecache,init]   Adapters:  total=493");
        parser.accept("[debug][aot,codecache,init]   Shared Blobs: total=10");
        parser.accept("[debug][aot,codecache,init]   C1 Blobs: total=20");
        parser.accept("[debug][aot,codecache,init]   C2 Blobs: total=30");
        parser.accept("[debug][aot,codecache,init]   AOT code cache size: 598432 bytes");

        assertEquals("553", Information.getMyself().getStatistics()
                .getValue("[LOG] [CodeCache] Loaded AOT code entries"));
        assertEquals("493", Information.getMyself().getStatistics()
                .getValue("[LOG] [CodeCache] Loaded Adapters"));
        assertEquals("10", Information.getMyself().getStatistics()
                .getValue("[LOG] [CodeCache] Loaded Shared Blobs"));
        assertEquals("20", Information.getMyself().getStatistics()
                .getValue("[LOG] [CodeCache] Loaded C1 Blobs"));
        assertEquals("30", Information.getMyself().getStatistics()
                .getValue("[LOG] [CodeCache] Loaded C2 Blobs"));
        assertEquals("598432 bytes", Information.getMyself().getStatistics()
                .getValue("[LOG] [CodeCache] AOT code cache size"));
    }

    @Test
    void whereWereYouLoadedFrom() {
        Element e = ElementFactory.getOrCreate("org.cutecats.Test", "Class", null);

        e.setLoaded(Element.WhichRun.Training);
        assertEquals(Element.WhichRun.Training, e.wasLoaded());
        e.setLoaded(Element.WhichRun.Production);
        assertEquals(Element.WhichRun.Both, e.wasLoaded());

        e = ElementFactory.getOrCreate("org.cutecats.Test2", "Class", null);

        e.setLoaded(Element.WhichRun.Production);
        assertEquals(Element.WhichRun.Production, e.wasLoaded());
        e.setLoaded(Element.WhichRun.Training);
        assertEquals(Element.WhichRun.Both, e.wasLoaded());
        e.setLoaded(Element.WhichRun.Training);
        assertEquals(Element.WhichRun.Both, e.wasLoaded());
        e.setLoaded(Element.WhichRun.Production);
        assertEquals(Element.WhichRun.Both, e.wasLoaded());

    }

}
