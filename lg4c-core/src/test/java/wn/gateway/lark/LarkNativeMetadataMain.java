package wn.gateway.lark;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.ws.Client;

import wn.gateway.config.GatewayAppConfig;

public final class LarkNativeMetadataMain {
    private LarkNativeMetadataMain() {
    }

    public static void main(String[] args) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        captureOfficialEventParsing(mapper);
        captureClientShutdownPath(mapper);
    }

    private static void captureOfficialEventParsing(ObjectMapper mapper) throws Throwable {
        AtomicReference<P2MessageReceiveV1> received = new AtomicReference<>();
        EventDispatcher dispatcher = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                        received.set(event);
                    }
                })
                .build();

        dispatcher.doWithoutValidation(sampleEventPayload(mapper));

        if (received.get() == null) {
            throw new IllegalStateException("official event dispatcher did not parse im.message.receive_v1");
        }
        if (!"ou_event".equals(received.get().getEvent().getSender().getSenderId().getOpenId())) {
            throw new IllegalStateException("unexpected sender id in parsed event");
        }
    }

    private static void captureClientShutdownPath(ObjectMapper mapper) throws Exception {
        EventDispatcher dispatcher = EventDispatcher.newBuilder("", "")
                .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                    @Override
                    public void handle(P2MessageReceiveV1 event) {
                    }
                })
                .build();
        Client sdkClient = new Client.Builder("app-id", "app-secret")
                .domain("https://open.feishu.cn")
                .eventHandler(dispatcher)
                .build();
        QuarkusLarkGatewayClient gatewayClient = new QuarkusLarkGatewayClient(mapper);
        gatewayClient.setConfig(sampleConfig());
        setField(gatewayClient, "sdkClient", sdkClient);
        setField(gatewayClient, "started", Boolean.TRUE);
        gatewayClient.close();
        if (gatewayClient.isConnected()) {
            throw new IllegalStateException("gateway client should report disconnected after close");
        }
    }

    private static GatewayAppConfig sampleConfig() {
        return GatewayAppConfig.builder()
                .codexCommand(List.of("codex"))
                .workspaceRoot(Path.of("/tmp/workspace"))
                .recordRoot(Path.of("/tmp/.lg4c/records"))
                .agentTemplate("agent")
                .feishuAppId("app-id")
                .feishuAppSecret("app-secret")
                .allowedUsers(List.of("ou_1"))
                .allowedChats(List.of("oc_1"))
                .loggingLevel("INFO")
                .build();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static byte[] sampleEventPayload(ObjectMapper mapper) throws Exception {
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
