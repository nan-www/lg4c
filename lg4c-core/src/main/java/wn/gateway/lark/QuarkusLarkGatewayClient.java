package wn.gateway.lark;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.EventMessage;
import com.lark.oapi.service.im.v1.model.EventSender;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.UserId;
import com.lark.oapi.ws.Client;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.InboundMessage;
import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.dto.LarkReplyRequest;

@RequiredArgsConstructor
public class QuarkusLarkGatewayClient implements LarkGatewayClient {
    private static final Logger log = LoggerFactory.getLogger(QuarkusLarkGatewayClient.class);
    private static final String FIELD_EXECUTOR = "executor";
    private static final String FIELD_AUTO_RECONNECT = "autoReconnect";
    private static final String METHOD_DISCONNECT = "disconnect";

    private final GatewayAppConfig config;
    private final ObjectMapper mapper;
    private final LarkReplyApi replyApi;
    private final CachedLarkAccessTokenProvider accessTokenProvider;
    private volatile Client sdkClient;
    private volatile boolean started;

    @Override
    public void start(Consumer<InboundMessage> messageConsumer) {
        if (config.feishuWebsocketUrl() != null) {
            log.warn("feishu websocket override is ignored when using official lark sdk");
        }

        EventDispatcher dispatcher = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) throws Exception {
                        messageConsumer.accept(parseMessage(event));
                    }
                })
                .build();
        Client client = new Client.Builder(config.feishuAppId(), config.feishuAppSecret())
                .domain(config.larkEnvironment().baseUrl())
                .eventHandler(dispatcher)
                .build();
        client.start();
        sdkClient = client;
        started = true;
    }

    @Override
    public CompletableFuture<Void> sendReply(InboundMessage message, String answer) {
        String token = accessTokenProvider.getTenantAccessToken(config);
        LarkReplyRequest payload = buildReply(message, answer);
        return replyApi.sendReply("Bearer " + token, message.messageId(), payload).toCompletableFuture();
    }

    @Override
    public boolean isConnected() {
        return sdkClient != null && started;
    }

    @Override
    public void close() throws IOException {
        Client client = sdkClient;
        sdkClient = null;
        started = false;
        if (client == null) {
            return;
        }
        try {
            writeField(client, FIELD_AUTO_RECONNECT, Boolean.FALSE);
            invokeDisconnect(client);
            shutdownExecutor(client);
        } catch (ReflectiveOperationException e) {
            throw new IOException("failed to close official lark sdk client", e);
        }
    }

    private InboundMessage parseMessage(P2MessageReceiveV1 event) throws IOException {
        EventSender sender = event.getEvent() == null ? null : event.getEvent().getSender();
        EventMessage message = event.getEvent() == null ? null : event.getEvent().getMessage();
        String userId = extractUserId(sender == null ? null : sender.getSenderId());
        String chatId = message == null ? "" : message.getChatId();
        String messageId = message == null ? "" : message.getMessageId();
        String text = extractText(message);
        long epochMillis = parseEpochMillis(
                message == null ? null : message.getCreateTime(),
                event.getHeader() == null ? null : event.getHeader().getCreateTime());
        return new InboundMessage(userId, chatId, messageId, text, Instant.ofEpochMilli(epochMillis));
    }

    private String extractUserId(UserId senderId) {
        if (senderId == null) {
            return "";
        }
        return firstNonBlank(senderId.getOpenId(), senderId.getUserId(), senderId.getUnionId());
    }

    private String extractText(EventMessage message) throws IOException {
        if (message == null || message.getContent() == null) {
            return "";
        }
        JsonNode rawContent;
        try {
            rawContent = mapper.readTree(message.getContent());
        } catch (IOException e) {
            return message.getContent();
        }
        JsonNode parsedText = rawContent.path("text");
        if (parsedText.isTextual()) {
            return parsedText.asText();
        }
        if (rawContent.isTextual()) {
            try {
                JsonNode nestedContent = mapper.readTree(rawContent.asText());
                JsonNode nestedText = nestedContent.path("text");
                if (nestedText.isTextual()) {
                    return nestedText.asText();
                }
            } catch (IOException ignored) {
                return rawContent.asText();
            }
            return rawContent.asText();
        }
        return message.getContent();
    }

    private long parseEpochMillis(String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                return Long.parseLong(candidate);
            } catch (NumberFormatException ignored) {
                // Fall through to the next candidate.
            }
        }
        return System.currentTimeMillis();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private void invokeDisconnect(Client client) throws ReflectiveOperationException {
        Method method = Client.class.getDeclaredMethod(METHOD_DISCONNECT);
        method.setAccessible(true);
        method.invoke(client);
    }

    private void shutdownExecutor(Client client) throws ReflectiveOperationException {
        Object value = readField(client, FIELD_EXECUTOR);
        if (value instanceof ExecutorService executorService) {
            executorService.shutdownNow();
        }
    }

    private void writeField(Client client, String name, Object value) throws ReflectiveOperationException {
        Field field = Client.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(client, value);
    }

    private Object readField(Client client, String name) throws ReflectiveOperationException {
        Field field = Client.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(client);
    }

    private LarkReplyRequest buildReply(InboundMessage message, String answer) {
        ObjectNode content = mapper.createObjectNode();
        content.put("text", answer);
        return new LarkReplyRequest(content.toString(), "text", message.messageId());
    }
}
