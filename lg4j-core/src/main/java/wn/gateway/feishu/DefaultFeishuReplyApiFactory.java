package wn.gateway.feishu;

import java.net.URI;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import wn.gateway.config.GatewayAppConfig;

@ApplicationScoped
public class DefaultFeishuReplyApiFactory implements FeishuReplyApiFactory {

    @Override
    public FeishuReplyApi create(GatewayAppConfig config) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(config.feishuReplyUrl()))
                .build(FeishuReplyApi.class);
    }
}
