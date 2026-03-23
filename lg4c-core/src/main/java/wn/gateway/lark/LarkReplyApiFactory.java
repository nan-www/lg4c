package wn.gateway.lark;

import java.net.URI;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import wn.gateway.config.GatewayAppConfig;

@ApplicationScoped
public class LarkReplyApiFactory {

    public LarkReplyApi create(GatewayAppConfig config) {
        String baseUri = config.feishuReplyUrl() != null
                ? config.feishuReplyUrl()
                : config.larkEnvironment().baseUrl();
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUri))
                .build(LarkReplyApi.class);
    }
}
