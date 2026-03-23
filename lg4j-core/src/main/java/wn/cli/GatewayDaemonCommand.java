package wn.cli;

import java.util.concurrent.Callable;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Dependent
@Command(name = "daemon", description = "Run lg4c as a local daemon")
public class GatewayDaemonCommand implements Callable<Integer> {
    @Inject
    GatewayStartupFlow startupFlow;

    @Mixin
    GatewayLaunchOptions options = new GatewayLaunchOptions();

    @Override
    public Integer call() throws Exception {
        try {
            return startupFlow.run(options);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return 2;
        }
    }
}
