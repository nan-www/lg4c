package example.usage.lark;

import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;

import java.nio.charset.StandardCharsets;

public class SendReply {
    public static void main(String arg[]) throws Exception {
        // 构建client
        Client client = Client.newBuilder("YOUR_APP_ID", "YOUR_APP_SECRET").openBaseUrl("https://fsopen.bytedance.net").build();

        // 创建请求对象
        CreateMessageReq req = CreateMessageReq.newBuilder()
                .receiveIdType("open_id")
                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                        .receiveId("ou_7d8a6e6df7621556ce0d21922b676706ccs")
                        .msgType("text")
                        .content("{\"text\":\"test content\"}")
                        .uuid("选填，每次调用前请更换，如a0d69e20-1dd1-458b-k525-dfeca4015204")
                        .build())
                .build();

        // 发起请求
        CreateMessageResp resp = client.im().v1().message().create(req);

        // 处理服务端错误
        if (!resp.success()) {
            System.out.println(String.format("code:%s,msg:%s,reqId:%s, resp:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId(), Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)))));
            return;
        }

        // 业务数据处理
        System.out.println(Jsons.DEFAULT.toJson(resp.getData()));
    }
}
