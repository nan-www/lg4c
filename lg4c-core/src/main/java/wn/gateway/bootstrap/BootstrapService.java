package wn.gateway.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.config.GatewayConfigStore;

@ApplicationScoped
public class BootstrapService {
    private static final String FEISHU_BOOTSTRAP_SUMMARY =
            "Feishu bootstrap: provide APP_ID and APP_SECRET only; endpoint discovery and tenant token management are automatic.";

    private GatewayConfigStore store;

    @Inject
    public BootstrapService(GatewayConfigStore store) {
        this.store = store;
    }

    public void initializeWorkspace(GatewayAppConfig config) throws IOException {
        Files.createDirectories(config.workspaceRoot());
        Files.createDirectories(config.recordRoot());
        Path agentFile = config.workspaceRoot().resolve("AGENT.md");
        if (Files.notExists(agentFile)) {
            Files.writeString(agentFile, config.agentTemplate());
        }
    }

    public void save(Path homeDir, GatewayAppConfig config) throws IOException {
        store.save(homeDir, config);
    }

    public String feishuBootstrapSummary() {
        return FEISHU_BOOTSTRAP_SUMMARY;
    }

    public List<String> warningsFor(GatewayAppConfig config) {
        List<String> warnings = new ArrayList<>();
        if (config.feishuWebsocketUrl() != null || config.feishuReplyUrl() != null) {
            warnings.add(
                    "Warning: manual Feishu websocket/reply endpoint overrides are deprecated. Re-run bootstrap without them unless you need expert overrides.");
        }
        return warnings;
    }
}
