package wn.gateway.lark.bootstrap;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LarkEndpointDiscoveryResponse(
        int code,
        String msg,
        EndpointData data) {
    public static LarkEndpointDiscoveryResponse success(String websocketUrl, LarkClientRuntimeConfig runtimeConfig) {
        return new LarkEndpointDiscoveryResponse(0, "ok", new EndpointData(websocketUrl, runtimeConfig));
    }

    public record EndpointData(
            @JsonProperty("URL") String url,
            @JsonProperty("ClientConfig") ClientConfigData clientConfig) {
        public EndpointData(String url, LarkClientRuntimeConfig runtimeConfig) {
            this(url, runtimeConfig == null ? null : new ClientConfigData(
                    runtimeConfig.reconnectCount(),
                    runtimeConfig.reconnectIntervalSeconds(),
                    runtimeConfig.reconnectNonceSeconds(),
                    runtimeConfig.pingIntervalSeconds()));
        }
    }

    public record ClientConfigData(
            @JsonProperty("ReconnectCount") Integer reconnectCount,
            @JsonProperty("ReconnectInterval") Integer reconnectInterval,
            @JsonProperty("ReconnectNonce") Integer reconnectNonce,
            @JsonProperty("PingInterval") Integer pingInterval) {
        public LarkClientRuntimeConfig toRuntimeConfig() {
            return new LarkClientRuntimeConfig(
                    reconnectCount == null ? LarkClientRuntimeConfig.DEFAULT.reconnectCount() : reconnectCount,
                    reconnectInterval == null ? LarkClientRuntimeConfig.DEFAULT.reconnectIntervalSeconds() : reconnectInterval,
                    reconnectNonce == null ? LarkClientRuntimeConfig.DEFAULT.reconnectNonceSeconds() : reconnectNonce,
                    pingInterval == null ? LarkClientRuntimeConfig.DEFAULT.pingIntervalSeconds() : pingInterval);
        }
    }
}
