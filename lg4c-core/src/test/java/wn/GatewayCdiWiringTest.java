package wn;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Unremovable;
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

    @Test
    void picocliSubcommandsAreMarkedUnremovableForNative() {
        assertTrue(
                GatewayDaemonCommand.class.isAnnotationPresent(Unremovable.class),
                "GatewayDaemonCommand must survive native bean removal");
        assertTrue(
                GatewayDoctorCommand.class.isAnnotationPresent(Unremovable.class),
                "GatewayDoctorCommand must survive native bean removal");
    }
}
