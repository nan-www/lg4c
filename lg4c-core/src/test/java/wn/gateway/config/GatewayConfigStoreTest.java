package wn.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GatewayConfigStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void roundTripsConfigWithoutLarkEndpointOverrides() throws IOException {
        Path homeDir = tempDir.resolve("home");
        Files.createDirectories(homeDir);

        GatewayAppConfig config = GatewayAppConfig.builder()
                .codexCommand(List.of("codex"))
                .workspaceRoot(tempDir.resolve("workspace"))
                .recordRoot(homeDir.resolve(".lg4c/records"))
                .agentTemplate("agent")
                .feishuAppId("app-id")
                .feishuAppSecret("app-secret")
                .allowedUsers(List.of("ou_1"))
                .allowedChats(List.of("oc_1"))
                .loggingLevel("INFO")
                .build();

        GatewayConfigStore store = new GatewayConfigStore();
        store.save(homeDir, config);

        GatewayAppConfig reloaded = store.load(homeDir);

        assertEquals("https://open.feishu.cn", reloaded.feishuBaseUrl());
        assertNull(reloaded.feishuWebsocketUrl());
        assertNull(reloaded.feishuReplyUrl());
        assertEquals(config.allowedUsers(), reloaded.allowedUsers());
        assertEquals(config.allowedChats(), reloaded.allowedChats());
    }

    @Test
    void loadsLegacyEndpointOverrides() throws IOException {
        Path homeDir = tempDir.resolve("legacy-home");
        Path configDir = homeDir.resolve(".lg4c/config");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("application.yml"), """
                gateway:
                  codex:
                    command:
                      - codex
                  workspace:
                    root: %s
                  record:
                    root: %s
                  agent:
                    template: legacy agent
                  feishu:
                    appId: app-id
                    appSecret: app-secret
                    websocketUrl: wss://open.feishu.test/ws
                    replyUrl: https://open.feishu.test/reply
                  access:
                    allowedUsers:
                      - ou_1
                    allowedChats:
                      - oc_1
                  logging:
                    level: INFO
                """.formatted(
                tempDir.resolve("workspace"),
                homeDir.resolve(".lg4c/records")));

        GatewayAppConfig reloaded = new GatewayConfigStore().load(homeDir);

        assertEquals("wss://open.feishu.test/ws", reloaded.feishuWebsocketUrl());
        assertEquals("https://open.feishu.test/reply", reloaded.feishuReplyUrl());
        assertEquals("https://open.feishu.cn", reloaded.feishuBaseUrl());
    }
}
