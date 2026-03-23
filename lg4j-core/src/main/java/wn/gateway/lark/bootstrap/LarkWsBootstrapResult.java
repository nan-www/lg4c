package wn.gateway.lark.bootstrap;

public record LarkWsBootstrapResult(
        String websocketUrl,
        LarkClientRuntimeConfig runtimeConfig) {
}
