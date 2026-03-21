package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.InboundMessage;

class QuarkusLarkGatewayClientVirtualThreadTest {

    @Test
    void sendReplyDelegatesOnCallerThreadAndReturnsUnderlyingFuture() {
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> apiThread = new AtomicReference<>();
        CompletableFuture<Void> replyFuture = new CompletableFuture<>();
        LarkReplyApi replyApi = (appId, appSecret, request) -> {
            apiThread.set(Thread.currentThread());
            return replyFuture;
        };
        LarkWebSocketConnector connector = (config, endpoint, endpointConfig) -> null;
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                connector);

        CompletableFuture<Void> returned = client.sendReply(message(), "ok");

        assertSame(callerThread, apiThread.get());
        assertSame(replyFuture, returned);
    }

    @Test
    void websocketConnectRunsOnCallerThread() throws Exception {
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> connectThread = new AtomicReference<>();
        LarkReplyApi replyApi = (appId, appSecret, request) -> CompletableFuture.completedFuture(null);
        LarkWebSocketConnector connector = (config, endpoint, endpointConfig) -> {
            connectThread.set(Thread.currentThread());
            return null;
        };
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                connector);

        client.start(message -> {
        });

        assertSame(callerThread, connectThread.get());
        client.close();
    }

    private static GatewayAppConfig config() {
        return GatewayAppConfig.builder()
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
    }

    private static InboundMessage message() {
        return new InboundMessage("ou_1", "oc_1", "om_1", "hello", java.time.Instant.now());
    }
}
