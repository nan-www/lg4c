package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.ws.Client;

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
        Client sdkClient = mock(Client.class);
        try (MockedConstruction<Client.Builder> builderConstruction = mockConstruction(
                Client.Builder.class,
                (builder, context) -> {
                    createThread.set(Thread.currentThread());
                    when(builder.eventHandler(any(EventDispatcher.class))).thenReturn(builder);
                    when(builder.domain(eq(config().larkEnvironment().baseUrl()))).thenReturn(builder);
                    when(builder.build()).thenReturn(sdkClient);
                })) {
            QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(
                    config(),
                    new ObjectMapper(),
                    (authorization, messageId, request) -> CompletableFuture.completedFuture(null),
                    mock(CachedLarkAccessTokenProvider.class));
            doAnswer(invocation -> {
                startThread.set(Thread.currentThread());
                return null;
            }).when(sdkClient).start();

            client.start(message -> {
            });

            assertEquals(1, builderConstruction.constructed().size());
        }

        assertSame(callerThread, createThread.get());
        assertSame(callerThread, startThread.get());
    }
}
