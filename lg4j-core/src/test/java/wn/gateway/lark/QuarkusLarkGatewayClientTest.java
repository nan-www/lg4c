package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;
import wn.gateway.lark.bootstrap.dto.LarkWsBootstrapResult;
import wn.gateway.lark.dto.LarkReplyRequest;

class QuarkusLarkGatewayClientTest extends LarkTestSupport {

    @Test
    void startUsesDiscoveredWebsocketUrl() throws Exception {
        AtomicReference<String> connectedUrl = new AtomicReference<>();
        LarkEndpointDiscoveryService discoveryService = mock(LarkEndpointDiscoveryService.class);
        when(discoveryService.resolve(any())).thenReturn(new LarkWsBootstrapResult(
                "wss://open.feishu.cn/ws/discovered",
                LarkClientRuntimeConfig.DEFAULT));
        CachedLarkAccessTokenProvider tokenProvider = mock(CachedLarkAccessTokenProvider.class);
        LarkReplyApi replyApi = (authorization, messageId, request) -> CompletableFuture.completedFuture(null);
        LarkWebSocketConnector connector = mock(LarkWebSocketConnector.class);
        doAnswer(invocation -> {
            connectedUrl.set(invocation.getArgument(0, String.class));
            return null;
        }).when(connector).connect(anyString(), any(), any());
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
        CachedLarkAccessTokenProvider tokenProvider = mock(CachedLarkAccessTokenProvider.class);
        when(tokenProvider.getTenantAccessToken(any())).thenReturn("tenant-token");
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                mock(LarkWebSocketConnector.class),
                mock(LarkEndpointDiscoveryService.class),
                tokenProvider);

        CompletableFuture<Void> returned = client.sendReply(message(), "ok");

        assertSame(replyFuture, returned);
        assertEquals("Bearer tenant-token", authorization.get());
        assertEquals("om_1", messageId.get());
        assertEquals("text", request.get().msgType());
        assertEquals("{\"text\":\"ok\"}", request.get().content());
    }
}
