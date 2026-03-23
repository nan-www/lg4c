package wn.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import wn.gateway.bootstrap.BootstrapService;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.config.GatewayConfigStore;

@Dependent
@Command(name = "doctor", description = "Check local lg4c dependencies")
public class GatewayDoctorCommand implements Callable<Integer> {

    @Inject
    GatewayConfigStore store;

    @Inject
    BootstrapService bootstrapService;

    @Option(names = "--home", defaultValue = "${sys:user.home}")
    Path home;

    @Override
    public Integer call() {
        Path configFile = store.configFile(home);
        boolean configExists = store.exists(home);
        System.out.printf("config: %s%n", configExists ? configFile : "missing");
        if (configExists) {
            printFeishuStatus();
        }
        System.out.printf("codex: %s%n", resolveCodex());
        return 0;
    }

    private void printFeishuStatus() {
        try {
            GatewayAppConfig config = store.load(home);
            System.out.println("feishu: configured via app credentials");
            System.out.println(bootstrapService.feishuBootstrapSummary());
            bootstrapService.warningsFor(config).forEach(System.out::println);
        } catch (Exception e) {
            System.out.printf("feishu: invalid config (%s)%n", e.getMessage());
        }
    }

    private String resolveCodex() {
        String path = System.getenv().getOrDefault("PATH", "");
        for (String entry : path.split(":")) {
            Path candidate = Path.of(entry).resolve("codex");
            if (Files.isExecutable(candidate)) {
                return candidate.toString();
            }
        }
        return "missing";
    }
}
