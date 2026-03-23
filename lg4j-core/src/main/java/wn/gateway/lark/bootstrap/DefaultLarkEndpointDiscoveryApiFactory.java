package wn.gateway.lark.bootstrap;

import java.net.URI;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import wn.gateway.config.GatewayAppConfig;

@ApplicationScoped
public class DefaultLarkEndpointDiscoveryApiFactory {

    public LarkEndpointDiscoveryApi create(GatewayAppConfig config) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(config.larkEnvironment().baseUrl()))
                .build(LarkEndpointDiscoveryApi.class);
    }
}
