package tooling.leyden.commands;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import tooling.leyden.commands.logparser.AOTMapParser;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class DescribeCommandTest extends DefaultTest {

    @Test
    void codeCache() {
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
        DescribeCommand command = new DescribeCommand();
        command.parent = getDefaultCommand();
        command.parameters = new CommonParameters();
        command.parameters.setName("int[] java.math.BigInteger.subtract(int[], int[])");
        command.parameters.setTypes(new String[] {"Method"});
        command.parameters.verbose = true;
        command.parameters.hints = true;

        var output = """
                -----
                  Method int[] java.math.BigInteger.subtract(int[], int[]) on address 0x00000008003eecd8 with size 112.
                  Training Information:\s
                    It has no MethodCounters associated to it.
                      ℹ️  This method doesn't seem to have been called significantly during training run.
                    It has no MethodData associated to it.
                      ℹ️  This means it may be profiled, but not ready to be compiled on a high level.
                      💡  If this is a key method in your app, you should have this asset.
                    It has no CompileTrainingData associated to it.
                      ℹ️  This method was not considered for optimization during training run.
                    It has no MethodTrainingData associated to it.
                      💡  If you think the training for this method is not good enough, make sure your training run use it more, as it would on a long production run.
                
                  References:\s
                    Assets referenced from this asset:\s
                       [Cached][Untrained][Class]   java.math.BigInteger
                    Assets that refer to this one:\s
                       [Cached][Trained][NMethod]     (4) [1298] java.math.BigInteger.subtract([I[I)[I
                       [Cached][Trained][NMethod]     (4) [3154] java.math.BigInteger.subtract([I[I)[I
                
                  This information comes from:\s
                    > AOT Map
                
                  This element has no warnings.
                -----
                """;
        assertEquals(output, command.searchAndPrintElements().toString());


        command.parameters.setName("(4) [3154] java.math.BigInteger.subtract([I[I)[I");
        command.parameters.setTypes(null);

        output = """
             -----
               NMethod (4) [3154] java.math.BigInteger.subtract([I[I)[I on address 0x00007fe0fbfe28d4 with size 11533.
               This is part of the Code Cache. Code Cache ID is 3154
               This is the compiled code of the method int[] java.math.BigInteger.subtract(int[], int[]).
               Compilation Level: 4
                 ℹ️  Higher compilation levels mean a more optimized compilation.
                 💡  Key methods should aim for compilation 3 or above.
               The adapter signature that would be used to call this method is 'II  '.
        
        
               References:\s
                 Assets referenced from this asset:\s
                    [Cached][Untrained][Method]   int[] java.math.BigInteger.subtract(int[], int[])
        
                 There are no assets that refer to this one.
        
               This information comes from:\s
                 > AOT Map
        
               This element has the following warnings:\s
                 > 0001 [CacheCreation] This nmethod does not have an adapter.
             -----
             """;
        assertEquals(output, command.searchAndPrintElements().toString());
    }

    @Test
    void testError() {
        DescribeCommand command = new DescribeCommand();
        command.parameters = new CommonParameters();
        command.parameters.setName("Nope");
        assertEquals("ERROR: Element '" + command.parameters.getName() +
                "' not found. Try looking for it with ls.", command.printElements(List.of()).toString());
    }

}
