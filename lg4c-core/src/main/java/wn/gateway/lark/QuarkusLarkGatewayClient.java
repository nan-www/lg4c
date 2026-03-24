package wn.gateway.lark;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.InboundMessage;
import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.dto.LarkWsBootstrapResult;
import wn.gateway.lark.dto.LarkReplyRequest;
import wn.gateway.lark.dto.LarkWsAckResponse;
import wn.gateway.lark.dto.LarkWsProtoCodec;
import wn.gateway.lark.dto.LarkWsProtoFrame;

@RequiredArgsConstructor
@Slf4j
public class QuarkusLarkGatewayClient implements LarkGatewayClient {
    private static final int CONTROL_FRAME_METHOD = 0;
    private static final int DATA_FRAME_METHOD = 1;
    private static final String HEADER_MESSAGE_ID = "message_id";
    private static final String HEADER_TYPE = "type";
    private static final String HEADER_SUM = "sum";
    private static final String HEADER_SEQ = "seq";
    private static final String HEADER_BIZ_RT = "biz_rt";
    private static final String MESSAGE_TYPE_EVENT = "event";
    private static final String MESSAGE_TYPE_PING = "ping";
    private static final String MESSAGE_TYPE_PONG = "pong";

    private final GatewayAppConfig config;
    private final ObjectMapper mapper;
    private final LarkReplyApi replyApi;
    private final LarkWebSocketConnector webSocketConnector;
    private final LarkEndpointDiscoveryService endpointDiscoveryService;
    private final CachedLarkAccessTokenProvider accessTokenProvider;
    private final LarkWsProtoCodec frameCodec = new LarkWsProtoCodec();
    private final ConcurrentMap<String, byte[][]> pendingPayloads = new ConcurrentHashMap<>();
    private volatile Session session;

    @Override
    public void start(Consumer<InboundMessage> messageConsumer) {
        ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create().build();
        try {
            LarkWsBootstrapResult bootstrap = endpointDiscoveryService.resolve(config);
            session = webSocketConnector.connect(bootstrap.websocketUrl(), new LarkEndpoint(messageConsumer), endpointConfig);
        } catch (DeploymentException | IOException e) {
            throw new IllegalStateException("failed to connect to feishu websocket", e);
        }
    }

    @Override
    public CompletableFuture<Void> sendReply(InboundMessage message, String answer) {
        String token = accessTokenProvider.getTenantAccessToken(config);
        LarkReplyRequest payload = buildReply(message, answer);
        return replyApi.sendReply("Bearer " + token, message.messageId(), payload).toCompletableFuture();
    }

    @Override
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @Override
    public void close() throws IOException {
        Session current = session;
        if (current != null && current.isOpen()) {
            current.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "shutdown"));
        }
    }

    private InboundMessage parseMessage(byte[] payload) throws IOException {
        JsonNode root = mapper.readTree(payload);
        String userId = root.at("/event/sender/sender_id/open_id").asText(root.at("/sender/id").asText());
        String chatId = root.at("/event/message/chat_id").asText(root.at("/chat/id").asText());
        String messageId = root.at("/event/message/message_id").asText(root.at("/message/id").asText());
        String text = extractText(root);
        long epochMillis = root.at("/header/create_time").asLong(root.path("timestamp").asLong(System.currentTimeMillis()));
        return new InboundMessage(userId, chatId, messageId, text, Instant.ofEpochMilli(epochMillis));
    }

    private LarkReplyRequest buildReply(InboundMessage message, String answer) {
        ObjectNode content = mapper.createObjectNode();
        content.put("text", answer);
        return new LarkReplyRequest(content.toString(), "text", message.messageId());
    }

    private String extractText(JsonNode root) throws IOException {
        JsonNode structuredText = root.at("/event/message/content/text");
        if (structuredText.isTextual()) {
            return structuredText.asText();
        }

        JsonNode rawContent = root.at("/event/message/content");
        if (rawContent.isTextual()) {
            try {
                JsonNode parsedContent = mapper.readTree(rawContent.asText());
                JsonNode parsedText = parsedContent.path("text");
                if (parsedText.isTextual()) {
                    return parsedText.asText();
                }
            } catch (IOException ignored) {
                return rawContent.asText();
            }
            return rawContent.asText();
        }

        return root.at("/message/text").asText();
    }

    private void handleTextFrame(Consumer<InboundMessage> messageConsumer, String payload) {
        try {
            messageConsumer.accept(parseMessage(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("failed to parse feishu event", e);
        }
    }

    private void handleBinaryFrame(Session session, Consumer<InboundMessage> messageConsumer, byte[] payload) {
        try {
            LarkWsProtoFrame frame = frameCodec.decode(payload);
            if (frame.method() == CONTROL_FRAME_METHOD) {
                handleControlFrame(frame);
                return;
            }
            if (frame.method() != DATA_FRAME_METHOD) {
                log.debug("ignore unsupported feishu frame method {}", frame.method());
                return;
            }
            handleDataFrame(session, messageConsumer, frame);
        } catch (Exception e) {
            log.error("failed to handle feishu websocket frame", e);
        }
    }

    private void handleControlFrame(LarkWsProtoFrame frame) throws IOException {
        String type = frame.headerValue(HEADER_TYPE);
        if (MESSAGE_TYPE_PING.equals(type)) {
            return;
        }
        if (MESSAGE_TYPE_PONG.equals(type) && frame.payload().length > 0) {
            mapper.readTree(frame.payload());
        }
    }

    private void handleDataFrame(Session session, Consumer<InboundMessage> messageConsumer, LarkWsProtoFrame frame)
            throws IOException {
        byte[] payload = resolvePayload(frame);
        if (payload == null) {
            return;
        }

        long startedAt = System.currentTimeMillis();
        int statusCode = 200;
        try {
            if (MESSAGE_TYPE_EVENT.equals(frame.headerValue(HEADER_TYPE))) {
                messageConsumer.accept(parseMessage(payload));
            }
        } catch (Exception e) {
            statusCode = 500;
            log.error("failed to dispatch feishu message", e);
        }

        sendAck(session, frame, statusCode, System.currentTimeMillis() - startedAt);
    }

    private byte[] resolvePayload(LarkWsProtoFrame frame) {
        int sum = frame.headerValueAsInt(HEADER_SUM, 1);
        if (sum <= 1) {
            return frame.payload();
        }

        String messageId = frame.headerValue(HEADER_MESSAGE_ID);
        if (messageId == null || messageId.isBlank()) {
            return frame.payload();
        }
        return combinePayload(messageId, sum, frame.headerValueAsInt(HEADER_SEQ, 0), frame.payload());
    }

    private byte[] combinePayload(String messageId, int sum, int seq, byte[] payload) {
        byte[][] chunks = pendingPayloads.compute(messageId, (key, existing) -> {
            byte[][] next = existing;
            if (next == null || next.length != sum) {
                next = new byte[sum][];
            }
            if (seq >= 0 && seq < next.length) {
                next[seq] = Arrays.copyOf(payload, payload.length);
            }
            return next;
        });

        if (chunks == null || Arrays.stream(chunks).anyMatch(chunk -> chunk == null)) {
            return null;
        }

        pendingPayloads.remove(messageId);
        int totalLength = Arrays.stream(chunks).mapToInt(chunk -> chunk.length).sum();
        byte[] merged = new byte[totalLength];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, merged, offset, chunk.length);
            offset += chunk.length;
        }
        return merged;
    }

    private void sendAck(Session session, LarkWsProtoFrame frame, int statusCode, long bizRtMillis) throws IOException {
        if (session == null || !session.isOpen()) {
            return;
        }
        byte[] responsePayload = mapper.writeValueAsBytes(new LarkWsAckResponse(statusCode));
        LarkWsProtoFrame ack = frame.withPayload(responsePayload).withHeader(HEADER_BIZ_RT, Long.toString(Math.max(bizRtMillis, 0)));
        session.getAsyncRemote().sendBinary(ByteBuffer.wrap(frameCodec.encode(ack)));
    }

    private final class LarkEndpoint extends Endpoint {
        private final Consumer<InboundMessage> messageConsumer;

        private LarkEndpoint(Consumer<InboundMessage> messageConsumer) {
            this.messageConsumer = messageConsumer;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            session.addMessageHandler(byte[].class, message -> handleBinaryFrame(session, messageConsumer, message));
            session.addMessageHandler(String.class, message -> handleTextFrame(messageConsumer, message));
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            QuarkusLarkGatewayClient.this.session = null;
        }

        @Override
        public void onError(Session session, Throwable thr) {
            throw new IllegalStateException("feishu websocket error", thr);
        }
    }
}
