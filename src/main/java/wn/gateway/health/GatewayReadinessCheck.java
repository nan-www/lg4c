package wn.gateway.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import wn.gateway.runtime.GatewayRuntimeState;

@Readiness
public class GatewayReadinessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return GatewayRuntimeState.ready()
                ? HealthCheckResponse.up("gateway-ready")
                : HealthCheckResponse.down("gateway-ready");
    }
}
