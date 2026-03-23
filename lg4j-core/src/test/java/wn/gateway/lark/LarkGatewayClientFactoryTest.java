package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.config.GatewayAppConfig;
import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.DefaultLarkEndpointDiscoveryService;

class LarkGatewayClientFactoryTest {

    @Test
    void factoryCreatesQuarkusWebSocketClient() {
        LarkGatewayClientFactory factory = new LarkGatewayClientFactory();
        factory.mapper = new ObjectMapper();
        factory.replyApiFactory = mock(LarkReplyApiFactory.class);
        factory.webSocketConnector = mock(LarkWebSocketConnector.class);
        factory.endpointDiscoveryService = mock(DefaultLarkEndpointDiscoveryService.class);
        factory.accessTokenProvider = mock(CachedLarkAccessTokenProvider.class);

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
