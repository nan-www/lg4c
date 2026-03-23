package wn.gateway.lark.auth;

import wn.gateway.config.GatewayAppConfig;

public interface LarkAccessTokenProvider {

    String getTenantAccessToken(GatewayAppConfig config);
}
