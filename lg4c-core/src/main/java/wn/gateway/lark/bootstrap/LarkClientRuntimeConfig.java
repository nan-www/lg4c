package wn.gateway.lark.bootstrap;

public record LarkClientRuntimeConfig(
        int reconnectCount,
        int reconnectIntervalSeconds,
        int reconnectNonceSeconds,
        int pingIntervalSeconds) {
    public static final LarkClientRuntimeConfig DEFAULT = new LarkClientRuntimeConfig(-1, 120, 30, 120);
}
