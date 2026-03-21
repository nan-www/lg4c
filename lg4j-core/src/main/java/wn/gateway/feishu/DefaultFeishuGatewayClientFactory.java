package wn.gateway.feishu;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;

@ApplicationScoped
public class DefaultFeishuGatewayClientFactory implements FeishuGatewayClientFactory {
    @Inject
    ObjectMapper mapper;
    @Inject
    FeishuReplyApiFactory replyApiFactory;
    @Inject
    FeishuWebSocketConnector webSocketConnector;

    @Override
    public FeishuGatewayClient create(GatewayAppConfig config) {
        return new QuarkusFeishuGatewayClient(
                config,
                mapper,
                replyApiFactory.create(config),
                webSocketConnector);
    }
}
