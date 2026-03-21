package wn.gateway.feishu;

import wn.gateway.config.GatewayAppConfig;

public interface FeishuGatewayClientFactory {

    FeishuGatewayClient create(GatewayAppConfig config);
}
