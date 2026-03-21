package wn.gateway.session;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import wn.gateway.domain.ConversationKey;
import wn.gateway.util.VTFactory;

public class SerialConversationDispatcher implements AutoCloseable {
    private final ExecutorService executor;
    private final Map<String, CompletableFuture<Void>> tails = new ConcurrentHashMap<>();

    public SerialConversationDispatcher() {
        this(new VTFactory().newVirtualThreadExecutor("conversation-dispatch"));
    }

    public SerialConversationDispatcher(ExecutorService executor) {
        this.executor = executor;
    }

    public CompletableFuture<Void> dispatch(ConversationKey key, Runnable task) {
        CompletableFuture<Void> next = tails.compute(key.value(), (ignored, tail) -> {
            CompletableFuture<Void> previous = tail == null
                    ? CompletableFuture.completedFuture(null)
                    : tail.handle((result, failure) -> null);
            return previous.thenRunAsync(task, executor);
        });
        next.whenComplete((result, failure) -> tails.remove(key.value(), next));
        return next;
    }

    public void close(Duration timeout) throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
