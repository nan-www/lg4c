package example.usage;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class VirtualThreadExample {
    private VirtualThreadExample() {
    }

    public static void main(String[] args) throws Exception {
        try (var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            Future<String> result = executor.submit(() -> "example module running on VT=" + Thread.currentThread().isVirtual());
            System.out.println(result.get());
        }
    }
}
