package tooling.leyden.aotcache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NMethodObjectTest {

    @Test
    void translateSymbols() {
        // We just want to create an nmethod object, we will test the translateSymbols independently of
        // the identifier we pass to the constructor
        NMethodObject nmethod = new NMethodObject("sun.nio.cs.UTF_8.updatePositions(Ljava/nio/Buffer;ILjava/nio/Buffer;I)V");
        var symbols = nmethod.translateParameters("(Ljava/nio/Buffer;ILjava/nio/Buffer;I)");
        assertEquals("java.nio.Buffer, int, java.nio.Buffer, int", symbols);

        symbols = nmethod.translateParameters("(Ljdk.internal.classfile.impl.BufWriterImpl;ILjava.util.List;Ljava.lang.classfile.attribu" +
                "te.StackMapFrameInfo)");
        assertEquals("jdk.internal.classfile.impl.BufWriterImpl, int, java.util.List, java.lang.classfile.attr" +
                "ibute.StackMapFrameInfo", symbols);

        symbols = nmethod.translateParameters("([BII)");
        assertEquals("byte[], int, int", symbols);

        symbols = nmethod.translateParameters("(I[B)");
        assertEquals("int, byte[]", symbols);

        symbols = nmethod.translateParameters("([B)");
        assertEquals("byte[]", symbols);

        symbols = nmethod.translateParameters("([CII[Ljava/lang/foreign/MemorySegment;)");
        assertEquals("char[], int, int, java.lang.foreign.MemorySegment[]", symbols);

        assertEquals("java.math.BigInteger", nmethod.translateSymbols("Ljava/math/BigInteger;"));
    }
}