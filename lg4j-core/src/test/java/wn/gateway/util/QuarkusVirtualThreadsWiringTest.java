package wn.gateway.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.virtual.threads.VirtualThreads;
import jakarta.inject.Inject;

@QuarkusTest
class QuarkusVirtualThreadsWiringTest {

    @Inject
    VTFactory vtFactory;

    @Inject
    @VirtualThreads
    ExecutorService virtualThreadsExecutor;

    @Test
    void quarkusCanInjectVirtualThreadExecutorAndFactoryUsesIt() throws Exception {
        assertNotNull(vtFactory);
        assertNotNull(virtualThreadsExecutor);

        ExecutorService executor = vtFactory.newVirtualThreadExecutor("quarkus-vt");
        assertSame(virtualThreadsExecutor, executor);
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(
                () -> Thread.currentThread().isVirtual(),
                executor);

        assertTrue(future.get(2, TimeUnit.SECONDS));
    }
}
