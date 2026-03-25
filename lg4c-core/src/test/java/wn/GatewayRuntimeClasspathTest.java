package wn;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class GatewayRuntimeClasspathTest {

    @Test
    void runtimeClasspathDoesNotCarryQuarkusHttpServer() {
        assertNull(
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResource("io/quarkus/vertx/http/HttpServer.class"),
                "The CLI runtime should not carry the Quarkus HTTP server");
    }
}
