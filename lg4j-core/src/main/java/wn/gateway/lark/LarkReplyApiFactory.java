package wn.gateway.lark;

import wn.gateway.config.GatewayAppConfig;

public interface LarkReplyApiFactory {

    LarkReplyApi create(GatewayAppConfig config);
}
