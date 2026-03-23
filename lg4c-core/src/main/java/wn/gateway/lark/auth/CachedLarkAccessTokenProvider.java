package wn.gateway.lark.auth;

import java.time.Clock;
import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.lark.auth.dto.LarkTenantAccessTokenRequest;
import wn.gateway.lark.auth.dto.LarkTenantAccessTokenResponse;

@ApplicationScoped
public class CachedLarkAccessTokenProvider {
    private final LarkTenantAccessTokenApiFactory apiFactory;
    private final Clock clock;
    private final int refreshSkewSeconds;

    private volatile CachedToken cachedToken;

    @Inject
    public CachedLarkAccessTokenProvider(LarkTenantAccessTokenApiFactory apiFactory) {
        this(apiFactory, Clock.systemUTC(), 60);
    }

    public CachedLarkAccessTokenProvider(
            LarkTenantAccessTokenApiFactory apiFactory,
            Clock clock,
            int refreshSkewSeconds) {
        this.apiFactory = apiFactory;
        this.clock = clock;
        this.refreshSkewSeconds = refreshSkewSeconds;
    }

    public String getTenantAccessToken(GatewayAppConfig config) {
        CachedToken snapshot = cachedToken;
        Instant now = clock.instant();
        if (snapshot != null && snapshot.canUse(now.plusSeconds(refreshSkewSeconds))) {
            return snapshot.token();
        }
        synchronized (this) {
            snapshot = cachedToken;
            now = clock.instant();
            if (snapshot != null && snapshot.canUse(now.plusSeconds(refreshSkewSeconds))) {
                return snapshot.token();
            }
            LarkTenantAccessTokenResponse response = apiFactory.create(config)
                    .fetch(new LarkTenantAccessTokenRequest(config.feishuAppId(), config.feishuAppSecret()));
            if (response == null || response.tenantAccessToken() == null || response.tenantAccessToken().isBlank()) {
                throw new IllegalStateException("failed to fetch feishu tenant access token");
            }
            if (response.code() != 0) {
                throw new IllegalStateException("failed to fetch feishu tenant access token: " + response.msg());
            }
            cachedToken = new CachedToken(
                    response.tenantAccessToken(),
                    now.plusSeconds(Math.max(1, response.expireSeconds())));
            return cachedToken.token();
        }
    }

    private record CachedToken(String token, Instant expiresAt) {
        public boolean canUse(Instant expiresAt){
            return this.expiresAt.isAfter(expiresAt);
        }
    }
}
