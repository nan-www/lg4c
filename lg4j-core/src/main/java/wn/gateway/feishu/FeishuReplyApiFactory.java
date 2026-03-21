package wn.gateway.feishu;

import wn.gateway.config.GatewayAppConfig;

public interface FeishuReplyApiFactory {

    FeishuReplyApi create(GatewayAppConfig config);
}
