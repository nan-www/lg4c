package wn.gateway.lark.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;

@ApplicationScoped
public class LarkEndpointDiscoveryService {
    private final LarkEndpointDiscoveryApiFactory apiFactory;

    @Inject
    public LarkEndpointDiscoveryService(LarkEndpointDiscoveryApiFactory apiFactory) {
        this.apiFactory = apiFactory;
    }

    public LarkWsBootstrapResult resolve(GatewayAppConfig config) {
        if (config.feishuWebsocketUrl() != null) {
            return new LarkWsBootstrapResult(config.feishuWebsocketUrl(), LarkClientRuntimeConfig.DEFAULT);
        }

        LarkEndpointDiscoveryResponse response = apiFactory.create(config)
                .discover(new LarkEndpointDiscoveryRequest(config.feishuAppId(), config.feishuAppSecret()));
        if (response == null || response.data() == null || response.data().url() == null || response.data().url().isBlank()) {
            throw new IllegalStateException("failed to discover feishu websocket endpoint");
        }
        if (response.code() != 0) {
            throw new IllegalStateException("failed to discover feishu websocket endpoint: " + response.msg());
        }
        LarkClientRuntimeConfig runtimeConfig = response.data().clientConfig() == null
                ? LarkClientRuntimeConfig.DEFAULT
                : response.data().clientConfig().toRuntimeConfig();
        return new LarkWsBootstrapResult(response.data().url(), runtimeConfig);
    }
}
