package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
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

class QuarkusLarkGatewayClientTest extends LarkTestSupport {
    private static final IHttpTransport NOOP_HTTP_TRANSPORT = rawRequest -> {
        throw new AssertionError("messageClient transport should not be used in this test");
    };

    @Test
    void startBuildsOfficialSdkClientAndBridgesTypedEventPayload() throws Exception {
        AtomicReference<wn.gateway.domain.InboundMessage> received = new AtomicReference<>();
        AtomicReference<EventDispatcher> dispatcher = new AtomicReference<>();
        Client sdkClient = mock(Client.class);

        try (MockedConstruction<Client.Builder> builderConstruction = mockConstruction(
                Client.Builder.class,
                (builder, context) -> {
                    assertEquals("app-id", context.arguments().get(0));
                    assertEquals("app-secret", context.arguments().get(1));
                    when(builder.eventHandler(any(EventDispatcher.class))).thenAnswer(invocation -> {
                        dispatcher.set(invocation.getArgument(0, EventDispatcher.class));
                        return builder;
                    });
                    when(builder.domain(eq(config().larkEnvironment().baseUrl()))).thenReturn(builder);
                    when(builder.build()).thenReturn(sdkClient);
                })) {
            QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(new ObjectMapper(), NOOP_HTTP_TRANSPORT);
            client.setConfig(config());

            client.start(received::set);

            assertEquals(1, builderConstruction.constructed().size());
            assertNotNull(dispatcher.get());
            verify(sdkClient).start();

            try {
                dispatcher.get().doWithoutValidation(sampleEventPayload(new ObjectMapper()));
            } catch (Throwable throwable) {
                throw new AssertionError("dispatcher should accept official sdk payloads", throwable);
            }

            assertNotNull(received.get());
            assertEquals("ou_event", received.get().userId());
            assertEquals("oc_event", received.get().chatId());
            assertEquals("om_event", received.get().messageId());
            assertEquals("hello from lark", received.get().text());
        }
    }

    @Test
    void sendReplyUsesOfficialSdkCreateMessageApi() throws Exception {
        AtomicReference<CreateMessageReq> request = new AtomicReference<>();
        AtomicReference<IHttpTransport> httpTransport = new AtomicReference<>();
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
            request.set(invocation.getArgument(0, CreateMessageReq.class));
            return response;
        });

        try (MockedConstruction<com.lark.oapi.Client.Builder> ignored = mockConstruction(
                com.lark.oapi.Client.Builder.class,
                (builder, context) -> {
                    assertEquals("app-id", context.arguments().get(0));
                    assertEquals("app-secret", context.arguments().get(1));
                    when(builder.openBaseUrl(eq(config().larkEnvironment().baseUrl()))).thenReturn(builder);
                    when(builder.httpTransport(eq(customHttpTransport))).thenAnswer(invocation -> {
                        httpTransport.set(invocation.getArgument(0, IHttpTransport.class));
                        return builder;
                    });
                    when(builder.build()).thenReturn(messageClient);
                })) {
            QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(new ObjectMapper(), customHttpTransport);
            client.setConfig(config());

            CompletableFuture<Void> returned = client.sendReply(message(), "ok");

            assertTrue(returned.isDone());
            returned.join();
            assertNotNull(httpTransport.get());
            assertEquals("chat_id", request.get().getReceiveIdType());
            assertEquals("oc_1", request.get().getCreateMessageReqBody().getReceiveId());
            assertEquals("text", request.get().getCreateMessageReqBody().getMsgType());
            assertEquals("{\"text\":\"ok\"}", request.get().getCreateMessageReqBody().getContent());
        }
    }

    @Test
    void closeBeforeStartIsSafe() throws Exception {
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(new ObjectMapper(), NOOP_HTTP_TRANSPORT);

        client.close();
    }

    private byte[] sampleEventPayload(ObjectMapper mapper) throws Exception {
        var root = mapper.createObjectNode();
        root.put("schema", "2.0");
        root.putObject("header")
                .put("event_type", "im.message.receive_v1")
                .put("create_time", "1710000000000");
        var event = root.putObject("event");
        event.putObject("sender")
                .putObject("sender_id")
                .put("open_id", "ou_event");
        event.putObject("message")
                .put("chat_id", "oc_event")
                .put("message_id", "om_event")
                .put("content", "{\"text\":\"hello from lark\"}");
        return mapper.writeValueAsString(root).getBytes(StandardCharsets.UTF_8);
    }
}
