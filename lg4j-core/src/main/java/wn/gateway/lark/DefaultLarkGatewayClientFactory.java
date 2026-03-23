package wn.gateway.lark;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.lark.auth.LarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryService;

@ApplicationScoped
public class DefaultLarkGatewayClientFactory implements LarkGatewayClientFactory {
    @Inject
    ObjectMapper mapper;
    @Inject
    LarkReplyApiFactory replyApiFactory;
    @Inject
    LarkWebSocketConnector webSocketConnector;
    @Inject
    LarkEndpointDiscoveryService endpointDiscoveryService;
    @Inject
    LarkAccessTokenProvider accessTokenProvider;

    @Override
    public LarkGatewayClient create(GatewayAppConfig config) {
        return new QuarkusLarkGatewayClient(
                config,
                mapper,
                replyApiFactory.create(config),
                webSocketConnector,
                endpointDiscoveryService,
                accessTokenProvider);
    }
}
