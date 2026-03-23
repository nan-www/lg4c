package wn.gateway.lark;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/open-apis/im/v1/messages/{messageId}/reply")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface LarkReplyApi {

    @POST
    CompletionStage<Void> sendReply(
            @HeaderParam("Authorization") String authorization,
            @PathParam("messageId") String messageId,
            LarkReplyRequest request);
}
