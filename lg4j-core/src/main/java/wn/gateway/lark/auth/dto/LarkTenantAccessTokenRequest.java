package wn.gateway.lark.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LarkTenantAccessTokenRequest(
        @JsonProperty("app_id") String appId,
        @JsonProperty("app_secret") String appSecret) {
}
