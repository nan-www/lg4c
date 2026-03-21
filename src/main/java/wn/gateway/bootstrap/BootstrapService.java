package wn.gateway.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.config.GatewayConfigStore;

@ApplicationScoped
public class BootstrapService {
    private GatewayConfigStore store;

    public BootstrapService() {
    }

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
}
