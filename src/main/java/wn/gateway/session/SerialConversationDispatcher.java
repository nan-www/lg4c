package wn.gateway.session;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import wn.gateway.domain.ConversationKey;

public class SerialConversationDispatcher implements AutoCloseable {
    private final ExecutorService executor;
    private final boolean managesExecutor;
    private final Map<String, CompletableFuture<Void>> tails = new ConcurrentHashMap<>();

    public SerialConversationDispatcher() {
        this(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("lg4c-conversation-dispatch-", 0).factory()), true);
    }

    public SerialConversationDispatcher(ExecutorService executor) {
        this(executor, true);
    }

    public SerialConversationDispatcher(ExecutorService executor, boolean managesExecutor) {
        this.executor = executor;
        this.managesExecutor = managesExecutor;
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
        if (managesExecutor) {
            executor.shutdown();
            executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return;
        }
        awaitOutstandingTasks(timeout);
    }

    @Override
    public void close() {
        if (managesExecutor) {
            executor.shutdownNow();
        }
    }

    private void awaitOutstandingTasks(Duration timeout) throws InterruptedException {
        CompletableFuture<?>[] inFlight = tails.values().toArray(CompletableFuture[]::new);
        if (inFlight.length == 0) {
            return;
        }
        try {
            CompletableFuture.allOf(inFlight).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ignored) {
            // best effort wait only
        } catch (java.util.concurrent.ExecutionException ignored) {
            // individual task failures are handled by the caller chain
        }
    }
}
