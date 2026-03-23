package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;
import wn.gateway.lark.auth.LarkTenantAccessTokenApi;
import wn.gateway.lark.auth.LarkTenantAccessTokenResponse;

class CachedLarkAccessTokenProviderTest extends LarkTestSupport {

    @Test
    void cachesTokenUntilRefreshThreshold() {
        AtomicInteger calls = new AtomicInteger();
        LarkTenantAccessTokenApi api = request -> {
            int idx = calls.incrementAndGet();
            return new LarkTenantAccessTokenResponse(0, "ok", "token-" + idx, 7200);
        };
        MutableClock clock = new MutableClock(Instant.parse("2026-03-23T00:00:00Z"));
        CachedLarkAccessTokenProvider provider = CachedLarkAccessTokenProvider.forTest(api, clock, 60);

        String token1 = provider.getTenantAccessToken(config());
        String token2 = provider.getTenantAccessToken(config());
        clock.advanceSeconds(7200 - 59);
        String token3 = provider.getTenantAccessToken(config());

        assertEquals("token-1", token1);
        assertEquals("token-1", token2);
        assertEquals("token-2", token3);
        assertEquals(2, calls.get());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}
