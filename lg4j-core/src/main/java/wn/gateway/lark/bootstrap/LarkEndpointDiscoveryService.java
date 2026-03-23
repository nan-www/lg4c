package wn.gateway.lark.bootstrap;

import wn.gateway.config.GatewayAppConfig;

public interface LarkEndpointDiscoveryService {

    LarkWsBootstrapResult resolve(GatewayAppConfig config);
}
