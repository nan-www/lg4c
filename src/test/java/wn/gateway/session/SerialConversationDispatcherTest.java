package wn.gateway.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import wn.gateway.domain.ConversationKey;

class SerialConversationDispatcherTest {

    @Test
    void dispatchesMessagesSeriallyWithinSameConversationAndConcurrentlyAcrossDifferentOnes() throws Exception {
        SerialConversationDispatcher dispatcher = new SerialConversationDispatcher(
                Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("dispatcher-test-", 0).factory()));
        List<String> executionOrder = new CopyOnWriteArrayList<>();
        ConversationKey sameKey = new ConversationKey("u1", "c1");
        ConversationKey otherKey = new ConversationKey("u2", "c2");
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch otherDone = new CountDownLatch(1);

        CompletableFuture<Void> first = dispatcher.dispatch(sameKey, () -> {
            executionOrder.add("first-start");
            firstStarted.countDown();
            await(releaseFirst);
            executionOrder.add("first-end");
        });

        assertTrue(firstStarted.await(2, TimeUnit.SECONDS));

        CompletableFuture<Void> second = dispatcher.dispatch(sameKey, () -> executionOrder.add("second"));
        CompletableFuture<Void> other = dispatcher.dispatch(otherKey, () -> {
            executionOrder.add("other");
            otherDone.countDown();
        });

        assertTrue(otherDone.await(2, TimeUnit.SECONDS));
        releaseFirst.countDown();

        CompletableFuture.allOf(first, second, other).get(2, TimeUnit.SECONDS);
        assertEquals(List.of("first-start", "other", "first-end", "second"), executionOrder);

        dispatcher.close(Duration.ofSeconds(1));
    }

    @Test
    void dispatchRunsTasksOnVirtualThreads() throws Exception {
        SerialConversationDispatcher dispatcher = new SerialConversationDispatcher(
                Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("dispatcher-vt-check-", 0).factory()));
        AtomicBoolean ranOnVirtualThread = new AtomicBoolean();

        dispatcher.dispatch(new ConversationKey("u1", "c1"), () -> ranOnVirtualThread.set(Thread.currentThread().isVirtual()))
                .get(2, TimeUnit.SECONDS);

        assertTrue(ranOnVirtualThread.get());
        dispatcher.close(Duration.ofSeconds(1));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
