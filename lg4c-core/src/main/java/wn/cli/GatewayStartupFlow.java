package wn.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.bootstrap.BootstrapService;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.config.GatewayConfigStore;
import wn.gateway.runtime.GatewayDaemonService;

@ApplicationScoped
public class GatewayStartupFlow {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String START_BANNER_COLOR = "\u001B[38;2;183;230;163m";
    static final String START_BANNER = "Almost friday sir.";

    private final GatewayConfigStore store;
    private final BootstrapService bootstrapService;
    private final GatewayDaemonService daemonService;

    @Inject
    public GatewayStartupFlow(
            GatewayConfigStore store,
            BootstrapService bootstrapService,
            GatewayDaemonService daemonService) {
        this.store = store;
        this.bootstrapService = bootstrapService;
        this.daemonService = daemonService;
    }

    public Integer run(GatewayLaunchOptions options) throws Exception {
        GatewayAppConfig config = store.exists(options.home) ? store.load(options.home) : bootstrap(options);
        System.out.println(START_BANNER_COLOR + START_BANNER + ANSI_RESET);
        daemonService.run(config);
        return 0;
    }

    private GatewayAppConfig bootstrap(GatewayLaunchOptions options) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String appId = valueOrPrompt(options.feishuAppId, reader, "APPID");
        String appSecret = valueOrPrompt(options.feishuAppSecret, reader, "APPSecret");
        Path workspaceRoot = options.workspace != null ? options.workspace : Path.of(promptForValue(reader, "WorkSpace"));

        GatewayAppConfig config = GatewayAppConfig.builder()
                .codexCommand(options.codexCommand)
                .workspaceRoot(workspaceRoot)
                .recordRoot(options.home.resolve(".lg4c/records"))
                .feishuAppId(appId)
                .feishuAppSecret(appSecret)
                .feishuBaseUrl(options.feishuBaseUrl)
                .feishuWebsocketUrl(options.feishuWebsocketUrl)
                .feishuReplyUrl(options.feishuReplyUrl)
                .allowedUsers(options.allowedUsers)
                .allowedChats(options.allowedChats)
                .loggingLevel(options.loggingLevel)
                .build();

        bootstrapService.initializeWorkspace(config);
        bootstrapService.save(options.home, config);
        System.out.printf("Bootstrap complete: %s%n", store.configFile(options.home));
        System.out.println(bootstrapService.feishuBootstrapSummary());
        bootstrapService.warningsFor(config).forEach(System.out::println);
        return config;
    }

    private String valueOrPrompt(String currentValue, BufferedReader reader, String label) throws IOException {
        return hasText(currentValue) ? currentValue : promptForValue(reader, label);
    }

    private String promptForValue(BufferedReader reader, String label) throws IOException {
        while (true) {
            System.out.printf("%s: ", label);
            System.out.flush();
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalStateException("Missing required input for " + label);
            }
            if (hasText(line)) {
                return line.trim();
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
