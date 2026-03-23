package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.lark.auth.LarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryService;
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
        LarkWebSocketConnector connector = (websocketUrl, endpoint, endpointConfig) -> null;
        LarkEndpointDiscoveryService discoveryService = config -> new LarkWsBootstrapResult(
                "wss://open.feishu.test/ws",
                LarkClientRuntimeConfig.DEFAULT);
        LarkAccessTokenProvider tokenProvider = config -> "tenant-token";
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
        LarkWebSocketConnector connector = (websocketUrl, endpoint, endpointConfig) -> {
            connectThread.set(Thread.currentThread());
            return null;
        };
        LarkEndpointDiscoveryService discoveryService = config -> new LarkWsBootstrapResult(
                "wss://open.feishu.test/ws",
                LarkClientRuntimeConfig.DEFAULT);
        LarkAccessTokenProvider tokenProvider = config -> "tenant-token";
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
