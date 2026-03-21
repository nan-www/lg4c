package wn.gateway.feishu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.InboundMessage;

class QuarkusFeishuGatewayClientVirtualThreadTest {

    @Test
    void sendReplyRunsOnVirtualThread() throws Exception {
        AtomicBoolean ranOnVirtualThread = new AtomicBoolean();
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("feishu-reply-test-", 0).factory());
        FeishuReplyApi replyApi = (appId, appSecret, request) -> {
            ranOnVirtualThread.set(Thread.currentThread().isVirtual());
            return CompletableFuture.completedFuture(null);
        };
        FeishuWebSocketConnector connector = (config, endpoint, endpointConfig) -> null;
        QuarkusFeishuGatewayClient client = new QuarkusFeishuGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                connector,
                executor);

        client.sendReply(message(), "ok").get(2, TimeUnit.SECONDS);

        assertTrue(ranOnVirtualThread.get());
        client.close();
    }

    @Test
    void websocketConnectRunsOnVirtualThread() throws Exception {
        AtomicBoolean ranOnVirtualThread = new AtomicBoolean();
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("feishu-connect-test-", 0).factory());
        FeishuReplyApi replyApi = (appId, appSecret, request) -> CompletableFuture.completedFuture(null);
        FeishuWebSocketConnector connector = (config, endpoint, endpointConfig) -> {
            ranOnVirtualThread.set(Thread.currentThread().isVirtual());
            return null;
        };
        QuarkusFeishuGatewayClient client = new QuarkusFeishuGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                connector,
                executor);

        client.start(message -> {
        });

        assertTrue(ranOnVirtualThread.get());
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
