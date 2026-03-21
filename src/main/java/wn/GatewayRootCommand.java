package wn;

import jakarta.enterprise.context.Dependent;
import picocli.CommandLine.Command;

@Dependent
@Command(
        name = "lg4c",
        mixinStandardHelpOptions = true,
        subcommands = { GatewayDaemonCommand.class, GatewayDoctorCommand.class })
public class GatewayRootCommand implements Runnable {

    @Override
    public void run() {
        // Picocli prints usage when no subcommand is provided.
    }
}
