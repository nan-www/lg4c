package wn.gateway.lark;

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
import wn.gateway.lark.bootstrap.DefaultLarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;
import wn.gateway.lark.bootstrap.LarkWsBootstrapResult;

class QuarkusLarkGatewayClientVirtualThreadTest extends LarkTestSupport {

    @Test
    void sendReplyDelegatesOnCallerThreadAndReturnsUnderlyingFuture() {
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> apiThread = new AtomicReference<>();
        CompletableFuture<Void> replyFuture = new CompletableFuture<>();
        LarkReplyApi replyApi = (authorization, messageId, request) -> {
            apiThread.set(Thread.currentThread());
            return replyFuture;
        };
        LarkWebSocketConnector connector = mock(LarkWebSocketConnector.class);
        DefaultLarkEndpointDiscoveryService discoveryService = mock(DefaultLarkEndpointDiscoveryService.class);
        when(discoveryService.resolve(any())).thenReturn(new LarkWsBootstrapResult(
                "wss://open.feishu.test/ws",
                LarkClientRuntimeConfig.DEFAULT));
        CachedLarkAccessTokenProvider tokenProvider = mock(CachedLarkAccessTokenProvider.class);
        when(tokenProvider.getTenantAccessToken(any())).thenReturn("tenant-token");
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                connector,
                discoveryService,
                tokenProvider);

        CompletableFuture<Void> returned = client.sendReply(message(), "ok");

        assertSame(callerThread, apiThread.get());
        assertSame(replyFuture, returned);
    }

    @Test
    void websocketConnectRunsOnCallerThread() throws Exception {
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> connectThread = new AtomicReference<>();
        LarkReplyApi replyApi = (authorization, messageId, request) -> CompletableFuture.completedFuture(null);
        LarkWebSocketConnector connector = mock(LarkWebSocketConnector.class);
        doAnswer(invocation -> {
            connectThread.set(Thread.currentThread());
            return null;
        }).when(connector).connect(anyString(), any(), any());
        DefaultLarkEndpointDiscoveryService discoveryService = mock(DefaultLarkEndpointDiscoveryService.class);
        when(discoveryService.resolve(any())).thenReturn(new LarkWsBootstrapResult(
                "wss://open.feishu.test/ws",
                LarkClientRuntimeConfig.DEFAULT));
        CachedLarkAccessTokenProvider tokenProvider = mock(CachedLarkAccessTokenProvider.class);
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                connector,
                discoveryService,
                tokenProvider);

        client.start(message -> {
        });

        assertSame(callerThread, connectThread.get());
        client.close();
    }
}
