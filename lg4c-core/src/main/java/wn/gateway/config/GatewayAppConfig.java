package wn.gateway.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import wn.gateway.lark.LarkEnvironment;

@Getter
@ToString
@EqualsAndHashCode
@Accessors(fluent = true)
@Builder(toBuilder = true)
public class GatewayAppConfig {
    private static final String DEFAULT_AGENT_TEMPLATE = """
            # LG4C Agent

            You are running under LG4C.
            Keep responses focused on the user's request and operate only inside the configured repository root.
            """;

    private final List<String> codexCommand;
    private final Path workspaceRoot;
    private final Path recordRoot;
    private final String agentTemplate;
    private final String feishuAppId;
    private final String feishuAppSecret;
    private final String feishuBaseUrl;
    private final String feishuWebsocketUrl;
    private final String feishuReplyUrl;
    private final List<String> allowedUsers;
    private final List<String> allowedChats;
    private final String loggingLevel;

    private GatewayAppConfig(
            List<String> codexCommand,
            Path workspaceRoot,
            Path recordRoot,
            String agentTemplate,
            String feishuAppId,
            String feishuAppSecret,
            String feishuBaseUrl,
            String feishuWebsocketUrl,
            String feishuReplyUrl,
            List<String> allowedUsers,
            List<String> allowedChats,
            String loggingLevel) {
        this.codexCommand = List.copyOf(Objects.requireNonNull(codexCommand));
        this.workspaceRoot = Objects.requireNonNull(workspaceRoot);
        this.recordRoot = Objects.requireNonNull(recordRoot);
        this.agentTemplate = agentTemplate == null ? DEFAULT_AGENT_TEMPLATE : agentTemplate;
        this.feishuAppId = Objects.requireNonNull(feishuAppId);
        this.feishuAppSecret = Objects.requireNonNull(feishuAppSecret);
        this.feishuBaseUrl = feishuBaseUrl == null || feishuBaseUrl.isBlank()
                ? LarkEnvironment.DEFAULT_BASE_URL
                : feishuBaseUrl;
        this.feishuWebsocketUrl = blankToNull(feishuWebsocketUrl);
        this.feishuReplyUrl = blankToNull(feishuReplyUrl);
        this.allowedUsers = List.copyOf(Objects.requireNonNull(allowedUsers));
        this.allowedChats = List.copyOf(Objects.requireNonNull(allowedChats));
        this.loggingLevel = loggingLevel == null || loggingLevel.isBlank() ? "INFO" : loggingLevel;
    }

    public void validate() {
        require(!codexCommand.isEmpty(), "gateway.codex.command must not be empty");
        require(workspaceRoot != null, "gateway.workspace.root must be configured");
        require(recordRoot != null, "gateway.record.root must be configured");
        require(agentTemplate != null && !agentTemplate.isBlank(), "gateway.agent.template must be configured");
        require(feishuAppId != null && !feishuAppId.isBlank(), "gateway.feishu.app-id must be configured");
        require(feishuAppSecret != null && !feishuAppSecret.isBlank(), "gateway.feishu.app-secret must be configured");
    }

    public LarkEnvironment larkEnvironment() {
        return new LarkEnvironment(feishuBaseUrl, feishuWebsocketUrl, feishuReplyUrl);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
