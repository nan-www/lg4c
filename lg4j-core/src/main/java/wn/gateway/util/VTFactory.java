package wn.gateway.util;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VTFactory {

    private VTFactory() {
    }

    public static ExecutorService newVirtualThreadExecutor(String purpose) {
        String normalizedPurpose = normalizePurpose(purpose);
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name(normalizedPurpose + "-", 0).factory());
    }

    private static String normalizePurpose(String purpose) {
        String value = Objects.requireNonNullElse(purpose, "worker")
                .trim()
                .replaceAll("[^a-zA-Z0-9]+", "-");
        return value.isBlank() ? "worker" : value;
    }
}
