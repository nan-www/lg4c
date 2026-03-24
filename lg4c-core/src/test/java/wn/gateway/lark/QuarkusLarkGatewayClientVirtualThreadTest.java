package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.lark.auth.CachedLarkAccessTokenProvider;

class QuarkusLarkGatewayClientVirtualThreadTest extends LarkTestSupport {

    @Test
    void sendReplyDelegatesOnCallerThreadAndReturnsUnderlyingFuture() {
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> apiThread = new AtomicReference<>();
        CompletableFuture<Void> replyFuture = new CompletableFuture<>();
        LarkReplyApi replyApi = (authorization, messageId, request) -> {
            apiThread.set(Thread.currentThread());
            return replyFuture;
        };
        CachedLarkAccessTokenProvider tokenProvider = mock(CachedLarkAccessTokenProvider.class);
        when(tokenProvider.getTenantAccessToken(any())).thenReturn("tenant-token");
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                replyApi,
                mock(OfficialLarkSdkLongConnectionFactory.class),
                tokenProvider);

        CompletableFuture<Void> returned = client.sendReply(message(), "ok");

        assertSame(callerThread, apiThread.get());
        assertSame(replyFuture, returned);
    }

    @Test
    void sdkConnectionStartRunsOnCallerThread() {
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> createThread = new AtomicReference<>();
        AtomicReference<Thread> startThread = new AtomicReference<>();

        OfficialLarkSdkLongConnectionFactory sdkFactory = mock(OfficialLarkSdkLongConnectionFactory.class);
        when(sdkFactory.create(any(), any())).thenAnswer(invocation -> {
            createThread.set(Thread.currentThread());
            return new LarkSdkLongConnection() {
                @Override
                public void start() {
                    startThread.set(Thread.currentThread());
                }

                @Override
                public boolean isConnected() {
                    return true;
                }

                @Override
                public void close() {
                }
            };
        });

        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                config(),
                new ObjectMapper(),
                (authorization, messageId, request) -> CompletableFuture.completedFuture(null),
                sdkFactory,
                mock(CachedLarkAccessTokenProvider.class));

        client.start(message -> {
        });

        assertSame(callerThread, createThread.get());
        assertSame(callerThread, startThread.get());
    }
}
