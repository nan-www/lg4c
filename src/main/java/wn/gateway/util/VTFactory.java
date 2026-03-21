package wn.gateway.util;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VTFactory {

    public ExecutorService newVirtualThreadExecutor(String purpose) {
        String normalizedPurpose = Objects.requireNonNullElse(purpose, "worker")
                .trim()
                .replaceAll("[^a-zA-Z0-9]+", "-");
        String threadNamePrefix = "lg4c-" + (normalizedPurpose.isBlank() ? "worker" : normalizedPurpose) + "-";
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(threadNamePrefix, 0).factory());
    }
}
