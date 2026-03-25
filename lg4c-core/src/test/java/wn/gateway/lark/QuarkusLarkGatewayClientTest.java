package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.ws.Client;

import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.dto.LarkReplyRequest;

class QuarkusLarkGatewayClientTest extends LarkTestSupport {

    @Test
    void startBuildsOfficialSdkClientAndBridgesTypedEventPayload() throws Exception {
        AtomicReference<wn.gateway.domain.InboundMessage> received = new AtomicReference<>();
        AtomicReference<EventDispatcher> dispatcher = new AtomicReference<>();
        Client sdkClient = mock(Client.class);
        CachedLarkAccessTokenProvider tokenProvider = mock(CachedLarkAccessTokenProvider.class);
        LarkReplyApi replyApi = (authorization, messageId, request) -> CompletableFuture.completedFuture(null);
        LarkReplyApiFactory replyApiFactory = mock(LarkReplyApiFactory.class);
        when(replyApiFactory.create(any())).thenReturn(replyApi);

        try (MockedConstruction<Client.Builder> builderConstruction = mockConstruction(
                Client.Builder.class,
                (builder, context) -> {
                    assertEquals("app-id", context.arguments().get(0));
                    assertEquals("app-secret", context.arguments().get(1));
                    when(builder.eventHandler(any(EventDispatcher.class))).thenAnswer(invocation -> {
                        dispatcher.set(invocation.getArgument(0, EventDispatcher.class));
                        return builder;
                    });
                    when(builder.domain(eq(config().larkEnvironment().baseUrl()))).thenReturn(builder);
                    when(builder.build()).thenReturn(sdkClient);
                })) {
            QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                    new ObjectMapper(),
                    replyApiFactory,
                    tokenProvider);
            client.setConfig(config());

            client.start(received::set);

            assertEquals(1, builderConstruction.constructed().size());
            assertNotNull(dispatcher.get());
            verify(sdkClient).start();

            try {
                dispatcher.get().doWithoutValidation(sampleEventPayload(new ObjectMapper()));
            } catch (Throwable throwable) {
                throw new AssertionError("dispatcher should accept official sdk payloads", throwable);
            }

            assertNotNull(received.get());
            assertEquals("ou_event", received.get().userId());
            assertEquals("oc_event", received.get().chatId());
            assertEquals("om_event", received.get().messageId());
            assertEquals("hello from lark", received.get().text());
        }
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
        LarkReplyApiFactory replyApiFactory = mock(LarkReplyApiFactory.class);
        when(replyApiFactory.create(any())).thenReturn(replyApi);
        CachedLarkAccessTokenProvider tokenProvider = mock(CachedLarkAccessTokenProvider.class);
        when(tokenProvider.getTenantAccessToken(any())).thenReturn("tenant-token");
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                new ObjectMapper(),
                replyApiFactory,
                tokenProvider);
        client.setConfig(config());

        CompletableFuture<Void> returned = client.sendReply(message(), "ok");

        assertSame(replyFuture, returned);
        assertEquals("Bearer tenant-token", authorization.get());
        assertEquals("om_1", messageId.get());
        assertEquals("text", request.get().msgType());
        assertEquals("{\"text\":\"ok\"}", request.get().content());
    }

    @Test
    void closeBeforeStartIsSafe() throws Exception {
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                new ObjectMapper(),
                mock(LarkReplyApiFactory.class),
                mock(CachedLarkAccessTokenProvider.class));

        client.close();
    }

    private byte[] sampleEventPayload(ObjectMapper mapper) throws Exception {
        var root = mapper.createObjectNode();
        root.put("schema", "2.0");
        root.putObject("header")
                .put("event_type", "im.message.receive_v1")
                .put("create_time", "1710000000000");
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
