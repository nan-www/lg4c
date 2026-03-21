package wn.gateway.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import wn.gateway.runtime.GatewayRuntimeState;

@Liveness
public class GatewayLivenessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return GatewayRuntimeState.live()
                ? HealthCheckResponse.up("gateway-live")
                : HealthCheckResponse.down("gateway-live");
    }
}
