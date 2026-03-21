package wn;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import wn.gateway.bootstrap.BootstrapService;
import wn.gateway.config.GatewayConfigStore;
import wn.gateway.runtime.GatewayDaemonService;

@QuarkusTest
class GatewayCdiWiringTest {

    @Inject
    GatewayRootCommand rootCommand;

    @Inject
    GatewayDaemonCommand daemonCommand;

    @Inject
    GatewayDoctorCommand doctorCommand;

    @Inject
    GatewayConfigStore configStore;

    @Inject
    BootstrapService bootstrapService;

    @Inject
    GatewayDaemonService daemonService;

    @Test
    void quarkusCanInjectGatewayBeans() {
        assertNotNull(rootCommand);
        assertNotNull(daemonCommand);
        assertNotNull(doctorCommand);
        assertNotNull(configStore);
        assertNotNull(bootstrapService);
        assertNotNull(daemonService);
    }
}
