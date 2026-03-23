package wn.gateway.lark.auth;

import wn.gateway.config.GatewayAppConfig;

public interface LarkTenantAccessTokenApiFactory {

    LarkTenantAccessTokenApi create(GatewayAppConfig config);
}
