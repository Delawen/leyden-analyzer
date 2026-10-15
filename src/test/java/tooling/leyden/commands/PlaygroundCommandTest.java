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

@QuarkusTest
class PlaygroundCommandTest extends DefaultTest {

    @Test
    void diff() {
        String mapFile = """
AOT cache map for playground1
0x00000008003eb000: @@ Class             1000 java.math.BigInteger
0x00000008003ed000: @@ Class             520 [Ljava.math.BigInteger;
0x00000008003ed400: @@ Class             520 [[Ljava.math.BigInteger;
0x00000008001d3cf8: @@ Class             528 java.lang.Object
0x00007fbcf7ffeff4: @@ StubGenBlob       25896 id=73 initial_blob (stub gen)
0x00007fbcf7ffefc8: @@ SharedBlob        403 id=13 throw_StackOverflowError_blob (shared runtime)
0x00007fbcf7ffef9c: @@ StubGenBlob       3621 id=74 continuation_blob (stub gen)
0x00007fbcf7ffed34: @@ SharedBlob        2156 id=0 deopt_blob (shared runtime)
0x00007fbcf7ffe49c: @@ StubGenBlob       129373 id=75 compiler_blob (stub gen)
0x00007fbcf7ffe470: @@ StubGenBlob       59182 id=76 final_blob (stub gen)
0x00007fbcf7ffcc8c: @@ C1Blob            650 id=17 dtrace_object_alloc_blob (C1 runtime)
0x00007fbcf7ffcc60: @@ C1Blob            387 id=18 unwind_exception_blob (C1 runtime)
0x00007fbcf7ffbe74: @@ C2Blob            335 id=70 vthread_start_transition_blob (C2 runtime)
0x00007fbcf7ffbe48: @@ Adapter           1333 id=217 ILIL
0x00007fbcf7ffbe1c: @@ Adapter           1376 id=218 LIIILLL
0x00007fbcf7ffbdf0: @@ C2Blob            333 id=71 vthread_end_transition_blob (C2 runtime)
0x00007fbcf7ffbdc4: @@ Adapter           1460 id=219 LLLIILILLII
0x00007fbcf7ffbd98: @@ Adapter           1342 id=220 L
0x00000008003ec258: @@ Method            112 java.math.BigInteger java.math.BigInteger.subtract(java.math.BigInteger)
0x00000008003eebd0: @@ Method            112 int[] java.math.BigInteger.subtract(long, int[])
0x00000008003eecd8: @@ Method            112 int[] java.math.BigInteger.subtract(int[], int[])
0x00000008008095e8: @@ Method            88 void java.lang.Object.wait()
0x0000000801318e00: @@ ConstMethod       88 void java.lang.Object.wait()
0x00000008012d6d58: @@ ConstMethod       256 java.math.BigInteger java.math.BigInteger.subtract(java.math.BigInteger)
0x00000008012db2f8: @@ ConstMethod       344 int[] java.math.BigInteger.subtract(long, int[])
0x00000008012db5b8: @@ ConstMethod       296 int[] java.math.BigInteger.subtract(int[], int[])
0x00007fe0fbff6293: @@ Nmethod           15469 level=3 id=123 java.lang.Object.wait() Z
0x00007fe0fbff6b70: @@ Nmethod           15469 level=4 id=1298 java.math.BigInteger.subtract([I[I)[I
0x00007fe0fbfe8890: @@ Nmethod           6552 level=2 id=2131 java.math.BigInteger.subtract(Ljava/math/BigInteger;)Ljava/math/BigInteger;
0x00007fe0fbfe28d4: @@ Nmethod           11533 level=4 id=3154 java.math.BigInteger.subtract([I[I)[I
0x00007f9ab3fe6c60: @@ Nmethod           2444 level=2 id=2550 java.util.Arrays.sort([Ljava/lang/Object;Ljava/util/Comparator;)V
AOT cache map for playground2
0x00000008005e30e8: @@ Class             672 java.time.Month
0x00000008006107c8: @@ Class             600 net.bytebuddy.jar.asm.commons.AnnotationRemapper
0x0000000800610e88: @@ Class             608 net.bytebuddy.jar.asm.commons.ModuleRemapper
0x0000000800611448: @@ Class             592 net.bytebuddy.jar.asm.commons.RecordComponentRemapper
0x00000008003eb000: @@ Class             1000 java.math.BigInteger
0x00000008003ed000: @@ Class             520 [Ljava.math.BigInteger;
0x00000008003ed400: @@ Class             520 [[Ljava.math.BigInteger;
0x00007fbcf7ffeff4: @@ StubGenBlob       25896 id=73 initial_blob (stub gen)
0x00007fbcf7ffefc8: @@ SharedBlob        403 id=13 throw_StackOverflowError_blob (shared runtime)
0x00007fbcf7ffef9c: @@ StubGenBlob       3621 id=74 continuation_blob (stub gen)
0x00007fbcf7ffed34: @@ SharedBlob        2156 id=0 deopt_blob (shared runtime)
0x00007fbcf7ffe49c: @@ StubGenBlob       129373 id=75 compiler_blob (stub gen)
0x00007fbcf7ffe470: @@ StubGenBlob       59182 id=76 final_blob (stub gen)
0x00007fbcf7ffcc8c: @@ C1Blob            650 id=17 dtrace_object_alloc_blob (C1 runtime)
0x00007fbcf7ffcc60: @@ C1Blob            387 id=18 unwind_exception_blob (C1 runtime)
0x00007fbcf7ffbe74: @@ C2Blob            335 id=70 vthread_start_transition_blob (C2 runtime)
0x00007fbcf7ffbe48: @@ Adapter           1333 id=217 ILIL
0x00007fbcf7ffbe1c: @@ Adapter           1376 id=218 LIIILLL
0x00007fbcf7ffbdf0: @@ C2Blob            333 id=71 vthread_end_transition_blob (C2 runtime)
0x00007fbcf7ffbdc4: @@ Adapter           1460 id=219 LLLIILILLII
0x00007fbcf7ffbd98: @@ Adapter           1342 id=220 L
0x00000008003ec258: @@ Method            112 java.math.BigInteger java.math.BigInteger.subtract(java.math.BigInteger)
0x00000008003eebd0: @@ Method            112 int[] java.math.BigInteger.subtract(long, int[])
0x00000008003eecd8: @@ Method            112 int[] java.math.BigInteger.subtract(int[], int[])
0x00000008012d6d58: @@ ConstMethod       256 java.math.BigInteger java.math.BigInteger.subtract(java.math.BigInteger)
0x00000008012db2f8: @@ ConstMethod       344 int[] java.math.BigInteger.subtract(long, int[])
0x00000008012db5b8: @@ ConstMethod       296 int[] java.math.BigInteger.subtract(int[], int[])
0x00007fe0fbff6b70: @@ Nmethod           15469 level=4 id=1298 java.math.BigInteger.subtract([I[I)[I
0x00007fe0fbfe8890: @@ Nmethod           6552 level=2 id=2131 java.math.BigInteger.subtract(Ljava/math/BigInteger;)Ljava/math/BigInteger;
                """;

        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        AOTMapParser aotCacheParser = new AOTMapParser(loadFile);
        BufferedReader reader = new BufferedReader(new StringReader(mapFile));
        reader.lines().forEach(aotCacheParser::accept);
        aotCacheParser.postProcessing();

        PlaygroundCommand command = new PlaygroundCommand();
        command.parent = getDefaultCommand();
        Playground playground1 = Information.getMyself().getPlayground("playground1");
        Playground playground2 = Information.getMyself().getPlayground("playground2");

        var output = """
Classes stored on the cache         playground1         playground2
                                       4                   7

  💡  Use the option --verbose to see the diff list of classes.
    """;

        assertEquals(output, command.printClassesComparison(playground1, playground2).toString());


        output = """
Classes stored on the cache         playground1         playground2
                                       4                   7

Classes in playground1 and not in playground2:
java.lang.Object
                
Classes in playground2 and not in playground1:
net.bytebuddy.jar.asm.commons.RecordComponentRemapper
java.time.Month
net.bytebuddy.jar.asm.commons.AnnotationRemapper
net.bytebuddy.jar.asm.commons.ModuleRemapper

""";

        command.verbose = true;
        assertEquals(output, command.printClassesComparison(playground1, playground2).toString());

        output = """
Summary Count                playground1         playground2
             Adapter                   4                   4
              C1Blob                   2                   2
              C2Blob                   2                   2
               Class                   6                   7
         ConstMethod                   4                   3
              Method                   6                   3
             NMethod                   5                   2
          SharedBlob                   2                   2
         StubGenBlob                   4                   4
             """;
        assertEquals(output, command.printDetailedCountComparison(playground1, playground2).toString());

        output = """
Compiled Methods by Level              playground1              playground2
                  Level 2                        2                        1
                  Level 3                        1                        0
                  Level 4                        2                        1
""";
        assertEquals(output, command.printMethodsCompiledComparison(playground1, playground2).toString());
    }


}
