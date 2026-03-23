package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.lark.auth.LarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.LarkWsBootstrapResult;

class QuarkusLarkGatewayClientTest extends LarkTestSupport {

    @Test
    void startUsesDiscoveredWebsocketUrl() {
        AtomicReference<String> connectedUrl = new AtomicReference<>();
        LarkEndpointDiscoveryService discoveryService = config -> new LarkWsBootstrapResult(
                "wss://open.feishu.cn/ws/discovered",
                LarkClientRuntimeConfig.DEFAULT);
        LarkAccessTokenProvider tokenProvider = config -> "tenant-token";
        LarkReplyApi replyApi = (authorization, messageId, request) -> CompletableFuture.completedFuture(null);
        LarkWebSocketConnector connector = (websocketUrl, endpoint, endpointConfig) -> {
            connectedUrl.set(websocketUrl);
            return null;
        };
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                connector,
                discoveryService,
                tokenProvider);

        client.start(message -> {
        });

        assertEquals("wss://open.feishu.cn/ws/discovered", connectedUrl.get());
    }

    @Test
    void sendReplyUsesBearerTokenAndUnderlyingFuture() {
        CompletableFuture<Void> replyFuture = new CompletableFuture<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> messageId = new AtomicReference<>();
        AtomicReference<LarkReplyRequest> request = new AtomicReference<>();
        LarkReplyApi replyApi = (auth, msgId, payload) -> {
            authorization.set(auth);
            messageId.set(msgId);
            request.set(payload);
            return replyFuture;
        };
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                (websocketUrl, endpoint, endpointConfig) -> null,
                config -> new LarkWsBootstrapResult("wss://ignored", LarkClientRuntimeConfig.DEFAULT),
                config -> "tenant-token");

        CompletableFuture<Void> returned = client.sendReply(message(), "ok");

        assertSame(replyFuture, returned);
        assertEquals("Bearer tenant-token", authorization.get());
        assertEquals("om_1", messageId.get());
        assertEquals("text", request.get().msgType());
        assertEquals("{\"text\":\"ok\"}", request.get().content());
    }
}
