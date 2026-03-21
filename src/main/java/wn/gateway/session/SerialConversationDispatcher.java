package wn.gateway.session;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import wn.gateway.domain.ConversationKey;

public class SerialConversationDispatcher implements AutoCloseable {
    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();

    public CompletableFuture<Void> dispatch(ConversationKey key, Runnable task) {
        ExecutorService executor = executors.computeIfAbsent(key.value(), ignored -> Executors.newSingleThreadExecutor());
        return CompletableFuture.runAsync(task, executor);
    }

    public void close(Duration timeout) throws InterruptedException {
        for (ExecutorService executor : executors.values()) {
            executor.shutdown();
        }
        for (ExecutorService executor : executors.values()) {
            executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void close() {
        executors.values().forEach(ExecutorService::shutdownNow);
    }
}
