package wn.gateway.feishu;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.InboundMessage;

public class QuarkusFeishuGatewayClient implements FeishuGatewayClient {
    private final GatewayAppConfig config;
    private final ObjectMapper mapper;
    private final FeishuReplyApi replyApi;
    private final FeishuWebSocketConnector webSocketConnector;
    private final ExecutorService networkExecutor;
    private final boolean managesExecutor;
    private volatile Session session;

    public QuarkusFeishuGatewayClient(
            GatewayAppConfig config,
            ObjectMapper mapper,
            FeishuReplyApi replyApi,
            FeishuWebSocketConnector webSocketConnector,
            ExecutorService networkExecutor) {
        this(config, mapper, replyApi, webSocketConnector, networkExecutor, true);
    }

    public QuarkusFeishuGatewayClient(
            GatewayAppConfig config,
            ObjectMapper mapper,
            FeishuReplyApi replyApi,
            FeishuWebSocketConnector webSocketConnector,
            ExecutorService networkExecutor,
            boolean managesExecutor) {
        this.config = Objects.requireNonNull(config);
        this.mapper = Objects.requireNonNull(mapper);
        this.replyApi = Objects.requireNonNull(replyApi);
        this.webSocketConnector = Objects.requireNonNull(webSocketConnector);
        this.networkExecutor = Objects.requireNonNull(networkExecutor);
        this.managesExecutor = managesExecutor;
    }

    @Override
    public void start(Consumer<InboundMessage> messageConsumer) {
        ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
                .configurator(new HeaderConfigurator(config))
                .build();
        try {
            session = runOnVirtualThread(() -> webSocketConnector.connect(config, new FeishuEndpoint(messageConsumer), endpointConfig));
        } catch (IOException e) {
            throw new IllegalStateException("failed to connect to feishu websocket", e);
        }
    }

    @Override
    public CompletableFuture<Void> sendReply(InboundMessage message, String answer) {
        FeishuReplyRequest payload = new FeishuReplyRequest(
                message.messageId(),
                message.chatId(),
                message.userId(),
                answer);
        return CompletableFuture.runAsync(
                () -> replyApi.sendReply(config.feishuAppId(), config.feishuAppSecret(), payload)
                        .toCompletableFuture()
                        .join(),
                networkExecutor);
    }

    @Override
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @Override
    public void close() throws IOException {
        try {
            Session current = session;
            if (current != null && current.isOpen()) {
                current.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "shutdown"));
            }
        } finally {
            if (managesExecutor) {
                networkExecutor.shutdownNow();
            }
        }
    }

    private <T> T runOnVirtualThread(IoCallable<T> operation) throws IOException {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return operation.call();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, networkExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IllegalStateException("virtual-thread network operation failed", cause);
        }
    }

    private InboundMessage parseMessage(String payload) throws IOException {
        JsonNode root = mapper.readTree(payload);
        String userId = root.at("/event/sender/sender_id/open_id").asText(root.at("/sender/id").asText());
        String chatId = root.at("/event/message/chat_id").asText(root.at("/chat/id").asText());
        String messageId = root.at("/event/message/message_id").asText(root.at("/message/id").asText());
        String text = root.at("/event/message/content/text").asText(root.at("/message/text").asText());
        long epochMillis = root.at("/header/create_time").asLong(root.path("timestamp").asLong(System.currentTimeMillis()));
        return new InboundMessage(userId, chatId, messageId, text, Instant.ofEpochMilli(epochMillis));
    }

    private final class FeishuEndpoint extends Endpoint {
        private final Consumer<InboundMessage> messageConsumer;
        private final StringBuilder buffer = new StringBuilder();

        private FeishuEndpoint(Consumer<InboundMessage> messageConsumer) {
            this.messageConsumer = messageConsumer;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            session.addMessageHandler(String.class, (MessageHandler.Partial<String>) (message, last) -> {
                buffer.append(message);
                if (last) {
                    try {
                        messageConsumer.accept(parseMessage(buffer.toString()));
                    } catch (IOException e) {
                        throw new IllegalStateException("failed to parse feishu event", e);
                    } finally {
                        buffer.setLength(0);
                    }
                }
            });
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            QuarkusFeishuGatewayClient.this.session = null;
        }

        @Override
        public void onError(Session session, Throwable thr) {
            throw new IllegalStateException("feishu websocket error", thr);
        }
    }

    private static final class HeaderConfigurator extends ClientEndpointConfig.Configurator {
        private final GatewayAppConfig config;

        private HeaderConfigurator(GatewayAppConfig config) {
            this.config = config;
        }

        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            headers.put("X-Feishu-App-Id", List.of(config.feishuAppId()));
            headers.put("X-Feishu-App-Secret", List.of(config.feishuAppSecret()));
        }
    }

    @FunctionalInterface
    private interface IoCallable<T> {
        T call() throws Exception;
    }
}
