package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.config.GatewayAppConfig;
import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.dto.LarkReplyRequest;

class QuarkusLarkGatewayClientTest extends LarkTestSupport {

    @Test
    void startBuildsSdkConnectionAndBridgesRawEventPayload() throws Exception {
        AtomicBoolean started = new AtomicBoolean();
        AtomicReference<GatewayAppConfig> capturedConfig = new AtomicReference<>();
        AtomicReference<Consumer<byte[]>> rawEventConsumer = new AtomicReference<>();
        AtomicReference<wn.gateway.domain.InboundMessage> received = new AtomicReference<>();

        LarkSdkLongConnection connection = new LarkSdkLongConnection() {
            @Override
            public void start() {
                started.set(true);
            }

            @Override
            public boolean isConnected() {
                return started.get();
            }

            @Override
            public void close() {
            }
        };
        OfficialLarkSdkLongConnectionFactory sdkFactory = mock(OfficialLarkSdkLongConnectionFactory.class);
        when(sdkFactory.create(any(), any())).thenAnswer(invocation -> {
            capturedConfig.set(invocation.getArgument(0, GatewayAppConfig.class));
            rawEventConsumer.set(invocation.getArgument(1));
            return connection;
        });
        CachedLarkAccessTokenProvider tokenProvider = mock(CachedLarkAccessTokenProvider.class);
        LarkReplyApi replyApi = (authorization, messageId, request) -> CompletableFuture.completedFuture(null);
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                sdkFactory,
                tokenProvider);

        client.start(received::set);

        assertTrue(started.get());
        assertEquals(config(), capturedConfig.get());
        assertNotNull(rawEventConsumer.get());

        rawEventConsumer.get().accept(sampleEventPayload(new ObjectMapper()));

        assertNotNull(received.get());
        assertEquals("ou_event", received.get().userId());
        assertEquals("oc_event", received.get().chatId());
        assertEquals("om_event", received.get().messageId());
        assertEquals("hello from lark", received.get().text());
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
                mock(OfficialLarkSdkLongConnectionFactory.class),
                tokenProvider);

        CompletableFuture<Void> returned = client.sendReply(message(), "ok");

        assertSame(replyFuture, returned);
        assertEquals("Bearer tenant-token", authorization.get());
        assertEquals("om_1", messageId.get());
        assertEquals("text", request.get().msgType());
        assertEquals("{\"text\":\"ok\"}", request.get().content());
    }

    @Test
    void closeDelegatesToSdkConnection() throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        OfficialLarkSdkLongConnectionFactory sdkFactory = mock(OfficialLarkSdkLongConnectionFactory.class);
        when(sdkFactory.create(any(), any())).thenReturn(new LarkSdkLongConnection() {
            @Override
            public void start() {
            }

            @Override
            public boolean isConnected() {
                return true;
            }

            @Override
            public void close() {
                closed.set(true);
            }
        });
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                (authorization, messageId, request) -> CompletableFuture.completedFuture(null),
                sdkFactory,
                mock(CachedLarkAccessTokenProvider.class));

        client.start(message -> {
        });
        client.close();

        assertTrue(closed.get());
    }

    private byte[] sampleEventPayload(ObjectMapper mapper) throws Exception {
        var root = mapper.createObjectNode();
        root.putObject("header").put("create_time", 1710000000000L);
        var event = root.putObject("event");
        event.putObject("sender")
                .putObject("sender_id")
                .put("open_id", "ou_event");
        event.putObject("message")
                .put("chat_id", "oc_event")
                .put("message_id", "om_event")
                .put("content", "{\"text\":\"hello from lark\"}");
        return mapper.writeValueAsString(root).getBytes(StandardCharsets.UTF_8);
    }
}
