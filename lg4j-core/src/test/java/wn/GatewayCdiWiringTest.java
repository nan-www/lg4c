package wn;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import wn.cli.GatewayDaemonCommand;
import wn.cli.GatewayDoctorCommand;
import wn.cli.GatewayStartupFlow;
import wn.gateway.bootstrap.BootstrapService;
import wn.gateway.config.GatewayConfigStore;
import wn.gateway.runtime.GatewayDaemonService;

class GatewayCdiWiringTest {

    @Test
    void gatewayBeansRemainConstructable() {
        GatewayRootCommand rootCommand = new GatewayRootCommand();
        GatewayDaemonCommand daemonCommand = new GatewayDaemonCommand();
        GatewayDoctorCommand doctorCommand = new GatewayDoctorCommand();
        GatewayConfigStore configStore = new GatewayConfigStore();
        BootstrapService bootstrapService = new BootstrapService(configStore);
        GatewayDaemonService daemonService = new GatewayDaemonService();
        GatewayStartupFlow startupFlow = new GatewayStartupFlow(configStore, bootstrapService, daemonService);

        assertNotNull(rootCommand);
        assertNotNull(daemonCommand);
        assertNotNull(doctorCommand);
        assertNotNull(configStore);
        assertNotNull(bootstrapService);
        assertNotNull(daemonService);
        assertNotNull(startupFlow);
    }
}
