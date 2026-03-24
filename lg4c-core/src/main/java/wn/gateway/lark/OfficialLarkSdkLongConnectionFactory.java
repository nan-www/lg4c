package wn.gateway.lark;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lark.oapi.core.request.EventReq;
import com.lark.oapi.event.CustomEventHandler;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.ws.Client;

import jakarta.enterprise.context.ApplicationScoped;
import wn.gateway.config.GatewayAppConfig;

@ApplicationScoped
public class OfficialLarkSdkLongConnectionFactory {
    private static final Logger log = LoggerFactory.getLogger(OfficialLarkSdkLongConnectionFactory.class);
    private static final String IM_MESSAGE_RECEIVE_V1 = "im.message.receive_v1";

    public LarkSdkLongConnection create(GatewayAppConfig config, Consumer<byte[]> rawEventConsumer) {
        if (config.feishuWebsocketUrl() != null) {
            log.warn("feishu websocket override is ignored when using official lark sdk");
        }

        EventDispatcher dispatcher = EventDispatcher.newBuilder("", "")
                .onCustomizedEvent(IM_MESSAGE_RECEIVE_V1, new CustomEventHandler() {
                    @Override
                    public void handle(EventReq event) {
                        rawEventConsumer.accept(event.getBody());
                    }
                })
                .build();

        Client client = new Client.Builder(config.feishuAppId(), config.feishuAppSecret())
                .domain(config.larkEnvironment().baseUrl())
                .eventHandler(dispatcher)
                .build();
        return new OfficialLarkSdkLongConnection(client);
    }

    static final class OfficialLarkSdkLongConnection implements LarkSdkLongConnection {
        private static final String FIELD_CONN = "conn";
        private static final String FIELD_EXECUTOR = "executor";
        private static final String FIELD_AUTO_RECONNECT = "autoReconnect";
        private static final String METHOD_DISCONNECT = "disconnect";

        private final Client client;
        private volatile boolean started;

        private OfficialLarkSdkLongConnection(Client client) {
            this.client = client;
        }

        @Override
        public void start() {
            client.start();
            started = true;
        }

        @Override
        public boolean isConnected() {
            Object connection = readField(FIELD_CONN);
            return connection != null || started;
        }

        @Override
        public void close() throws IOException {
            started = false;
            try {
                writeField(FIELD_AUTO_RECONNECT, Boolean.FALSE);
                invokeDisconnect();
                shutdownExecutor();
            } catch (ReflectiveOperationException e) {
                throw new IOException("failed to close official lark sdk client", e);
            }
        }

        private void invokeDisconnect() throws ReflectiveOperationException {
            Method method = Client.class.getDeclaredMethod(METHOD_DISCONNECT);
            method.setAccessible(true);
            method.invoke(client);
        }

        private void shutdownExecutor() throws ReflectiveOperationException {
            Object value = readField(FIELD_EXECUTOR);
            if (value instanceof ExecutorService executorService) {
                executorService.shutdownNow();
            }
        }

        private void writeField(String name, Object value) throws ReflectiveOperationException {
            Field field = Client.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(client, value);
        }

        private Object readField(String name) {
            try {
                Field field = Client.class.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(client);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
    }
}
