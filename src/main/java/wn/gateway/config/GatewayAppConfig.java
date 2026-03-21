package wn.gateway.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record GatewayAppConfig(
        List<String> codexCommand,
        Path workspaceRoot,
        Path recordRoot,
        String agentTemplate,
        String feishuAppId,
        String feishuAppSecret,
        String feishuWebsocketUrl,
        String feishuReplyUrl,
        List<String> allowedUsers,
        List<String> allowedChats,
        String loggingLevel) {

    public GatewayAppConfig {
        codexCommand = List.copyOf(codexCommand);
        allowedUsers = List.copyOf(allowedUsers);
        allowedChats = List.copyOf(allowedChats);
        loggingLevel = loggingLevel == null || loggingLevel.isBlank() ? "INFO" : loggingLevel;
    }

    public void validate() {
        require(!codexCommand.isEmpty(), "gateway.codex.command must not be empty");
        require(workspaceRoot != null, "gateway.workspace.root must be configured");
        require(recordRoot != null, "gateway.record.root must be configured");
        require(agentTemplate != null && !agentTemplate.isBlank(), "gateway.agent.template must be configured");
        require(feishuAppId != null && !feishuAppId.isBlank(), "gateway.feishu.app-id must be configured");
        require(feishuAppSecret != null && !feishuAppSecret.isBlank(), "gateway.feishu.app-secret must be configured");
        require(feishuWebsocketUrl != null && !feishuWebsocketUrl.isBlank(), "gateway.feishu.websocket-url must be configured");
        require(feishuReplyUrl != null && !feishuReplyUrl.isBlank(), "gateway.feishu.reply-url must be configured");
        require(!allowedUsers.isEmpty(), "gateway.access.allowed-users must not be empty");
        require(!allowedChats.isEmpty(), "gateway.access.allowed-chats must not be empty");
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static final class Builder {
        private final List<String> codexCommand = new ArrayList<>();
        private Path workspaceRoot;
        private Path recordRoot;
        private String agentTemplate = """
                # LG4C Agent

                You are running under LG4C.
                Keep responses focused on the user's request and operate only inside the configured repository root.
                """;
        private String feishuAppId;
        private String feishuAppSecret;
        private String feishuWebsocketUrl;
        private String feishuReplyUrl;
        private final List<String> allowedUsers = new ArrayList<>();
        private final List<String> allowedChats = new ArrayList<>();
        private String loggingLevel = "INFO";

        public Builder codexCommand(List<String> value) {
            codexCommand.clear();
            codexCommand.addAll(value);
            return this;
        }

        public Builder workspaceRoot(Path value) {
            workspaceRoot = value;
            return this;
        }

        public Builder recordRoot(Path value) {
            recordRoot = value;
            return this;
        }

        public Builder agentTemplate(String value) {
            agentTemplate = value;
            return this;
        }

        public Builder feishuAppId(String value) {
            feishuAppId = value;
            return this;
        }

        public Builder feishuAppSecret(String value) {
            feishuAppSecret = value;
            return this;
        }

        public Builder feishuWebsocketUrl(String value) {
            feishuWebsocketUrl = value;
            return this;
        }

        public Builder feishuReplyUrl(String value) {
            feishuReplyUrl = value;
            return this;
        }

        public Builder allowedUsers(List<String> value) {
            allowedUsers.clear();
            allowedUsers.addAll(value);
            return this;
        }

        public Builder allowedChats(List<String> value) {
            allowedChats.clear();
            allowedChats.addAll(value);
            return this;
        }

        public Builder loggingLevel(String value) {
            loggingLevel = value;
            return this;
        }

        public GatewayAppConfig build() {
            return new GatewayAppConfig(
                    List.copyOf(codexCommand),
                    Objects.requireNonNull(workspaceRoot),
                    Objects.requireNonNull(recordRoot),
                    Objects.requireNonNull(agentTemplate),
                    Objects.requireNonNull(feishuAppId),
                    Objects.requireNonNull(feishuAppSecret),
                    Objects.requireNonNull(feishuWebsocketUrl),
                    Objects.requireNonNull(feishuReplyUrl),
                    List.copyOf(allowedUsers),
                    List.copyOf(allowedChats),
                    loggingLevel);
        }
    }
}
