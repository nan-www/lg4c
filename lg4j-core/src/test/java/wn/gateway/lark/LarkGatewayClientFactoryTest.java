package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;

@QuarkusTest
class LarkGatewayClientFactoryTest {

    @Inject
    LarkGatewayClientFactory factory;

    @Test
    void factoryCreatesQuarkusWebSocketClient() {
        GatewayAppConfig config = GatewayAppConfig.builder()
                .codexCommand(List.of("codex"))
                .workspaceRoot(Path.of("/tmp/workspace"))
                .recordRoot(Path.of("/tmp/.lg4c/records"))
                .agentTemplate("agent")
                .feishuAppId("app-id")
                .feishuAppSecret("app-secret")
                .feishuWebsocketUrl("wss://open.feishu.test/ws")
                .feishuReplyUrl("https://open.feishu.test/reply")
                .allowedUsers(List.of("ou_1"))
                .allowedChats(List.of("oc_1"))
                .loggingLevel("INFO")
                .build();

        LarkGatewayClient client = factory.create(config);

        assertNotNull(client);
        assertInstanceOf(QuarkusLarkGatewayClient.class, client);
    }
}
