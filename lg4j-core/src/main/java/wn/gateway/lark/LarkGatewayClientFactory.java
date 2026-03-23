package wn.gateway.lark;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.bootstrap.DefaultLarkEndpointDiscoveryService;

@ApplicationScoped
public class LarkGatewayClientFactory {
    @Inject
    ObjectMapper mapper;
    @Inject
    LarkReplyApiFactory replyApiFactory;
    @Inject
    LarkWebSocketConnector webSocketConnector;
    @Inject
    DefaultLarkEndpointDiscoveryService endpointDiscoveryService;
    @Inject
    CachedLarkAccessTokenProvider accessTokenProvider;

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
