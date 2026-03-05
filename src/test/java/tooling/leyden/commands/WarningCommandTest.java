package tooling.leyden.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import tooling.leyden.aotcache.Warning;
import tooling.leyden.commands.logparser.AOTMapParser;
import tooling.leyden.commands.logparser.TrainingLogParser;

@QuarkusTest
class WarningCommandTest extends DefaultTest {

    private static WarningCommand warningCommand;
    private static AOTMapParser aotCacheParser;
    private static TrainingLogParser trainingParser;

    @BeforeAll
    static void init() {
        final var loadFile = new LoadFileCommand();
        loadFile.setParent(getDefaultCommand());
        trainingParser = new TrainingLogParser(loadFile);
        aotCacheParser = new AOTMapParser(loadFile);
        warningCommand = new WarningCommand();
        warningCommand.parent = getDefaultCommand();
    }

    @Test
    void checkUsedAndNotTrained() {
        aotCacheParser.accept("0x0000000801711128: @@ Class             624 org.infinispan.xsite.NoOpBackupSender");

        //Method not called
        aotCacheParser.accept(
                "0x00000008017116c0: @@ Method            88 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");

        //Method with no compiledTrainingData
        aotCacheParser
                .accept("0x0000000801711610: @@ Method            88 void org.infinispan.xsite.NoOpBackupSender.<init>()");
        aotCacheParser.accept(
                "0x0000000801b3d568: @@ MethodTrainingData 96 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");
        aotCacheParser.accept(
                "0x0000000801b3d5f0: @@ MethodData        240 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");
        aotCacheParser.accept(
                "0x0000000801b3d6e0: @@ MethodCounters    64 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");
        aotCacheParser.accept(
                "0x000000080429e780: @@ ConstMethod       64 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");

        List<Warning> warningList = warningCommand.getTopPackagesUsedAndNotTrained();
        assertEquals(1, warningList.size());

        //Now we add the compiledTrainingData
        aotCacheParser.accept(
                "0x0000000801cd5648: @@ CompileTrainingData 80 1 org.infinispan.xsite.NoOpBackupSender org.infinispan.xsite.NoOpBackupSender.getInstance()");

        assertTrue(warningCommand.getTopPackagesUsedAndNotTrained().isEmpty());
    }

    @Test
    void filter() {

        trainingParser.accept("[warning][aot] Skipping org/apache/logging/log4j/core/async/AsyncLoggerContext: Failed " +
                "verification");
        trainingParser.accept("[warning][aot] Skipping com/thoughtworks/xstream/security/ForbiddenClassException: Unlinked " +
                "class not supported by AOTConfiguration");
        trainingParser.accept("[info][aot       ] JVM_StartThread() ignored: java.lang.ref.Reference$ReferenceHandler");
        trainingParser.accept("[warning][aot       ] Preload Warning: Verification failed for org.infinispan.remoting" +
                ".transport.jgroups.JGroupsRaftManager");
        trainingParser.accept("[warning][aot       ] Preload Warning: Verification failed for org.apache.logging.log4j.core" +
                ".async.AsyncLoggerContext");

        assertEquals(5, warningCommand.getWarnings().size());

        warningCommand.limit = 1;
        assertEquals(1, warningCommand.getWarnings().size());

        warningCommand.limit = null;
        warningCommand.name = "org.apache.logging.log4j.core.async.AsyncLoggerContext";
        assertEquals(2, warningCommand.getWarnings().size());

        warningCommand.name = "java.lang.ref.Reference$ReferenceHandler";
        assertEquals(1, warningCommand.getWarnings().size());
        warningCommand.name = null;
    }

    @Test
    void testRevertedAot() {
        trainingParser.accept("[trace  ][aot,resolve ] reverted indy   CP entry [129]: " +
                "org/infinispan/notifications/cachelistener/BaseQueueingSegmentListener (1)    " +
                "java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;" +
                "Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");
        trainingParser.accept("[trace  ][aot,resolve ] reverted indy   CP entry [147]: " +
                "org/infinispan/notifications/cachelistener/BaseQueueingSegmentListener (2)    " +
                "java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");
        trainingParser.accept("[trace  ][aot,resolve ] reverted indy   CP entry [150]: " +
                "org/infinispan/notifications/cachelistener/BaseQueueingSegmentListener (3)    java/lang/invoke/LambdaMetafactory.metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");

        trainingParser.accept("[trace  ][aot,resolve ] reverted method CP entry [  6]: " +
                "io/reactivex/rxjava3/internal/jdk8/FlowableStageSubscriber " +
                "io/reactivex/rxjava3/internal/jdk8/FlowableStageSubscriber.afterSubscribe:" +
                "(Lorg/reactivestreams/Subscription;)V");
        trainingParser.accept("[trace  ][aot,resolve ] reverted method CP entry [ 13]: " +
                "io/reactivex/rxjava3/internal/jdk8/FlowableStageSubscriber " +
                "java/util/concurrent/atomic/AtomicReference.lazySet:(Ljava/lang/Object;)V");
        trainingParser.accept("[trace  ][aot,resolve ] reverted method CP entry [ 14]: " +
                "io/reactivex/rxjava3/internal/jdk8/FlowableStageSubscriber io/reactivex/rxjava3/internal/jdk8/FlowableStageSubscriber.cancelUpstream:()V");

        trainingParser.accept("[trace  ][aot,resolve ] reverted klass  CP entry [  8]: " +
                "io/reactivex/rxjava3/internal/jdk8/ObservableCollectWithCollector unreg => java/lang/Throwable");
        trainingParser.accept("[trace  ][aot,resolve ] reverted klass  CP entry [  7]: " +
                "io/reactivex/rxjava3/internal/operators/observable/ObservableCollect unreg => " +
                "java/lang/Throwable");
        trainingParser.accept("[trace  ][aot,resolve ] reverted klass  CP entry [  7]: " +
                "io/reactivex/rxjava3/internal/operators/observable/ObservableToList unreg => java/lang/Throwable");

        assertEquals(9, warningCommand.getWarnings().size());

        warningCommand.name = "java.util.concurrent.atomic.AtomicReference";
        assertEquals(1, warningCommand.getWarnings().size());
        warningCommand.name = null;

    }
}
