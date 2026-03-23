package wn.gateway.lark.auth;

import java.net.URI;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import wn.gateway.config.GatewayAppConfig;

@ApplicationScoped
public class DefaultLarkTenantAccessTokenApiFactory implements LarkTenantAccessTokenApiFactory {

    @Override
    public LarkTenantAccessTokenApi create(GatewayAppConfig config) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(config.larkEnvironment().baseUrl()))
                .build(LarkTenantAccessTokenApi.class);
    }
}
