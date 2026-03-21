package wn.gateway.feishu;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import wn.gateway.domain.InboundMessage;

public interface FeishuGatewayClient extends Closeable {

    void start(Consumer<InboundMessage> messageConsumer);

    CompletableFuture<Void> sendReply(InboundMessage message, String answer);

    boolean isConnected();
}
