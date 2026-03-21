package wn.gateway.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class QuarkusVirtualThreadsWiringTest {

    @Test
    void factoryStaticMethodCreatesNamedVirtualThreads() throws Exception {
        try (ExecutorService executor = VTFactory.newVirtualThreadExecutor("quarkus-vt")) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    () -> {
                        assertTrue(Thread.currentThread().isVirtual());
                        return Thread.currentThread().getName();
                    },
                    executor);

            assertTrue(future.get(2, TimeUnit.SECONDS).startsWith("quarkus-vt"));
        }
    }
}
