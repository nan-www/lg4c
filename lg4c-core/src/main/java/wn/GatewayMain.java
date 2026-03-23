package wn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

@QuarkusMain
@ApplicationScoped
public class GatewayMain implements QuarkusApplication {
    @Inject
    GatewayRootCommand rootCommand;

    @Inject
    CommandLine.IFactory factory;

    @Override
    public int run(String... args) {
        CommandLine commandLine = new CommandLine(rootCommand, factory);
        return commandLine.execute(args);
    }
}
