package wn.gateway.lark.bootstrap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LarkEndpointDiscoveryRequest(
        @JsonProperty("AppID") String appId,
        @JsonProperty("AppSecret") String appSecret) {
}
