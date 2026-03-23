package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class QuarkusNetworkStackTest {

    @Test
    void productionCodeDoesNotUseJdkHttpClient() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            boolean usesJdkHttpClient = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(this::containsJdkHttpClientImport);

            assertFalse(usesJdkHttpClient, "network io should not rely on java.net.http");
        }
    }

    @Test
    void pomIncludesQuarkusRestClientDependency() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(
                pom.contains("<artifactId>quarkus-rest-client-jackson</artifactId>"),
                "quarkus rest client should back outbound http calls");
        assertFalse(
                pom.contains("<groupId>com.larksuite.oapi</groupId>"),
                "lg4j-core must not depend on the official lark sdk");
    }

    private boolean containsJdkHttpClientImport(Path path) {
        try {
            return Files.readString(path).contains("java.net.http");
        } catch (IOException e) {
            throw new IllegalStateException("failed to inspect " + path, e);
        }
    }
}
