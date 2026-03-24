package wn.gateway.codex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class StdioCodexProcessSupervisorTest {

    @Test
    void startsCodexMcpServerInWorkspaceDirectoryWithoutChangingCommandArguments() throws Exception {
        Path tempDir = Files.createTempDirectory("codex-supervisor-test");
        Path workspace = Files.createDirectories(tempDir.resolve("workspace"));
        Path argsFile = tempDir.resolve("args.txt");
        Path pwdFile = tempDir.resolve("pwd.txt");
        Path script = createCaptureScript(tempDir.resolve("capture.sh"), argsFile, pwdFile);

        StdioCodexProcessSupervisor supervisor = new StdioCodexProcessSupervisor(List.of("/bin/sh", script.toString()), workspace);
        try {
            supervisor.ensureStarted();

            waitUntilExists(argsFile);
            waitUntilExists(pwdFile);

            assertEquals(workspace.toRealPath().toString(), Path.of(Files.readString(pwdFile).trim()).toRealPath().toString());
            assertEquals(
                    List.of("-s", "workspace-write", "-a", "on-request", "mcp-server"),
                    Files.readAllLines(argsFile));
        } finally {
            if (supervisor.isAlive()) {
                supervisor.process().destroyForcibly();
            }
        }
    }

    @Test
    void surfacesRecentStderrWhenCodexProcessExits() throws Exception {
        Path tempDir = Files.createTempDirectory("codex-supervisor-stderr-test");
        Path workspace = Files.createDirectories(tempDir.resolve("workspace"));
        Path script = createExitWithStderrScript(tempDir.resolve("fail.sh"), "codex startup failed");

        StdioCodexProcessSupervisor supervisor = new StdioCodexProcessSupervisor(List.of("/bin/sh", script.toString()), workspace);

        supervisor.ensureStarted();
        waitUntilNotAlive(supervisor);

        CodexTransportException error = assertThrows(CodexTransportException.class, supervisor::process);
        assertTrue(error.getMessage().contains("codex startup failed"), error.getMessage());
    }

    private static Path createCaptureScript(Path script, Path argsFile, Path pwdFile) throws IOException {
        String content = """
                #!/bin/sh
                pwd > '%s'
                printf '%%s\\n' "$@" > '%s'
                sleep 5
                """.formatted(pwdFile, argsFile);
        Files.writeString(script, content);
        script.toFile().setExecutable(true);
        return script;
    }

    private static Path createExitWithStderrScript(Path script, String stderrLine) throws IOException {
        String content = """
                #!/bin/sh
                echo '%s' >&2
                exit 1
                """.formatted(stderrLine);
        Files.writeString(script, content);
        script.toFile().setExecutable(true);
        return script;
    }

    private static void waitUntilExists(Path path) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (System.nanoTime() < deadline) {
            if (Files.exists(path)) {
                return;
            }
            Thread.sleep(25);
        }
        assertTrue(Files.exists(path), "expected file to be created: " + path);
    }

    private static void waitUntilNotAlive(StdioCodexProcessSupervisor supervisor) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (System.nanoTime() < deadline) {
            if (!supervisor.isAlive()) {
                return;
            }
            Thread.sleep(25);
        }
        assertTrue(!supervisor.isAlive(), "expected process to exit");
    }
}
