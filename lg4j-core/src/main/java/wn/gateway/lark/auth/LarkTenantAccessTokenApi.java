package wn.gateway.lark.auth;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/open-apis/auth/v3/tenant_access_token/internal")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface LarkTenantAccessTokenApi {

    @POST
    LarkTenantAccessTokenResponse fetch(LarkTenantAccessTokenRequest request);
}
