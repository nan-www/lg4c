package wn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;
import wn.cli.GatewayDaemonCommand;
import wn.cli.GatewayDoctorCommand;
import wn.gateway.bootstrap.BootstrapService;
import wn.gateway.config.GatewayConfigStore;
import wn.gateway.runtime.GatewayDaemonService;

class GatewayCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void daemonRefusesToStartWithoutBootstrap() throws Exception {
        GatewayDaemonCommand command = daemonCommand();

        ExecutionResult result = execute(
                new CommandLine(command),
                "--home", tempDir.resolve("missing-home").toString());

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("--bootstrap"));
    }

    @Test
    void rootCommandPrintsUsage() {
        GatewayMain main = new GatewayMain();
        main.rootCommand = new GatewayRootCommand();
        main.factory = new CommandLine.IFactory() {
            @Override
            public <K> K create(Class<K> cls) throws Exception {
                return cls.getDeclaredConstructor().newInstance();
            }
        };
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(output, true));
            int exitCode = main.run();
            assertEquals(0, exitCode);
            assertTrue(output.toString().contains("Usage"));
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void daemonBootstrapCreatesLocalConfig() throws Exception {
        Path homeDir = tempDir.resolve("bootstrap-home");
        Path workspaceDir = tempDir.resolve("bootstrap-workspace");
        Files.createDirectories(homeDir);
        Files.createDirectories(workspaceDir);

        ExecutionResult result = execute(
                new CommandLine(daemonCommand()),
                "--bootstrap",
                "--home", homeDir.toString(),
                "--workspace", workspaceDir.toString(),
                "--codex-command", "codex",
                "--feishu-app-id", "app-id",
                "--feishu-app-secret", "app-secret",
                "--allowed-user", "ou_1",
                "--allowed-chat", "oc_1");

        assertEquals(0, result.exitCode(), result.stdout() + result.stderr());
        assertTrue(Files.exists(homeDir.resolve(".lg4c/config/application.yml")));
        assertTrue(Files.exists(workspaceDir.resolve("AGENT.md")));
        assertTrue(result.stdout().contains("APP_ID and APP_SECRET"));
        assertTrue(result.stdout().contains("automatic"));
    }

    @Test
    void doctorReportsAutomaticBootstrapChain() throws Exception {
        Path homeDir = tempDir.resolve("doctor-home");
        Path workspaceDir = tempDir.resolve("doctor-workspace");
        Files.createDirectories(homeDir);
        Files.createDirectories(workspaceDir);

        ExecutionResult bootstrap = execute(
                new CommandLine(daemonCommand()),
                "--bootstrap",
                "--home", homeDir.toString(),
                "--workspace", workspaceDir.toString(),
                "--codex-command", "codex",
                "--feishu-app-id", "app-id",
                "--feishu-app-secret", "app-secret",
                "--allowed-user", "ou_1",
                "--allowed-chat", "oc_1");
        assertEquals(0, bootstrap.exitCode(), bootstrap.stdout() + bootstrap.stderr());

        ExecutionResult doctor = execute(
                new CommandLine(doctorCommand()),
                "--home", homeDir.toString());

        assertEquals(0, doctor.exitCode(), doctor.stdout() + doctor.stderr());
        assertTrue(doctor.stdout().contains("feishu: configured via app credentials"));
        assertTrue(doctor.stdout().contains("endpoint discovery and tenant token management are automatic"));
    }

    @Test
    void doctorWarnsWhenManualFeishuOverridesAreConfigured() throws Exception {
        Path homeDir = tempDir.resolve("legacy-home");
        Path workspaceDir = tempDir.resolve("legacy-workspace");
        Files.createDirectories(homeDir);
        Files.createDirectories(workspaceDir);

        ExecutionResult bootstrap = execute(
                new CommandLine(daemonCommand()),
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
        assertEquals(0, bootstrap.exitCode(), bootstrap.stdout() + bootstrap.stderr());
        assertTrue(bootstrap.stdout().contains("deprecated"));

        ExecutionResult doctor = execute(
                new CommandLine(doctorCommand()),
                "--home", homeDir.toString());

        assertEquals(0, doctor.exitCode(), doctor.stdout() + doctor.stderr());
        assertTrue(doctor.stdout().contains("deprecated"));
    }

    private GatewayDaemonCommand daemonCommand() throws Exception {
        GatewayConfigStore store = new GatewayConfigStore();
        BootstrapService bootstrapService = new BootstrapService(store);
        GatewayDaemonCommand command = new GatewayDaemonCommand();
        inject(command, "store", store);
        inject(command, "bootstrapService", bootstrapService);
        inject(command, "daemonService", new NoopGatewayDaemonService());
        return command;
    }

    private GatewayDoctorCommand doctorCommand() throws Exception {
        GatewayConfigStore store = new GatewayConfigStore();
        BootstrapService bootstrapService = new BootstrapService(store);
        GatewayDoctorCommand command = new GatewayDoctorCommand();
        inject(command, "store", store);
        inject(command, "bootstrapService", bootstrapService);
        return command;
    }

    private ExecutionResult execute(CommandLine commandLine, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            System.setOut(new PrintStream(out, true));
            System.setErr(new PrintStream(err, true));
            commandLine.setOut(new PrintWriter(out, true));
            commandLine.setErr(new PrintWriter(err, true));
            int exitCode = commandLine.execute(args);
            return new ExecutionResult(exitCode, out.toString(), err.toString());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record ExecutionResult(int exitCode, String stdout, String stderr) {
    }

    private static final class NoopGatewayDaemonService extends GatewayDaemonService {
        @Override
        public void run(wn.gateway.config.GatewayAppConfig config) {
            // no-op for CLI contract tests
        }
    }
}
