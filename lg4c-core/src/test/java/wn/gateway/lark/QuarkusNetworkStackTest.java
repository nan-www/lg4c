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
    void pomUsesOfficialLarkSdkOnlyForOutboundMessaging() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertFalse(
                pom.contains("<artifactId>quarkus-rest-client-jackson</artifactId>"),
                "manual rest client wiring should be removed after switching to the official lark sdk");
        assertTrue(
                pom.contains("<groupId>com.larksuite.oapi</groupId>"),
                "lg4c-core should depend on the official lark sdk for long connections");
    }

    private boolean containsJdkHttpClientImport(Path path) {
        try {
            return Files.readString(path).contains("java.net.http");
        } catch (IOException e) {
            throw new IllegalStateException("failed to inspect " + path, e);
        }
    }
}
