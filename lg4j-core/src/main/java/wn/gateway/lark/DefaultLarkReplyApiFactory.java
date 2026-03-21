package wn.gateway.lark;

import java.net.URI;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import wn.gateway.config.GatewayAppConfig;

@ApplicationScoped
public class DefaultLarkReplyApiFactory implements LarkReplyApiFactory {

    @Override
    public LarkReplyApi create(GatewayAppConfig config) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(config.feishuReplyUrl()))
                .build(LarkReplyApi.class);
    }
}
