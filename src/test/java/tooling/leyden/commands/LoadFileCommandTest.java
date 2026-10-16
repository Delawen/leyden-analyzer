package tooling.leyden.commands;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tooling.leyden.aotcache.Information;
import tooling.leyden.aotcache.Playground;
import tooling.leyden.commands.logparser.AOTMapParser;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class LoadFileCommandTest extends DefaultTest {

    @Test
    void addTwoMapFilesWithExplicitPlayground() {

        String mapFile = """
AOT cache map for playground1
0x00000008003eb000: @@ Class             1000 java.math.BigInteger
AOT cache map for playground2
0x00000008005e30e8: @@ Class             672 java.time.Month
AOT cache map for playground3
0x00000008005e30e8: @@ Class             672 java.util.List
                """;

        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        loadFile.playground = "playground_zero";
        AOTMapParser aotCacheParser = new AOTMapParser(loadFile, loadFile.playground);
        BufferedReader reader = new BufferedReader(new StringReader(mapFile));
        reader.lines().forEach(aotCacheParser::accept);
        aotCacheParser.postProcessing();

        var playgrounds = Information.getMyself().getPlaygrounds();
        assertTrue(playgrounds.stream().anyMatch(p -> p.equals("default")));
        assertTrue(playgrounds.stream().noneMatch(p -> p.equals("playground1")));
        assertTrue(playgrounds.stream().anyMatch(p -> p.equals("playground2")));
        assertTrue(playgrounds.stream().anyMatch(p -> p.equals("playground3")));
        assertTrue(playgrounds.stream().anyMatch(p -> p.equals("playground_zero")));
        assertEquals(4, playgrounds.size());

        assertEquals(1,
                Information.getMyself().getElements(new CommonParameters(),
                        Information.getMyself().getPlayground("playground_zero")).count());
        assertEquals(1,
                Information.getMyself().getElements(new CommonParameters(),
                        Information.getMyself().getPlayground("playground2")).count());
        assertEquals(1,
                Information.getMyself().getElements(new CommonParameters(),
                        Information.getMyself().getPlayground("playground3")).count());
    }

}
