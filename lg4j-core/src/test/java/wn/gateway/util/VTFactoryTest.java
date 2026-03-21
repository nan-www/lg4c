package wn.gateway.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class VTFactoryTest {

    @Test
    void createsExecutorThatRunsTasksOnVirtualThreads() throws Exception {
        VTFactory factory = new VTFactory(
                Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("vt-factory-test-", 0).factory()));
        try (ExecutorService executor = factory.newVirtualThreadExecutor("vt-factory-test")) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(
                    () -> Thread.currentThread().isVirtual(),
                    executor);

            assertTrue(future.get(2, TimeUnit.SECONDS));
        }
    }
}
