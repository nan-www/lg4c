package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.core.httpclient.IHttpTransport;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.ws.Client;

class LarkNativeMetadataCaptureTest extends LarkTestSupport {
    private static final IHttpTransport NOOP_HTTP_TRANSPORT = rawRequest -> {
        throw new AssertionError("messageClient transport should not be used in this test");
    };

    @Test
    void eventDispatcherParsesMessageReceiveV1Payload() throws Throwable {
        AtomicReference<P2MessageReceiveV1> received = new AtomicReference<>();
        EventDispatcher dispatcher = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                        received.set(event);
                    }
                })
                .build();

        dispatcher.doWithoutValidation(sampleEventPayload(new ObjectMapper()));

        assertNotNull(received.get());
        assertEquals("ou_event", received.get().getEvent().getSender().getSenderId().getOpenId());
        assertEquals("oc_event", received.get().getEvent().getMessage().getChatId());
        assertEquals("om_event", received.get().getEvent().getMessage().getMessageId());
        assertEquals("{\"text\":\"hello from lark\"}", received.get().getEvent().getMessage().getContent());
    }

    @Test
    void closeExecutesOfficialClientShutdownPathWithoutStartingNetworkLoop() throws Exception {
        EventDispatcher dispatcher = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                    }
                })
                .build();
        Client sdkClient = new Client.Builder(config().feishuAppId(), config().feishuAppSecret())
                .domain(config().larkEnvironment().baseUrl())
                .eventHandler(dispatcher)
                .build();
        QuarkusLarkGatewayClient client = new QuarkusLarkGatewayClient(new ObjectMapper(), NOOP_HTTP_TRANSPORT);
        client.setConfig(config());
        setField(client, "sdkClient", sdkClient);
        setField(client, "started", Boolean.TRUE);

        client.close();

        assertFalse(client.isConnected());
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
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
