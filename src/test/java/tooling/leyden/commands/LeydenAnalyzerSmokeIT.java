package tooling.leyden.commands;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusIntegrationTest
public class LeydenAnalyzerSmokeIT {

    @Test
    public void testNativeReplStartupAndExit() throws Exception {
        final String nativeExePath = System.getProperty("native.image.path");
        if (nativeExePath == null) {
            throw new IllegalStateException("native.image.path is not set. Are you running with -Pnative?");
        }
        final ProcessBuilder pb = new ProcessBuilder(nativeExePath);
        pb.redirectErrorStream(true);
        final Process process = pb.start();
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write("help\n".getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        }
        final boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        assertTrue(finished, "Did not exit in time. Hangs?");
        assertEquals(0, process.exitValue(), "Process terminated non-zero exit code.");
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(output.contains("██╗     ███████╗██╗"), "Banner butchered.");
        assertTrue(output.contains("> "), "Prompt is missing.");
        assertTrue(output.contains("Usage: "), "Help command output is missing.");
    }
}
