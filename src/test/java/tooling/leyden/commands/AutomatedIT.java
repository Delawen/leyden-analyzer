package tooling.leyden.commands;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutomatedIT {

    @Test
    public void commands1() throws Exception {
        final String nativeExePath = System.getProperty("native.image.path");
        if (nativeExePath == null) {
            throw new IllegalStateException("native.image.path is not set. Are you running with -Pnative?");
        }
        final ProcessBuilder pb = new ProcessBuilder(
                nativeExePath,
                "aotCache=aot.map",
                "productionLog=production.log",
                "trainingLog=training.log" ,
                "instructions=src" + File.separator + "test" + File.separator +
                        "resources" + File.separator + "instructions" + File.separator + "commands1.txt");
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        process.waitFor(5, TimeUnit.MINUTES);
        final String errorOutput;
        try (InputStream stdout = process.getErrorStream()) {
            errorOutput = new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
        }
        System.out.println(errorOutput);
        final String output;
        try (InputStream stdout = process.getInputStream()) {
            output = new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertNotNull(output);
        System.out.println(output);
        assertTrue(output.contains("Executing automated commands..."), "No automated commands found.");
        assertTrue(output.contains("PRODUCTION RUN:"));
        assertTrue(output.contains("Classes loaded: "));
        assertTrue(output.contains("-> Cached:"));
        assertTrue(output.contains("  -> Not Cached:"));
        assertTrue(output.contains("  -> Cached and not used:"));
        assertTrue(output.contains("Lambda Methods:"));
        assertTrue(output.contains("  -> Cached:"));
        assertTrue(output.contains("  -> Not Cached:"));
        assertTrue(output.contains("AOT CACHE:"));
        assertTrue(output.contains("Metadata:"));
        assertTrue(output.contains(" - Classes in AOT Cache:"));
        assertTrue(output.contains("    -> KlassTrainingData:"));
        assertTrue(output.contains(" - Objects in AOT Cache:"));
        assertTrue(output.contains("    -> AOT-inited:"));
        assertTrue(output.contains("    -> java.lang.Class instances:"));
        assertTrue(output.contains("    -> java.lang.String instances:"));
        assertTrue(output.contains(" - Methods in AOT Cache:"));
        assertTrue(output.contains("    -> MethodCounters:"));
        assertTrue(output.contains("    -> MethodData:"));
        assertTrue(output.contains("    -> MethodTrainingData:"));
        assertTrue(output.contains("  -> CompileTrainingData:"));
    }
}
