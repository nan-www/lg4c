package wn.gateway.lark;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.InboundMessage;

abstract class LarkTestSupport {

    protected static GatewayAppConfig config() {
        return configBuilder().build();
    }

    protected static GatewayAppConfig.GatewayAppConfigBuilder configBuilder() {
        return GatewayAppConfig.builder()
                .codexCommand(List.of("codex"))
                .workspaceRoot(Path.of("/tmp/workspace"))
                .recordRoot(Path.of("/tmp/.lg4c/records"))
                .agentTemplate("agent")
                .feishuAppId("app-id")
                .feishuAppSecret("app-secret")
                .allowedUsers(List.of("ou_1"))
                .allowedChats(List.of("oc_1"))
                .loggingLevel("INFO");
    }

    protected static InboundMessage message() {
        return new InboundMessage("ou_1", "oc_1", "om_1", "hello", Instant.now());
    }
}
