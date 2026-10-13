package tooling.leyden;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tooling.leyden.aotcache.Information;
import tooling.leyden.commands.CommonParameters;
import tooling.leyden.commands.DefaultTest;
import tooling.leyden.commands.LoadFileCommand;
import tooling.leyden.commands.logparser.AOTMapParser;
import tooling.leyden.commands.logparser.ProductionLogParser;
import tooling.leyden.commands.logparser.TrainingLogParser;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PlaygroundTest extends DefaultTest {

    private static AOTMapParser aotCacheParser;
    private static TrainingLogParser trainingLogParser;
    private static ProductionLogParser productionLogParser;

    @BeforeAll
    static void init() {
        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        trainingLogParser = new TrainingLogParser(loadFile);
        productionLogParser = new ProductionLogParser(loadFile);
        aotCacheParser = new AOTMapParser(loadFile);
    }

    @Test
    void addTwoMapFiles() {
        String oneMapFilePlayground = "one.map.file";
        String anotherMapFilePlayground = "another.map.file";

        assertEquals("default", Information.getMyself().getPlayground());
        aotCacheParser.accept("AOT cache map for " + oneMapFilePlayground);
        assertEquals(oneMapFilePlayground, Information.getMyself().getPlayground());
        aotCacheParser.accept("0x0000000000000001: @@ Class             624 org.infinispan.xsite.NoOpBackupSender");
        aotCacheParser.accept("AOT cache map for " + anotherMapFilePlayground);
        assertEquals(anotherMapFilePlayground, Information.getMyself().getPlayground());
        aotCacheParser.accept("0x0000000000000002: @@ Class             624 org.infinispan.xsite.NoOpBackupSender");

        assertEquals(anotherMapFilePlayground, Information.getMyself().getPlayground());
        Set<String> sources = Information.getMyself().getPlaygroundObject().getSources();
        assertFalse(sources.isEmpty());
        assertEquals(1, sources.size());
        sources.stream().anyMatch(s -> s.equals("AOT cache map for " + anotherMapFilePlayground));
        var elements = Information.getMyself().getAll();
        assertEquals(1, elements.size());
        var element = elements.iterator().next();
        assertEquals("0x0000000000000002", element.getAddress());
        assertEquals(Information.getMyself().getPlayground(), element.getPlayground().getIdentifier());
        var commonParameters = new CommonParameters();
        elements = Information.getMyself().getElements(commonParameters).toList();
        assertEquals(1, elements.size());
        assertEquals("0x0000000000000002", element.getAddress());

        Information.getMyself().changePlayground(oneMapFilePlayground);
        sources = Information.getMyself().getPlaygroundObject().getSources();
        assertFalse(sources.isEmpty());
        assertEquals(1, sources.size());
        sources.stream().anyMatch(s -> s.equals("AOT cache map for " + oneMapFilePlayground));
        assertEquals(oneMapFilePlayground, Information.getMyself().getPlayground());
        elements = Information.getMyself().getAll();
        assertEquals(1, elements.size());
        element = elements.iterator().next();
        assertEquals("0x0000000000000001", element.getAddress());
        assertEquals(Information.getMyself().getPlayground(), element.getPlayground().getIdentifier());
        elements = Information.getMyself().getElements(commonParameters).toList();
        assertEquals(1, elements.size());
        assertEquals("0x0000000000000001", element.getAddress());

        Information.getMyself().changePlayground(anotherMapFilePlayground);
        assertEquals(anotherMapFilePlayground, Information.getMyself().getPlayground());
        elements = Information.getMyself().getAll();
        assertEquals(1, elements.size());
        element = elements.iterator().next();
        assertEquals("0x0000000000000002", element.getAddress());
        assertEquals(Information.getMyself().getPlayground(), element.getPlayground().getIdentifier());

        //Now test the clear
        Information.getMyself().clear(anotherMapFilePlayground);
        assertTrue(Information.getMyself().getAll().isEmpty());

        Information.getMyself().changePlayground(oneMapFilePlayground);
        assertFalse(Information.getMyself().getAll().isEmpty());
        Information.getMyself().clear(anotherMapFilePlayground);

        Information.getMyself().clear();
        Information.getMyself().changePlayground(oneMapFilePlayground);
        assertTrue(Information.getMyself().getAll().isEmpty());
    }



    @Test
    void addLogParser() {
        assertEquals("default", Information.getMyself().getPlayground());

        trainingLogParser.updatePlayground();
        productionLogParser.updatePlayground();
        trainingLogParser.accept(
                "[warning][aot] Skipping org/apache/logging/log4j/core/async/AsyncLoggerContext: Failed verification");
        productionLogParser.accept("[info ][class,load         ] java.lang.Comparable source: shared objects file");

        CommonParameters params = new CommonParameters();
        params.setUse(CommonParameters.ElementsToUse.both);
        var elements = Information.getMyself().getElements(params).toList();
        assertTrue(elements.stream()
                .allMatch(e -> e.getPlayground().getIdentifier().equalsIgnoreCase("default")));
        assertTrue(elements.stream().anyMatch(e -> e.getKey().equalsIgnoreCase("java.lang.Comparable")));
        assertTrue(elements.stream().anyMatch(e -> e.getKey().equalsIgnoreCase("org.apache.logging.log4j.core.async.AsyncLoggerContext")));
        assertEquals(2, elements.size());

        aotCacheParser.accept("AOT cache map for target/map.file");
        trainingLogParser.updatePlayground();
        trainingLogParser.accept("[warning][aot] Skipping org/slf4j/ILoggerFactory: Old class has been linked");
        productionLogParser.updatePlayground();
        productionLogParser.accept("[info ][class,load         ] java.lang.CharSequence source: shared objects file");
        assertEquals("target/map.file", Information.getMyself().getPlayground());
        elements = Information.getMyself().getElements(params).toList();
        assertTrue(elements.stream()
                .allMatch(e -> e.getPlayground().getIdentifier().equalsIgnoreCase("target/map.file")));
        assertTrue(elements.stream().anyMatch(e -> e.getKey().equalsIgnoreCase("java.lang.CharSequence")));
        assertTrue(elements.stream().anyMatch(e -> e.getKey().equalsIgnoreCase("org.slf4j.ILoggerFactory")));
        assertEquals(2, elements.size());

        productionLogParser.accept("[info ][class,load         ] java.lang.Comparable source: shared objects file");
        elements = Information.getMyself().getElements(params).toList();
        assertTrue(elements.stream().anyMatch(e -> e.getKey().equalsIgnoreCase("java.lang.Comparable")));
        assertTrue(elements.stream()
                .allMatch(e -> e.getPlayground().getIdentifier().equalsIgnoreCase("target/map.file")));
        assertEquals(3, elements.size());
    }

}
