package tooling.leyden.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import tooling.leyden.aotcache.*;
import tooling.leyden.commands.autocomplete.WhichRun;
import tooling.leyden.commands.logparser.AOTMapParser;

@QuarkusTest
class ListCommandTest extends DefaultTest {

    @Test
    void counters() {
        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        AOTMapParser aotCacheParser = new AOTMapParser(loadFile);

        aotCacheParser.accept("0x0000000801711128: @@ Class             624 org.infinispan.xsite.NoOpBackupSender");
        aotCacheParser.accept("0x00000008017113f0: @@ ConstantPoolCache 64 org.infinispan.xsite.NoOpBackupSender");
        aotCacheParser.accept(
                "0x00000008017116c0: @@ Method            88 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");
        aotCacheParser.accept("0x00000008017115b8: @@ Method            88 org.infinispan.interceptors" +
                ".InvocationStage org.infinispan.xsite.NoOpBackupSender.backupClear(org.infinispan.commands.write.ClearCommand)");
        aotCacheParser.accept("0x0000000801b3d6e0: @@ MethodCounters    64 org.infinispan.interceptors" +
                ".InvocationStage org.infinispan.xsite.NoOpBackupSender.backupClear(org.infinispan.commands.write.ClearCommand)");
        aotCacheParser
                .accept("0x0000000801711610: @@ Method            88 void org.infinispan.xsite.NoOpBackupSender.<init>()");
        aotCacheParser
                .accept("0x0000000801711668: @@ Method            88 void org.infinispan.xsite.NoOpBackupSender.<clinit>()");

        var detailedCount = Information.getMyself().getDetailedCount();
        assertTrue(detailedCount.containsKey("Class"));
        assertTrue(detailedCount.containsKey("ConstantPool"));
        assertTrue(detailedCount.containsKey("Method"));
        assertTrue(detailedCount.containsKey("MethodCounters"));
        assertEquals(4, detailedCount.size());
        assertEquals(1, detailedCount.get("Class"));
        assertEquals(1, detailedCount.get("ConstantPool"));
        assertEquals(4, detailedCount.get("Method"));
        assertEquals(1, detailedCount.get("MethodCounters"));
    }

    @Test
    void checkUsedAndNotTrained() {
        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        AOTMapParser aotCacheParser = new AOTMapParser(loadFile);

        aotCacheParser.accept("0x0000000801711128: @@ Class             624 org.infinispan.xsite.NoOpBackupSender");
        aotCacheParser.accept("0x00000008017113f0: @@ ConstantPoolCache 64 org.infinispan.xsite.NoOpBackupSender");
        aotCacheParser.accept(
                "0x00000008017116c0: @@ Method            88 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");
        aotCacheParser.accept("0x00000008017115b8: @@ Method            88 org.infinispan.interceptors" +
                ".InvocationStage org.infinispan.xsite.NoOpBackupSender.backupClear(org.infinispan.commands.write.ClearCommand)");
        aotCacheParser.accept("0x0000000801b3d6e0: @@ MethodCounters    64 org.infinispan.interceptors" +
                ".InvocationStage org.infinispan.xsite.NoOpBackupSender.backupClear(org.infinispan.commands.write.ClearCommand)");
        aotCacheParser
                .accept("0x0000000801711610: @@ Method            88 void org.infinispan.xsite.NoOpBackupSender.<init>()");
        aotCacheParser
                .accept("0x0000000801711668: @@ Method            88 void org.infinispan.xsite.NoOpBackupSender.<clinit>()");
        aotCacheParser.accept(
                "0x0000000801b3d568: @@ MethodTrainingData 96 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");
        aotCacheParser.accept(
                "0x0000000801b3d5f0: @@ MethodData        240 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");
        aotCacheParser.accept(
                "0x0000000801b3d6e0: @@ MethodCounters    64 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");
        aotCacheParser.accept("0x0000000802305250: @@ Symbol            48 org.infinispan.xsite.NoOpBackupSender)");
        aotCacheParser.accept("0x00000008025befb8: @@ Symbol            48 org/infinispan/xsite/NoOpBackupSender)");
        aotCacheParser.accept("0x00000008025befe8: @@ Symbol            48 ()Lorg/infinispan/xsite/NoOpBackupSender;)");
        aotCacheParser.accept("0x00000008026534d0: @@ Symbol            48 Lorg/infinispan/xsite/NoOpBackupSender;)");
        aotCacheParser.accept(
                "0x000000080429e1e0: @@ ConstMethod       80 java.lang.String org.infinispan.xsite.NoOpBackupSender.toString())");
        aotCacheParser.accept("0x00000008017115b8: @@ ConstMethod            88 org.infinispan.interceptors" +
                ".InvocationStage org.infinispan.xsite.NoOpBackupSender.backupClear(org.infinispan.commands.write.ClearCommand)");
        aotCacheParser.accept("0x000000080429e230: @@ ConstantPool      568 org.infinispan.xsite.NoOpBackupSender)");
        aotCacheParser.accept(
                "0x000000080429e780: @@ ConstMethod       64 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");
        aotCacheParser.accept(
                "0x0000000801cd5648: @@ CompileTrainingData 80 1 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");

        aotCacheParser.accept("0x0000000801711128: @@ Class             624 java.lang.UnsupportedOperationException");
        aotCacheParser.accept("0x0000000801bb65c8: @@ KlassTrainingData 40 java.lang.UnsupportedOperationException");

        ListCommand command = new ListCommand();
        command.parent = getDefaultCommand();
        command.parameters = new CommonParameters();
        command.parameters.lambdas = true;
        command.parameters.innerClasses = true;
        command.parameters.trained = false;
        command.parameters.loaded = WhichRun.all;

        var count = new AtomicInteger();
        assertTrue(command.findElements(count).allMatch(e -> !e.isTrained()));
        assertEquals(4, count.get());

        count = new AtomicInteger();
        command.parameters.types = new String[] { "Class" };
        assertTrue(command.findElements(count).allMatch(e -> e instanceof ClassObject));
        assertEquals(1, count.get());

        count = new AtomicInteger();
        command.parameters.types = new String[] { "Class", "Method" };
        assertTrue(command.findElements(count).allMatch(e -> e instanceof ClassObject || e instanceof MethodObject));
        assertEquals(4, count.get());

        command.parameters.types = new String[] { "Method" };
        count = new AtomicInteger();
        assertTrue(command.findElements(count).allMatch(Element::isTraineable));
        assertEquals(3, count.get());

        command.parameters.trained = true;
        count = new AtomicInteger();
        assertTrue(command.findElements(count).allMatch(Element::isTrained));
        assertEquals(1, count.get());


        command.parameters.types = null;
        command.parameters.trained = null;
        command.parameters.setNameLike("(.)*interceptors(.)*");
        count = new AtomicInteger();
        assertTrue(command.findElements(count).allMatch(e -> e.getKey().contains("interceptors")));
        assertEquals(3, count.get());
    }

    @Test
    void filterLambdasAndInnerClasses() {

        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        AOTMapParser aotCacheParser = new AOTMapParser(loadFile);

        aotCacheParser.accept("0x0000000801b99518: @@ Class             584 io.vertx.core.net.impl.SSLHelper");
        aotCacheParser.accept("0x0000000801baacf8: @@ Class             544 io.vertx.core.net.impl.SSLHelper$CachedProvider");
        aotCacheParser.accept("0x0000000801baafd8: @@ Class             544 io.vertx.core.net.impl.SSLHelper$EngineConfig");
        aotCacheParser
                .accept("0x0000000801eeae98: @@ Class             584 io.vertx.core.net.impl.SSLHelper$$Lambda/0x800000397");
        aotCacheParser
                .accept("0x0000000801eeb208: @@ Class             552 io.vertx.core.net.impl.SSLHelper$$Lambda/0x800000398");

        ListCommand command = new ListCommand();
        command.parent = getDefaultCommand();
        command.parameters = new CommonParameters();
        command.parameters.lambdas = true;
        command.parameters.innerClasses = true;
        command.parameters.loaded = WhichRun.all;
        assertEquals(5, command.findElements(new AtomicInteger()).count());

        command.parameters.lambdas = false;
        assertEquals(3, command.findElements(new AtomicInteger()).count());

        command.parameters.innerClasses = false;
        assertEquals(1, command.findElements(new AtomicInteger()).count());

        command.parameters.address = "0x0000000801eeb208";
        command.parameters.lambdas = true;
        command.parameters.innerClasses = true;
        AtomicInteger atin = new AtomicInteger();
        assertTrue(command.findElements(atin).allMatch(e -> e.getAddress().equals(command.parameters.address)));
        assertEquals(1, atin.getOpaque());
    }

    @Test
    void filterAOTInited() {

        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        AOTMapParser aotCacheParser = new AOTMapParser(loadFile);

        aotCacheParser.accept("0x00000000ffdf4f38: @@ Object (0xffdf4f38) java.lang.Class Ljava/util/ArrayList; (aot-inited)");
        aotCacheParser.accept("0x00000000ffe5d0a0: @@ Object (0xffe5d0a0) java.lang.Integer");

        ListCommand command = new ListCommand();
        command.parent = getDefaultCommand();
        command.parameters = new CommonParameters();
        AtomicInteger atomicInteger = new AtomicInteger();
        var elements = command.findElements(atomicInteger);
        assertTrue(elements.allMatch(e -> e instanceof InstanceObject));
        assertEquals(2, atomicInteger.get());

        command.parameters.showAOTInited = false;
        atomicInteger = new AtomicInteger();
        elements = command.findElements(atomicInteger);
        assertTrue(elements.allMatch(e -> !((InstanceObject) e).isAOTinited()));
        assertEquals(1, atomicInteger.get());

        command.parameters.showAOTInited = true;
        atomicInteger = new AtomicInteger();
        elements = command.findElements(atomicInteger);
        assertTrue(elements.allMatch(e -> ((InstanceObject) e).isAOTinited()));
        assertEquals(1, atomicInteger.get());
    }

    @Test
    void filterInstanceOf() {

        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        AOTMapParser aotCacheParser = new AOTMapParser(loadFile);

        aotCacheParser.accept("0x00000008007f4648: @@ Class             512 java.lang.String");
        aotCacheParser.accept("0x00000008007f5a48: @@ Class             776 java.lang.Class");
        aotCacheParser.accept("0x000000080081b748: @@ Class             648 java.lang.Integer");
        aotCacheParser.accept("0x00000000ffdf4f38: @@ Object (0xffdf4f38) java.lang.Class Ljava/util/ArrayList; (aot-inited)");
        aotCacheParser.accept("0x00000000ffe5d0a0: @@ Object (0xffe5d0a0) java.lang.Integer");
        aotCacheParser.accept("0x00000000ffd0a4c8: @@ Object (0xffd0a4c8) java.lang.String \"| resolve\"");

        ListCommand command = new ListCommand();
        command.parent = getDefaultCommand();
        command.parameters = new CommonParameters();
        command.parameters.setTypes(new String[] { "Object" });
        AtomicInteger atomicInteger = new AtomicInteger();
        var els = command.findElements(atomicInteger);
        assertTrue(els.allMatch(e -> e.getType().equals("Object")));
        assertEquals(3, atomicInteger.get());

        command.parameters.instanceOf = "java.lang.Class";
        atomicInteger = new AtomicInteger();
        els = command.findElements(atomicInteger);
        assertTrue(els.allMatch(e -> ((InstanceObject)e).getInstanceOf().getKey().equals(command.parameters.instanceOf)));
        assertEquals(1, atomicInteger.get());

        command.parameters.instanceOf = "java.lang.String";
        atomicInteger = new AtomicInteger();
        els = command.findElements(atomicInteger);
        assertTrue(els.allMatch(e -> ((InstanceObject)e).getInstanceOf().getKey().equals(command.parameters.instanceOf)));
        assertEquals(1, atomicInteger.get());

        command.parameters.instanceOf = "java.lang.Integer";
        atomicInteger = new AtomicInteger();
        els = command.findElements(atomicInteger);
        assertTrue(els.allMatch(e -> ((InstanceObject)e).getInstanceOf().getKey().equals(command.parameters.instanceOf)));
        assertEquals(1, atomicInteger.get());

        command.parameters.instanceOf = "java.util.ArrayList";
        atomicInteger = new AtomicInteger();
        els = command.findElements(atomicInteger);
        assertTrue(els.allMatch(e -> ((InstanceObject)e).getInstanceOf().getKey().equals(command.parameters.instanceOf)));
        assertEquals(0, atomicInteger.get());
    }

}
