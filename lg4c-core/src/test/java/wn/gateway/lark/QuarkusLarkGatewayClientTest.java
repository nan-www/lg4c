package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;
import wn.gateway.lark.bootstrap.dto.LarkWsBootstrapResult;
import wn.gateway.lark.dto.LarkReplyRequest;
import wn.gateway.lark.dto.LarkWsProtoCodec;
import wn.gateway.lark.dto.LarkWsProtoFrame;

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

    @Test
    void binaryEventFramesAreDecodedAndAcknowledged() throws Exception {
        AtomicReference<Endpoint> endpointRef = new AtomicReference<>();
        AtomicReference<MessageHandler.Whole<byte[]>> binaryHandler = new AtomicReference<>();
        AtomicReference<byte[]> ackBytes = new AtomicReference<>();
        AtomicReference<wn.gateway.domain.InboundMessage> received = new AtomicReference<>();

        Session session = mock(Session.class);
        when(session.isOpen()).thenReturn(true);
        RemoteEndpoint.Async asyncRemote = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(asyncRemote);
        doAnswer(invocation -> {
            ByteBuffer buffer = invocation.getArgument(0, ByteBuffer.class).duplicate();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            ackBytes.set(bytes);
            return null;
        }).when(asyncRemote).sendBinary(any(ByteBuffer.class));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            MessageHandler.Whole<byte[]> handler = invocation.getArgument(1, MessageHandler.Whole.class);
            binaryHandler.set(handler);
            return null;
        }).when(session).addMessageHandler(org.mockito.ArgumentMatchers.eq(byte[].class), any(MessageHandler.Whole.class));

        LarkEndpointDiscoveryService discoveryService = mock(LarkEndpointDiscoveryService.class);
        when(discoveryService.resolve(any())).thenReturn(new LarkWsBootstrapResult(
                "wss://open.feishu.cn/ws/discovered",
                LarkClientRuntimeConfig.DEFAULT));
        CachedLarkAccessTokenProvider tokenProvider = mock(CachedLarkAccessTokenProvider.class);
        LarkReplyApi replyApi = (authorization, messageId, request) -> CompletableFuture.completedFuture(null);
        LarkWebSocketConnector connector = mock(LarkWebSocketConnector.class);
        doAnswer(invocation -> {
            endpointRef.set(invocation.getArgument(1, Endpoint.class));
            return session;
        }).when(connector).connect(anyString(), any(), any());
        ObjectMapper mapper = new ObjectMapper();
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                mapper,
                replyApi,
                connector,
                discoveryService,
                tokenProvider);

        client.start(received::set);
        endpointRef.get().onOpen(session, mock(EndpointConfig.class));

        assertNotNull(binaryHandler.get());
        binaryHandler.get().onMessage(eventFrame(sampleEventPayload(mapper)));

        assertNotNull(received.get());
        assertEquals("ou_event", received.get().userId());
        assertEquals("oc_event", received.get().chatId());
        assertEquals("om_event", received.get().messageId());
        assertEquals("hello from lark", received.get().text());

        assertNotNull(ackBytes.get());
        LarkWsProtoFrame ack = new LarkWsProtoCodec().decode(ackBytes.get());
        assertEquals(1, ack.method());
        assertEquals("42", ack.headerValue("message_id"));
        assertEquals(200, mapper.readTree(ack.payload()).path("statusCode").asInt());
        assertNotNull(ack.headerValue("biz_rt"));
    }

    private byte[] eventFrame(byte[] payload) throws Exception {
        return new LarkWsProtoCodec().encode(new LarkWsProtoFrame(
                11L,
                12L,
                1,
                1,
                List.of(
                        new LarkWsProtoFrame.Header("message_id", "42"),
                        new LarkWsProtoFrame.Header("trace_id", "trace-1"),
                        new LarkWsProtoFrame.Header("type", "event"),
                        new LarkWsProtoFrame.Header("sum", "1"),
                        new LarkWsProtoFrame.Header("seq", "0")),
                "json",
                "application/json",
                payload,
                "log-12"));
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
