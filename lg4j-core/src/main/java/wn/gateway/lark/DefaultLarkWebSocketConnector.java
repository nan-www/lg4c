package wn.gateway.lark;

import java.io.IOException;
import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ApplicationScoped
public class DefaultLarkWebSocketConnector implements LarkWebSocketConnector {

    @Override
    public Session connect(String websocketUrl, Endpoint endpoint, ClientEndpointConfig endpointConfig)
            throws DeploymentException, IOException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        return container.connectToServer(endpoint, endpointConfig, URI.create(websocketUrl));
    }
}
