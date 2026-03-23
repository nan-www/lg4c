package wn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;
import wn.cli.GatewayDaemonCommand;
import wn.cli.GatewayDoctorCommand;
import wn.cli.GatewayStartupFlow;
import wn.gateway.bootstrap.BootstrapService;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.config.GatewayConfigStore;
import wn.gateway.runtime.GatewayDaemonService;

class GatewayCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void mainRunsDefaultStartupFlowWhenNoArgs() throws Exception {
        Path homeDir = tempDir.resolve("main-home");
        Path workspaceDir = tempDir.resolve("main-workspace");
        Files.createDirectories(homeDir);
        Files.createDirectories(workspaceDir);

        GatewayConfigStore store = new GatewayConfigStore();
        store.save(homeDir, configFor(workspaceDir));
        RecordingGatewayDaemonService daemonService = new RecordingGatewayDaemonService();

        GatewayMain main = new GatewayMain();
        main.rootCommand = rootCommand(daemonService);
        main.factory = CommandLine.defaultFactory();

        ExecutionResult result = executeMain(main, null, "--home", homeDir.toString());

        assertEquals(0, result.exitCode(), result.stdout() + result.stderr());
        assertTrue(result.stdout().contains("Almost friday sir."));
        assertFalse(result.stdout().contains("Usage"));
        assertEquals(1, daemonService.invocations);
    }

    @Test
    void rootCommandBootstrapsInteractivelyWhenConfigMissing() throws Exception {
        Path homeDir = tempDir.resolve("interactive-home");
        Path workspaceDir = tempDir.resolve("interactive-workspace");
        RecordingGatewayDaemonService daemonService = new RecordingGatewayDaemonService();
        String input = String.join(System.lineSeparator(), "app-id", "app-secret", workspaceDir.toString()) + System.lineSeparator();

        ExecutionResult result = executeWithInput(
                new CommandLine(rootCommand(daemonService)),
                input,
                "--home", homeDir.toString());

        assertEquals(0, result.exitCode(), result.stdout() + result.stderr());
        assertTrue(Files.exists(homeDir.resolve(".lg4c/config/application.yml")));
        assertTrue(Files.exists(workspaceDir.resolve("AGENT.md")));
        assertTrue(result.stdout().contains("Almost friday sir."));
        assertEquals(1, daemonService.invocations);

        GatewayAppConfig savedConfig = new GatewayConfigStore().load(homeDir);
        assertEquals("app-id", savedConfig.feishuAppId());
        assertEquals("app-secret", savedConfig.feishuAppSecret());
        assertEquals(workspaceDir, savedConfig.workspaceRoot());
        assertTrue(savedConfig.allowedUsers().isEmpty());
        assertTrue(savedConfig.allowedChats().isEmpty());
        assertNotNull(daemonService.lastConfig);
    }

    @Test
    void doctorReportsAutomaticBootstrapChain() throws Exception {
        Path homeDir = tempDir.resolve("doctor-home");
        Path workspaceDir = tempDir.resolve("doctor-workspace");
        Files.createDirectories(homeDir);
        Files.createDirectories(workspaceDir);

        new GatewayConfigStore().save(homeDir, configFor(workspaceDir));

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

        new GatewayConfigStore()
                .save(homeDir, configFor(workspaceDir).toBuilder()
                        .feishuWebsocketUrl("wss://open.feishu.test/ws")
                        .feishuReplyUrl("https://open.feishu.test/reply")
                        .build());

        ExecutionResult doctor = execute(
                new CommandLine(doctorCommand()),
                "--home", homeDir.toString());

        assertEquals(0, doctor.exitCode(), doctor.stdout() + doctor.stderr());
        assertTrue(doctor.stdout().contains("deprecated"));
    }

    @Test
    void daemonUsesParametersBeforePromptingForMissingFields() throws Exception {
        Path homeDir = tempDir.resolve("mixed-home");
        Path workspaceDir = tempDir.resolve("mixed-workspace");
        RecordingGatewayDaemonService daemonService = new RecordingGatewayDaemonService();
        String input = String.join(System.lineSeparator(), "prompt-secret", workspaceDir.toString()) + System.lineSeparator();

        ExecutionResult result = executeWithInput(
                new CommandLine(daemonCommand(daemonService)),
                input,
                "--home", homeDir.toString(),
                "--feishu-app-id", "flag-app-id");

        assertEquals(0, result.exitCode(), result.stdout() + result.stderr());
        assertTrue(result.stdout().contains("Almost friday sir."));
        assertEquals(1, daemonService.invocations);

        GatewayAppConfig savedConfig = new GatewayConfigStore().load(homeDir);
        assertEquals("flag-app-id", savedConfig.feishuAppId());
        assertEquals("prompt-secret", savedConfig.feishuAppSecret());
        assertEquals(workspaceDir, savedConfig.workspaceRoot());
    }

    @Test
    void daemonFailsFastWhenInteractiveInputEndsEarly() throws Exception {
        Path homeDir = tempDir.resolve("incomplete-home");

        ExecutionResult result = assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> executeWithInput(
                        new CommandLine(daemonCommand(new RecordingGatewayDaemonService())),
                        "app-id" + System.lineSeparator(),
                        "--home", homeDir.toString()));

        assertEquals(2, result.exitCode());
        assertFalse(Files.exists(homeDir.resolve(".lg4c/config/application.yml")));
        assertTrue(result.stderr().contains("APPSecret"));
    }

    private GatewayRootCommand rootCommand(RecordingGatewayDaemonService daemonService) throws Exception {
        GatewayRootCommand command = new GatewayRootCommand();
        inject(command, "startupFlow", startupFlow(daemonService));
        return command;
    }

    private GatewayDaemonCommand daemonCommand(RecordingGatewayDaemonService daemonService) throws Exception {
        GatewayDaemonCommand command = new GatewayDaemonCommand();
        inject(command, "startupFlow", startupFlow(daemonService));
        return command;
    }

    private GatewayStartupFlow startupFlow(RecordingGatewayDaemonService daemonService) {
        GatewayConfigStore store = new GatewayConfigStore();
        BootstrapService bootstrapService = new BootstrapService(store);
        return new GatewayStartupFlow(store, bootstrapService, daemonService);
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
        return executeInternal(commandLine, null, args);
    }

    private ExecutionResult executeWithInput(CommandLine commandLine, String input, String... args) {
        return executeInternal(commandLine, input, args);
    }

    private ExecutionResult executeInternal(CommandLine commandLine, String input, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            if (input != null) {
                System.setIn(new ByteArrayInputStream(input.getBytes()));
            }
            System.setOut(new PrintStream(out, true));
            System.setErr(new PrintStream(err, true));
            commandLine.setOut(new PrintWriter(out, true));
            commandLine.setErr(new PrintWriter(err, true));
            int exitCode = commandLine.execute(args);
            return new ExecutionResult(exitCode, out.toString(), err.toString());
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private ExecutionResult executeMain(GatewayMain main, String input, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            if (input != null) {
                System.setIn(new ByteArrayInputStream(input.getBytes()));
            }
            System.setOut(new PrintStream(out, true));
            System.setErr(new PrintStream(err, true));
            int exitCode = main.run(args);
            return new ExecutionResult(exitCode, out.toString(), err.toString());
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private GatewayAppConfig configFor(Path workspaceDir) {
        return GatewayAppConfig.builder()
                .codexCommand(List.of("codex"))
                .workspaceRoot(workspaceDir)
                .recordRoot(tempDir.resolve("records"))
                .agentTemplate("""
                        # LG4C Agent

                        Test agent template.
                        """)
                .feishuAppId("app-id")
                .feishuAppSecret("app-secret")
                .allowedUsers(List.of())
                .allowedChats(List.of())
                .loggingLevel("INFO")
                .build();
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record ExecutionResult(int exitCode, String stdout, String stderr) {
    }

    private static final class RecordingGatewayDaemonService extends GatewayDaemonService {
        int invocations;
        GatewayAppConfig lastConfig;

        @Override
        public void run(GatewayAppConfig config) {
            invocations++;
            lastConfig = config;
        }
    }
}
