package wn.gateway.feishu;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.util.VTFactory;

@ApplicationScoped
public class DefaultFeishuGatewayClientFactory implements FeishuGatewayClientFactory {
    @Inject
    ObjectMapper mapper;
    @Inject
    FeishuReplyApiFactory replyApiFactory;
    @Inject
    FeishuWebSocketConnector webSocketConnector;
    @Inject
    VTFactory vtFactory;

    @Override
    public FeishuGatewayClient create(GatewayAppConfig config) {
        return new QuarkusFeishuGatewayClient(
                config,
                mapper,
                replyApiFactory.create(config),
                webSocketConnector,
                vtFactory.newVirtualThreadExecutor("feishu-network"));
    }
}
