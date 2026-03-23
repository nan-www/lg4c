package wn.gateway.lark.bootstrap;

import wn.gateway.config.GatewayAppConfig;

public interface LarkEndpointDiscoveryApiFactory {

    LarkEndpointDiscoveryApi create(GatewayAppConfig config);
}
