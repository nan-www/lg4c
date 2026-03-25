package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.lark.oapi.core.httpclient.IHttpTransport;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.V1;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.lark.oapi.service.im.v1.resource.Message;
import com.lark.oapi.ws.Client;

class QuarkusLarkGatewayClientVirtualThreadTest extends LarkTestSupport {
    private static final IHttpTransport NOOP_HTTP_TRANSPORT = rawRequest -> {
        throw new AssertionError("messageClient transport should not be used in this test");
    };

    @Test
    void sendReplyDelegatesOnCallerThreadAndReturnsCompletedFuture() throws Exception {
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> apiThread = new AtomicReference<>();
        com.lark.oapi.Client messageClient = mock(com.lark.oapi.Client.class);
        ImService imService = mock(ImService.class);
        V1 v1 = mock(V1.class);
        Message messageApi = mock(Message.class);
        CreateMessageResp response = mock(CreateMessageResp.class);
        IHttpTransport customHttpTransport = mock(IHttpTransport.class);

        when(response.success()).thenReturn(true);
        when(messageClient.im()).thenReturn(imService);
        when(imService.v1()).thenReturn(v1);
        when(v1.message()).thenReturn(messageApi);
        when(messageApi.create(any(CreateMessageReq.class))).thenAnswer(invocation -> {
            apiThread.set(Thread.currentThread());
            return response;
        });
        try (MockedConstruction<com.lark.oapi.Client.Builder> ignored = mockConstruction(
                com.lark.oapi.Client.Builder.class,
                (builder, context) -> {
                    when(builder.openBaseUrl(eq(config().larkEnvironment().baseUrl()))).thenReturn(builder);
                    when(builder.httpTransport(eq(customHttpTransport))).thenReturn(builder);
                    when(builder.build()).thenReturn(messageClient);
                })) {
            QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(new ObjectMapper(), customHttpTransport);
            client.setConfig(config());

            CompletableFuture<Void> returned = client.sendReply(message(), "ok");

            assertSame(callerThread, apiThread.get());
            assertTrue(returned.isDone());
            returned.join();
        }
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
            QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(new ObjectMapper(), NOOP_HTTP_TRANSPORT);
            client.setConfig(config());
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
