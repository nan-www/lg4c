package wn.gateway.lark;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.InboundMessage;
import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.DefaultLarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.LarkWsBootstrapResult;

@RequiredArgsConstructor
public class QuarkusLarkGatewayClient implements LarkGatewayClient {
    private final GatewayAppConfig config;
    private final ObjectMapper mapper;
    private final LarkReplyApi replyApi;
    private final LarkWebSocketConnector webSocketConnector;
    private final DefaultLarkEndpointDiscoveryService endpointDiscoveryService;
    private final CachedLarkAccessTokenProvider accessTokenProvider;
    private volatile Session session;

    @Override
    public void start(Consumer<InboundMessage> messageConsumer) {
        ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
                .configurator(new HeaderConfigurator(config))
                .build();
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

    private InboundMessage parseMessage(String payload) throws IOException {
        JsonNode root = mapper.readTree(payload);
        String userId = root.at("/event/sender/sender_id/open_id").asText(root.at("/sender/id").asText());
        String chatId = root.at("/event/message/chat_id").asText(root.at("/chat/id").asText());
        String messageId = root.at("/event/message/message_id").asText(root.at("/message/id").asText());
        String text = root.at("/event/message/content/text").asText(root.at("/message/text").asText());
        long epochMillis = root.at("/header/create_time").asLong(root.path("timestamp").asLong(System.currentTimeMillis()));
        return new InboundMessage(userId, chatId, messageId, text, Instant.ofEpochMilli(epochMillis));
    }

    private LarkReplyRequest buildReply(InboundMessage message, String answer) {
        ObjectNode content = mapper.createObjectNode();
        content.put("text", answer);
        return new LarkReplyRequest(content.toString(), "text", message.messageId());
    }

    private final class LarkEndpoint extends Endpoint {
        private final Consumer<InboundMessage> messageConsumer;
        private final StringBuilder buffer = new StringBuilder();

        private LarkEndpoint(Consumer<InboundMessage> messageConsumer) {
            this.messageConsumer = messageConsumer;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            session.addMessageHandler(String.class, (message, last) -> {
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
            QuarkusLarkGatewayClient.this.session = null;
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
}
