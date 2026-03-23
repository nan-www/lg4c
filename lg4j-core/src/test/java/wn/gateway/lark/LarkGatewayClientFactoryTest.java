package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.config.GatewayAppConfig;
import wn.gateway.lark.auth.LarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.LarkWsBootstrapResult;

class LarkGatewayClientFactoryTest {

    @Test
    void factoryCreatesQuarkusWebSocketClient() {
        DefaultLarkGatewayClientFactory factory = new DefaultLarkGatewayClientFactory();
        factory.mapper = new ObjectMapper();
        factory.replyApiFactory = config -> (authorization, messageId, request) -> CompletableFuture.completedFuture(null);
        factory.webSocketConnector = (websocketUrl, endpoint, endpointConfig) -> null;
        factory.endpointDiscoveryService = config -> new LarkWsBootstrapResult(
                "wss://open.feishu.test/ws",
                LarkClientRuntimeConfig.DEFAULT);
        factory.accessTokenProvider = config -> "tenant-token";

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
