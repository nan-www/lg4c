package wn.gateway.lark;

import java.io.IOException;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Session;

public interface LarkWebSocketConnector {

    Session connect(String websocketUrl, Endpoint endpoint, ClientEndpointConfig endpointConfig)
            throws DeploymentException, IOException;
}
