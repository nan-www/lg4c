package wn.gateway.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import wn.gateway.config.GatewayAppConfig;
import wn.gateway.config.GatewayConfigStore;

class BootstrapServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void bootstrapPersistsConfigAndCreatesAgentFileWhenMissing() throws IOException {
        Path homeDir = tempDir.resolve("home");
        Path workspaceDir = tempDir.resolve("workspace");
        Files.createDirectories(homeDir);
        Files.createDirectories(workspaceDir);

        GatewayAppConfig config = GatewayAppConfig.builder()
                .codexCommand(List.of("/opt/homebrew/bin/codex"))
                .workspaceRoot(workspaceDir)
                .recordRoot(homeDir.resolve(".lg4c/records"))
                .agentTemplate("default agent instructions")
                .feishuAppId("app-id")
                .feishuAppSecret("app-secret")
                .allowedUsers(List.of("ou_1"))
                .allowedChats(List.of("oc_1"))
                .loggingLevel("INFO")
                .build();

        GatewayConfigStore store = new GatewayConfigStore();
        BootstrapService service = new BootstrapService(store);

        service.initializeWorkspace(config);
        service.save(homeDir, config);

        Path agentFile = workspaceDir.resolve("AGENT.md");
        assertTrue(Files.exists(agentFile));
        assertEquals("default agent instructions", Files.readString(agentFile));

        Path configFile = homeDir.resolve(".lg4c/config/application.yml");
        assertTrue(Files.exists(configFile));

        GatewayAppConfig reloaded = store.load(homeDir);
        assertEquals(config.workspaceRoot(), reloaded.workspaceRoot());
        assertEquals(config.allowedUsers(), reloaded.allowedUsers());
        assertEquals(config.allowedChats(), reloaded.allowedChats());
    }

    @Test
    void bootstrapDoesNotOverwriteExistingAgentFile() throws IOException {
        Path homeDir = tempDir.resolve("home-existing");
        Path workspaceDir = tempDir.resolve("workspace-existing");
        Files.createDirectories(homeDir);
        Files.createDirectories(workspaceDir);
        Path agentFile = workspaceDir.resolve("AGENT.md");
        Files.writeString(agentFile, "user customized");

        GatewayAppConfig config = GatewayAppConfig.builder()
                .codexCommand(List.of("codex"))
                .workspaceRoot(workspaceDir)
                .recordRoot(homeDir.resolve(".lg4c/records"))
                .agentTemplate("default agent instructions")
                .feishuAppId("app-id")
                .feishuAppSecret("app-secret")
                .allowedUsers(List.of("ou_1"))
                .allowedChats(List.of("oc_1"))
                .loggingLevel("INFO")
                .build();

        BootstrapService service = new BootstrapService(new GatewayConfigStore());
        service.initializeWorkspace(config);

        assertEquals("user customized", Files.readString(agentFile));
        assertFalse(Files.readString(agentFile).contains("default"));
    }

    @Test
    void feishuBootstrapSummaryDescribesAutomaticDiscovery() {
        BootstrapService service = new BootstrapService(new GatewayConfigStore());

        assertTrue(service.feishuBootstrapSummary().contains("APP_ID"));
        assertTrue(service.feishuBootstrapSummary().contains("APP_SECRET"));
        assertTrue(service.feishuBootstrapSummary().contains("automatic"));
    }

    @Test
    void warningsFlagManualEndpointOverridesAsDeprecated() {
        GatewayAppConfig config = GatewayAppConfig.builder()
                .codexCommand(List.of("codex"))
                .workspaceRoot(tempDir.resolve("workspace-warn"))
                .recordRoot(tempDir.resolve("home-warn/.lg4c/records"))
                .agentTemplate("default agent instructions")
                .feishuAppId("app-id")
                .feishuAppSecret("app-secret")
                .feishuWebsocketUrl("wss://open.feishu.test/ws")
                .feishuReplyUrl("https://open.feishu.test/reply")
                .allowedUsers(List.of("ou_1"))
                .allowedChats(List.of("oc_1"))
                .loggingLevel("INFO")
                .build();

        BootstrapService service = new BootstrapService(new GatewayConfigStore());

        assertEquals(1, service.warningsFor(config).size());
        assertTrue(service.warningsFor(config).getFirst().contains("deprecated"));
    }
}
