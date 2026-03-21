package wn.gateway.util;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.virtual.threads.VirtualThreads;

@ApplicationScoped
public class VTFactory {
    @Inject
    @VirtualThreads
    ExecutorService virtualThreadsExecutor;

    public VTFactory() {
    }

    VTFactory(ExecutorService virtualThreadsExecutor) {
        this.virtualThreadsExecutor = Objects.requireNonNull(virtualThreadsExecutor);
    }

    public ExecutorService newVirtualThreadExecutor(String purpose) {
        Objects.requireNonNullElse(purpose, "worker");
        return Objects.requireNonNull(virtualThreadsExecutor, "Quarkus virtual thread executor is not available");
    }
}
