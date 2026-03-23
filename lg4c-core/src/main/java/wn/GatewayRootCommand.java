package wn;

import java.util.concurrent.Callable;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import wn.cli.GatewayDaemonCommand;
import wn.cli.GatewayDoctorCommand;
import wn.cli.GatewayLaunchOptions;
import wn.cli.GatewayStartupFlow;

@Dependent
@Command(
        name = "lg4c",
        mixinStandardHelpOptions = true,
        subcommands = { GatewayDaemonCommand.class, GatewayDoctorCommand.class })
public class GatewayRootCommand implements Callable<Integer> {
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
