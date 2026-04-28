package tooling.leyden.commands;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            // Send the help command, newline, and the ASCII code for Ctrl-D (4)
            stdin.write("help\n\u0004".getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        }
        final boolean finished = process.waitFor(3, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
        }
        final String output;
        try (InputStream stdout = process.getInputStream()) {
            output = new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertNotNull(output);
        assertTrue(output.contains("> "), "Prompt is missing.");
        assertTrue(output.contains("Use 'load' to add assets"), "Help command output is missing.");
    }
}
