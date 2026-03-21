package wn.gateway.lark;

import java.io.IOException;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Session;
import wn.gateway.config.GatewayAppConfig;

public interface LarkWebSocketConnector {

    Session connect(GatewayAppConfig config, Endpoint endpoint, ClientEndpointConfig endpointConfig)
            throws DeploymentException, IOException;
}
