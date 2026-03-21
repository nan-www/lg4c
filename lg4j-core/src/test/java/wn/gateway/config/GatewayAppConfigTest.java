package wn.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GatewayAppConfigTest {

    @Test
    void builderKeepsConfigListsImmutableAndDefaultsLoggingLevel() {
        List<String> codexCommand = new ArrayList<>(List.of("codex"));
        List<String> allowedUsers = new ArrayList<>(List.of("ou_1"));
        List<String> allowedChats = new ArrayList<>(List.of("oc_1"));

        GatewayAppConfig config = GatewayAppConfig.builder()
                .codexCommand(codexCommand)
                .workspaceRoot(Path.of("/tmp/workspace"))
                .recordRoot(Path.of("/tmp/.lg4c/records"))
                .agentTemplate("agent")
                .feishuAppId("app-id")
                .feishuAppSecret("app-secret")
                .feishuWebsocketUrl("wss://open.feishu.test/ws")
                .feishuReplyUrl("https://open.feishu.test/reply")
                .allowedUsers(allowedUsers)
                .allowedChats(allowedChats)
                .loggingLevel("  ")
                .build();

        codexCommand.add("codex-mcp");
        allowedUsers.add("ou_2");
        allowedChats.add("oc_2");

        assertEquals(List.of("codex"), config.codexCommand());
        assertEquals(List.of("ou_1"), config.allowedUsers());
        assertEquals(List.of("oc_1"), config.allowedChats());
        assertEquals("INFO", config.loggingLevel());
        assertThrows(UnsupportedOperationException.class, () -> config.codexCommand().add("other"));
    }
}
