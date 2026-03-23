package wn.gateway.lark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import wn.gateway.lark.bootstrap.DefaultLarkEndpointDiscoveryApiFactory;
import wn.gateway.lark.bootstrap.DefaultLarkEndpointDiscoveryService;
import wn.gateway.lark.bootstrap.LarkClientRuntimeConfig;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryApi;
import wn.gateway.lark.bootstrap.LarkEndpointDiscoveryResponse;
import wn.gateway.lark.bootstrap.LarkWsBootstrapResult;

class LarkEndpointDiscoveryServiceTest extends LarkTestSupport {

    @Test
    void returnsConfiguredOverrideWithoutCallingApi() {
        DefaultLarkEndpointDiscoveryApiFactory apiFactory = mock(DefaultLarkEndpointDiscoveryApiFactory.class);
        DefaultLarkEndpointDiscoveryService service = new DefaultLarkEndpointDiscoveryService(apiFactory);

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
        DefaultLarkEndpointDiscoveryApiFactory apiFactory = mock(DefaultLarkEndpointDiscoveryApiFactory.class);
        when(apiFactory.create(any())).thenReturn(api);
        when(api.discover(any())).thenReturn(response);
        DefaultLarkEndpointDiscoveryService service = new DefaultLarkEndpointDiscoveryService(apiFactory);

        LarkWsBootstrapResult result = service.resolve(config());

        assertEquals("wss://open.feishu.cn/ws/123", result.websocketUrl());
        assertEquals(runtimeConfig, result.runtimeConfig());
    }
}
