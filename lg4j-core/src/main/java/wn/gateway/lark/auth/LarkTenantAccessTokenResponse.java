package wn.gateway.lark.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LarkTenantAccessTokenResponse(
        int code,
        String msg,
        @JsonProperty("tenant_access_token") String tenantAccessToken,
        @JsonProperty("expire") int expireSeconds) {
}
