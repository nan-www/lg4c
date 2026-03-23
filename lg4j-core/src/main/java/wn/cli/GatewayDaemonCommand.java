package wn.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import wn.gateway.bootstrap.BootstrapService;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.config.GatewayConfigStore;
import wn.gateway.runtime.GatewayDaemonService;

@Dependent
@Command(name = "daemon", description = "Run lg4c as a local daemon")
public class GatewayDaemonCommand implements Callable<Integer> {
    @Inject
    GatewayConfigStore store;

    @Inject
    BootstrapService bootstrapService;

    @Inject
    GatewayDaemonService daemonService;

    @Option(names = "--bootstrap")
    boolean bootstrap;

    @Option(names = "--home", defaultValue = "${sys:user.home}")
    Path home;

    @Option(names = "--workspace")
    Path workspace;

    @Option(names = "--codex-command", split = ",", defaultValue = "codex")
    List<String> codexCommand = new ArrayList<>();

    @Option(names = "--feishu-app-id")
    String feishuAppId;

    @Option(names = "--feishu-app-secret")
    String feishuAppSecret;

    @Option(names = "--feishu-base-url")
    String feishuBaseUrl;

    @Option(names = "--feishu-websocket-url")
    String feishuWebsocketUrl;

    @Option(names = "--feishu-reply-url")
    String feishuReplyUrl;

    @Option(names = "--allowed-user")
    List<String> allowedUsers = new ArrayList<>();

    @Option(names = "--allowed-chat")
    List<String> allowedChats = new ArrayList<>();

    @Option(names = "--logging-level", defaultValue = "INFO")
    String loggingLevel;

    @Override
    public Integer call() throws Exception {
        if (bootstrap) {
            if (workspace == null) {
                throw new IllegalArgumentException("--workspace is required with --bootstrap");
            }
            GatewayAppConfig config = GatewayAppConfig.builder()
                    .codexCommand(codexCommand)
                    .workspaceRoot(workspace)
                    .recordRoot(home.resolve(".lg4c/records"))
                    .agentTemplate(defaultAgentTemplate())
                    .feishuAppId(feishuAppId)
                    .feishuAppSecret(feishuAppSecret)
                    .feishuBaseUrl(feishuBaseUrl)
                    .feishuWebsocketUrl(feishuWebsocketUrl)
                    .feishuReplyUrl(feishuReplyUrl)
                    .allowedUsers(allowedUsers)
                    .allowedChats(allowedChats)
                    .loggingLevel(loggingLevel)
                    .build();
            bootstrapService.initializeWorkspace(config);
            bootstrapService.save(home, config);
            System.out.printf("Bootstrap complete: %s%n", store.configFile(home));
            System.out.println(bootstrapService.feishuBootstrapSummary());
            bootstrapService.warningsFor(config).forEach(System.out::println);
            return 0;
        }

        if (!store.exists(home)) {
            System.err.println("Missing lg4c config. Run `lg4c daemon --bootstrap` first.");
            return 2;
        }

        GatewayAppConfig config = store.load(home);
        daemonService.run(config);
        return 0;
    }

    private String defaultAgentTemplate() {
        return """
                # LG4C Agent

                You are running under LG4C(Local gateway for Codex).
                Only operate inside the configured repository root.
                Keep replies concise and focused on the incoming Feishu request.
                """;
    }
}
