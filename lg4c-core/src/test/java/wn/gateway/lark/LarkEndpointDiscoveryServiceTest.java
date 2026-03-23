package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryApiFactory;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryApi;
import wn.gateway.lark.bootstrap.dto.LarkEndpointDiscoveryResponse;
import wn.gateway.lark.bootstrap.dto.LarkWsBootstrapResult;

class LarkEndpointDiscoveryServiceTest extends LarkTestSupport {

    @Test
    void returnsConfiguredOverrideWithoutCallingApi() {
        LarkEndpointDiscoveryApiFactory apiFactory = mock(LarkEndpointDiscoveryApiFactory.class);
        LarkEndpointDiscoveryService service = new LarkEndpointDiscoveryService(apiFactory);

        LarkWsBootstrapResult result = service.resolve(configBuilder()
                .feishuWebsocketUrl("wss://open.feishu.test/ws")
                .build());

        assertEquals("wss://open.feishu.test/ws", result.websocketUrl());
        assertEquals(LarkClientRuntimeConfig.DEFAULT, result.runtimeConfig());
        verify(apiFactory, never()).create(any());
    }

    @Test
    void mapsSuccessfulDiscoveryResponse() {
        LarkClientRuntimeConfig runtimeConfig = new LarkClientRuntimeConfig(3, 10, 5, 20);
        LarkEndpointDiscoveryResponse response = LarkEndpointDiscoveryResponse.success(
                "wss://open.feishu.cn/ws/123",
                runtimeConfig);
        LarkEndpointDiscoveryApi api = mock(LarkEndpointDiscoveryApi.class);
        LarkEndpointDiscoveryApiFactory apiFactory = mock(LarkEndpointDiscoveryApiFactory.class);
        when(apiFactory.create(any())).thenReturn(api);
        when(api.discover(any())).thenReturn(response);
        LarkEndpointDiscoveryService service = new LarkEndpointDiscoveryService(apiFactory);

        LarkWsBootstrapResult result = service.resolve(config());

        assertEquals("wss://open.feishu.cn/ws/123", result.websocketUrl());
        assertEquals(runtimeConfig, result.runtimeConfig());
    }
}
