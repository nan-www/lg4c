package wn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
class GatewayCommandTest {

    @TempDir
    static Path tempDir;

    @Test
    void daemonRefusesToStartWithoutBootstrap(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("daemon", "--home", tempDir.resolve("missing-home").toString());

        assertEquals(2, result.exitCode());
        assertTrue(result.getErrorOutput().contains("--bootstrap"));
    }

    @Test
    void rootCommandPrintsUsage(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch();
        assertTrue(result.getOutput().contains("Usage"));
        assertEquals(0, result.exitCode());
    }

    @Test
    void daemonBootstrapCreatesLocalConfig(QuarkusMainLauncher launcher) throws Exception {
        Path homeDir = tempDir.resolve("bootstrap-home");
        Path workspaceDir = tempDir.resolve("bootstrap-workspace");
        Files.createDirectories(homeDir);
        Files.createDirectories(workspaceDir);

        LaunchResult result = launcher.launch(
                "daemon",
                "--bootstrap",
                "--home", homeDir.toString(),
                "--workspace", workspaceDir.toString(),
                "--codex-command", "codex",
                "--feishu-app-id", "app-id",
                "--feishu-app-secret", "app-secret",
                "--feishu-websocket-url", "wss://open.feishu.test/ws",
                "--feishu-reply-url", "https://open.feishu.test/reply",
                "--allowed-user", "ou_1",
                "--allowed-chat", "oc_1");

        assertEquals(0, result.exitCode(), result.getOutput() + result.getErrorOutput());
        assertTrue(Files.exists(homeDir.resolve(".lg4c/config/application.yml")));
        assertTrue(Files.exists(workspaceDir.resolve("AGENT.md")));
    }
}
