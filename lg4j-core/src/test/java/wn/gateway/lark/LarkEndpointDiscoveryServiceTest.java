package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import wn.gateway.config.GatewayAppConfig;
import wn.gateway.lark.bootstrap.DefaultLarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryApi;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryResponse;
import wn.gateway.lark.bootstrap.LarkWsBootstrapResult;

class LarkEndpointDiscoveryServiceTest extends LarkTestSupport {

    @Test
    void returnsConfiguredOverrideWithoutCallingApi() {
        LarkEndpointDiscoveryApi api = request -> {
            throw new AssertionError("api should not be called when websocket override is configured");
        };
        DefaultLarkEndpointDiscoveryService service = DefaultLarkEndpointDiscoveryService.forTest(api);

        LarkWsBootstrapResult result = service.resolve(configBuilder()
                .feishuWebsocketUrl("wss://open.feishu.test/ws")
                .build());

        assertEquals("wss://open.feishu.test/ws", result.websocketUrl());
        assertEquals(LarkClientRuntimeConfig.DEFAULT, result.runtimeConfig());
    }

    @Test
    void mapsSuccessfulDiscoveryResponse() {
        LarkClientRuntimeConfig runtimeConfig = new LarkClientRuntimeConfig(3, 10, 5, 20);
        LarkEndpointDiscoveryResponse response = LarkEndpointDiscoveryResponse.success(
                "wss://open.feishu.cn/ws/123",
                runtimeConfig);
        LarkEndpointDiscoveryApi api = request -> response;
        DefaultLarkEndpointDiscoveryService service = DefaultLarkEndpointDiscoveryService.forTest(api);

        LarkWsBootstrapResult result = service.resolve(config());

        assertEquals("wss://open.feishu.cn/ws/123", result.websocketUrl());
        assertEquals(runtimeConfig, result.runtimeConfig());
    }
}
