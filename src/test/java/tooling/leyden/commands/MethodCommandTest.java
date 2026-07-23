package tooling.leyden.commands;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tooling.leyden.aotcache.NMethodObject;
import tooling.leyden.commands.autocomplete.WhichRun;
import tooling.leyden.commands.logparser.AOTMapParser;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MethodCommandTest extends DefaultTest {

    private MethodCommand command = new MethodCommand();
    private AtomicInteger counter;

    @BeforeEach
    void loadExampleData() {
        String mapfile = """
0x00000008003eb000: @@ Class             1000 java.math.BigInteger
0x00000008003ed000: @@ Class             520 [Ljava.math.BigInteger;
0x00000008003ed400: @@ Class             520 [[Ljava.math.BigInteger;
0x00007fbcf7ffeff4: @@ StubGenBlob       25896 73 initial_blob (stub gen)
0x00007fbcf7ffefc8: @@ SharedBlob        403 13 throw_StackOverflowError_blob (shared runtime)
0x00007fbcf7ffef9c: @@ StubGenBlob       3621 74 continuation_blob (stub gen)
0x00007fbcf7ffed34: @@ SharedBlob        2156 0 deopt_blob (shared runtime)
0x00007fbcf7ffe49c: @@ StubGenBlob       129373 75 compiler_blob (stub gen)
0x00007fbcf7ffe470: @@ StubGenBlob       59182 76 final_blob (stub gen)
0x00007fbcf7ffcc8c: @@ C1Blob            650 17 dtrace_object_alloc_blob (C1 runtime)
0x00007fbcf7ffcc60: @@ C1Blob            387 18 unwind_exception_blob (C1 runtime)
0x00007fbcf7ffbe74: @@ C2Blob            335 70 vthread_start_transition_blob (C2 runtime)
0x00007fbcf7ffbe48: @@ Adapter           1333 217 ILIL
0x00007fbcf7ffbe1c: @@ Adapter           1376 218 LIIILLL
0x00007fbcf7ffbdf0: @@ C2Blob            333 71 vthread_end_transition_blob (C2 runtime)
0x00007fbcf7ffbdc4: @@ Adapter           1460 219 LLLIILILLII
0x00007fbcf7ffbd98: @@ Adapter           1342 220 L
0x00000008003ec258: @@ Method            112 java.math.BigInteger java.math.BigInteger.subtract(java.math.BigInteger)
0x00000008003eebd0: @@ Method            112 int[] java.math.BigInteger.subtract(long, int[])
0x00000008003eecd8: @@ Method            112 int[] java.math.BigInteger.subtract(int[], int[])
0x0000000801cb2438: @@ CompileTrainingData 80 3 int[] java.math.BigInteger.subtract(int[], int[])
0x00000008012d6d58: @@ ConstMethod       256 java.math.BigInteger java.math.BigInteger.subtract(java.math.BigInteger)
0x00000008012db2f8: @@ ConstMethod       344 int[] java.math.BigInteger.subtract(long, int[])
0x00000008012db5b8: @@ ConstMethod       296 int[] java.math.BigInteger.subtract(int[], int[])
0x00007fe0fbff6b70: @@ Nmethod           15469 4 1298 java.math.BigInteger.subtract([I[I)[I
0x00007fe0fbfe8890: @@ Nmethod           6552 2 2131 java.math.BigInteger.subtract(Ljava/math/BigInteger;)Ljava/math/BigInteger;
0x00007fe0fbfe28d4: @@ Nmethod           11533 4 3154 java.math.BigInteger.subtract([I[I)[I
0x00007f9ab3fe6c60: @@ Nmethod           2444 2 2550 java.util.Arrays.sort([Ljava/lang/Object;Ljava/util/Comparator;)V
                """;

        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        AOTMapParser aotCacheParser = new AOTMapParser(loadFile);
        BufferedReader reader = new BufferedReader(new StringReader(mapfile));
        reader.lines().forEach(aotCacheParser::accept);
        aotCacheParser.postProcessing();
        command.parent = getDefaultCommand();
        command.parameters = new CommonParameters();
        counter = new AtomicInteger();
    }

    @Test
    void showAllMethods() {
        command.parameters = new CommonParameters();
        command.parameters.loaded = WhichRun.all;
        var elements =  command.findElements(counter).toList();
        assertEquals(3, counter.get());
        assertTrue(elements.stream().allMatch(e -> e.getType().equalsIgnoreCase("Method")));
        assertTrue(elements.stream().anyMatch(e ->
                e.getKey().equals("java.math.BigInteger java.math.BigInteger.subtract(java.math.BigInteger)")));
        assertTrue(elements.stream().anyMatch(e ->
                e.getKey().equals("int[] java.math.BigInteger.subtract(long, int[])")));
        assertTrue(elements.stream().anyMatch(e ->
                e.getKey().equals("int[] java.math.BigInteger.subtract(int[], int[])")));
    }

    @Test
    void showTrainedMethods() {
        command.parameters = new CommonParameters();
        command.parameters.loaded = WhichRun.all;
        command.parameters.trained = true;
        var elements =  command.findElements(counter).toList();
        assertEquals(1, counter.get());
        assertTrue(elements.stream().allMatch(e -> e.isTrained()));

        command.parameters.trained = false;
        counter = new AtomicInteger();
        elements =  command.findElements(counter).toList();
        assertEquals(2, counter.get());
        assertTrue(elements.stream().noneMatch(e -> e.isTrained()));
    }

    @Test
    void hasBeenCompiled() {
        command.parameters = new CommonParameters();
        command.parameters.loaded = WhichRun.all;
        command.hasBeenCompiled = true;
        var elements =  command.findElements(counter).toList();
        assertEquals(2, counter.get());
        assertTrue(elements.stream().allMatch(e ->
                e.getWhoReferencesMe().stream().anyMatch(r -> r.getType().equals("NMethod"))));

        command.hasBeenCompiled = false;
        counter = new AtomicInteger();
        elements =  command.findElements(counter).toList();
        assertEquals(1, counter.get());
        assertTrue(elements.stream().allMatch(e ->
                e.getWhoReferencesMe().stream().noneMatch(r -> r.getType().equals("NMethod"))));
    }

    @Test
    void filterByTier() {
        command.parameters = new CommonParameters();
        command.parameters.loaded = WhichRun.all;
        command.tier = -1;
        var elements =  command.findElements(counter).toList();
        assertEquals(0, counter.get());

        command.tier = 2;
        counter = new AtomicInteger();
        elements =  command.findElements(counter).toList();
        assertEquals(1, counter.get());
        assertTrue(elements.stream().allMatch(e ->
                e.getWhoReferencesMe().stream().anyMatch(r ->
                        r.getType().equals("NMethod") && ((NMethodObject)r).getCompilationLevel().equals(2))));

        command.tier = 4;
        counter = new AtomicInteger();
        elements =  command.findElements(counter).toList();
        assertEquals(1, counter.get());
        assertTrue(elements.stream().allMatch(e ->
                e.getWhoReferencesMe().stream().anyMatch(r ->
                        r.getType().equals("NMethod") && ((NMethodObject)r).getCompilationLevel().equals(4))));
    }

    @Test
    void filterByTrainingTier() {
        command.parameters = new CommonParameters();
        command.parameters.loaded = WhichRun.all;
        command.trainingTier = -1;
        command.findElements(counter).toList();
        assertEquals(0, counter.get());

        command.trainingTier = 3;
        counter = new AtomicInteger();
        var elements =  command.findElements(counter).toList();
        assertEquals(1, counter.get());
        assertTrue(elements.stream().allMatch(e -> e.getCompileTrainingData().containsKey(3)));

        command.trainingTier = 2;
        counter = new AtomicInteger();
        command.findElements(counter).toList();
        assertEquals(0, counter.get());

        command.trainingTier = 4;
        elements =  command.findElements(counter).toList();
        assertEquals(0, counter.get());
        assertTrue(elements.stream().allMatch(e -> e.getCompileTrainingData().containsKey(4)));
    }

}
