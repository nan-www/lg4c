package wn.gateway.lark;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.InboundMessage;
import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.dto.LarkReplyRequest;

@RequiredArgsConstructor
public class QuarkusLarkGatewayClient implements LarkGatewayClient {
    private final GatewayAppConfig config;
    private final ObjectMapper mapper;
    private final LarkReplyApi replyApi;
    private final OfficialLarkSdkLongConnectionFactory sdkLongConnectionFactory;
    private final CachedLarkAccessTokenProvider accessTokenProvider;
    private volatile LarkSdkLongConnection sdkConnection;

    @Override
    public void start(Consumer<InboundMessage> messageConsumer) {
        LarkSdkLongConnection connection = sdkLongConnectionFactory.create(config, payload -> forwardPayload(messageConsumer, payload));
        connection.start();
        sdkConnection = connection;
    }

    @Override
    public CompletableFuture<Void> sendReply(InboundMessage message, String answer) {
        String token = accessTokenProvider.getTenantAccessToken(config);
        LarkReplyRequest payload = buildReply(message, answer);
        return replyApi.sendReply("Bearer " + token, message.messageId(), payload).toCompletableFuture();
    }

    @Override
    public boolean isConnected() {
        LarkSdkLongConnection connection = sdkConnection;
        return connection != null && connection.isConnected();
    }

    @Override
    public void close() throws IOException {
        LarkSdkLongConnection connection = sdkConnection;
        if (connection != null) {
            connection.close();
        }
    }

    private void forwardPayload(Consumer<InboundMessage> messageConsumer, byte[] payload) {
        try {
            messageConsumer.accept(parseMessage(payload));
        } catch (IOException e) {
            throw new IllegalStateException("failed to parse feishu event", e);
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

    private LarkReplyRequest buildReply(InboundMessage message, String answer) {
        ObjectNode content = mapper.createObjectNode();
        content.put("text", answer);
        return new LarkReplyRequest(content.toString(), "text", message.messageId());
    }
}
