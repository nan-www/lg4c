package wn.gateway.lark;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;

@ApplicationScoped
public class DefaultLarkGatewayClientFactory implements LarkGatewayClientFactory {
    @Inject
    ObjectMapper mapper;
    @Inject
    LarkReplyApiFactory replyApiFactory;
    @Inject
    LarkWebSocketConnector webSocketConnector;

    @Override
    public LarkGatewayClient create(GatewayAppConfig config) {
        return new QuarkusLarkGatewayClient(
                config,
                mapper,
                replyApiFactory.create(config),
                webSocketConnector);
    }
}
