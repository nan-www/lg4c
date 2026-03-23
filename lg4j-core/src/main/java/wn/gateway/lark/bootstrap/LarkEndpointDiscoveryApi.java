package wn.gateway.lark.bootstrap;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import wn.gateway.lark.bootstrap.dto.LarkEndpointDiscoveryRequest;
import wn.gateway.lark.bootstrap.dto.LarkEndpointDiscoveryResponse;

@Path("/callback/ws/endpoint")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface LarkEndpointDiscoveryApi {

    @POST
    LarkEndpointDiscoveryResponse discover(LarkEndpointDiscoveryRequest request);
}
