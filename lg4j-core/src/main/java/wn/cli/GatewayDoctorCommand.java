package wn.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import jakarta.enterprise.context.Dependent;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Dependent
@Command(name = "doctor", description = "Check local lg4c dependencies")
public class GatewayDoctorCommand implements Callable<Integer> {

    @Option(names = "--home", defaultValue = "${sys:user.home}")
    Path home;

    @Override
    public Integer call() {
        Path configFile = home.resolve(".lg4c/config/application.yml");
        boolean configExists = Files.exists(configFile);
        System.out.printf("config: %s%n", configExists ? configFile : "missing");
        System.out.printf("codex: %s%n", resolveCodex());
        return 0;
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
