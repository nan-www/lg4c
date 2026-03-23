package wn.gateway.lark.bootstrap.dto;

import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;

public record LarkWsBootstrapResult(
        String websocketUrl,
        LarkClientRuntimeConfig runtimeConfig) {
}
