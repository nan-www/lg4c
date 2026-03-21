package wn.gateway.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GatewayConfigStore {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    public Path configFile(Path homeDir) {
        return homeDir.resolve(".lg4c/config/application.yml");
    }

    public GatewayAppConfig load(Path homeDir) throws IOException {
        ConfigDocument document = YAML.readValue(configFile(homeDir).toFile(), ConfigDocument.class);
        GatewayAppConfig config = GatewayAppConfig.builder()
                .codexCommand(document.gateway.codex.command)
                .workspaceRoot(Path.of(document.gateway.workspace.root))
                .recordRoot(Path.of(document.gateway.record.root))
                .agentTemplate(document.gateway.agent.template)
                .feishuAppId(document.gateway.feishu.appId)
                .feishuAppSecret(document.gateway.feishu.appSecret)
                .feishuWebsocketUrl(document.gateway.feishu.websocketUrl)
                .feishuReplyUrl(document.gateway.feishu.replyUrl)
                .allowedUsers(document.gateway.access.allowedUsers)
                .allowedChats(document.gateway.access.allowedChats)
                .loggingLevel(document.gateway.logging.level)
                .build();
        config.validate();
        return config;
    }

    public void save(GatewayAppConfig config) throws IOException {
        throw new UnsupportedOperationException("homeDir is required");
    }

    public void save(Path homeDir, GatewayAppConfig config) throws IOException {
        config.validate();
        Files.createDirectories(configFile(homeDir).getParent());
        ConfigDocument document = ConfigDocument.from(config);
        YAML.writeValue(configFile(homeDir).toFile(), document);
    }

    public GatewayAppConfig loadOrThrow(Path homeDir) {
        try {
            return load(homeDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unused")
    static final class ConfigDocument {
        public GatewayDocument gateway;

        static ConfigDocument from(GatewayAppConfig config) {
            ConfigDocument document = new ConfigDocument();
            document.gateway = new GatewayDocument();
            document.gateway.codex = new CodexDocument();
            document.gateway.codex.command = config.codexCommand();
            document.gateway.workspace = new WorkspaceDocument();
            document.gateway.workspace.root = config.workspaceRoot().toString();
            document.gateway.record = new RecordDocument();
            document.gateway.record.root = config.recordRoot().toString();
            document.gateway.agent = new AgentDocument();
            document.gateway.agent.template = config.agentTemplate();
            document.gateway.feishu = new LarkDocument();
            document.gateway.feishu.appId = config.feishuAppId();
            document.gateway.feishu.appSecret = config.feishuAppSecret();
            document.gateway.feishu.websocketUrl = config.feishuWebsocketUrl();
            document.gateway.feishu.replyUrl = config.feishuReplyUrl();
            document.gateway.access = new AccessDocument();
            document.gateway.access.allowedUsers = config.allowedUsers();
            document.gateway.access.allowedChats = config.allowedChats();
            document.gateway.logging = new LoggingDocument();
            document.gateway.logging.level = config.loggingLevel();
            return document;
        }
    }

    @SuppressWarnings("unused")
    static final class GatewayDocument {
        public CodexDocument codex;
        public WorkspaceDocument workspace;
        public RecordDocument record;
        public AgentDocument agent;
        public LarkDocument feishu;
        public AccessDocument access;
        public LoggingDocument logging;
    }

    @SuppressWarnings("unused")
    static final class CodexDocument {
        public List<String> command;
    }

    @SuppressWarnings("unused")
    static final class WorkspaceDocument {
        public String root;
    }

    @SuppressWarnings("unused")
    static final class RecordDocument {
        public String root;
    }

    @SuppressWarnings("unused")
    static final class AgentDocument {
        public String template;
    }

    @SuppressWarnings("unused")
    static final class LarkDocument {
        public String appId;
        public String appSecret;
        public String websocketUrl;
        public String replyUrl;
    }

    @SuppressWarnings("unused")
    static final class AccessDocument {
        public List<String> allowedUsers;
        public List<String> allowedChats;
    }

    @SuppressWarnings("unused")
    static final class LoggingDocument {
        public String level;
    }
}
