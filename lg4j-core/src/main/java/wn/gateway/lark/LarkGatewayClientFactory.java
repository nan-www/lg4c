package wn.gateway.lark;

import wn.gateway.config.GatewayAppConfig;

public interface LarkGatewayClientFactory {

    LarkGatewayClient create(GatewayAppConfig config);
}
