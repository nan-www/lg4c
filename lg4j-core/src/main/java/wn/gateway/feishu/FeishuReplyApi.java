package wn.gateway.feishu;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface FeishuReplyApi {

    @POST
    CompletionStage<Void> sendReply(
            @HeaderParam("X-Feishu-App-Id") String appId,
            @HeaderParam("X-Feishu-App-Secret") String appSecret,
            FeishuReplyRequest request);
}
