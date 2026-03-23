package wn.gateway.lark.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LarkReplyRequest(
        String content,
        String msgType,
        String uuid) {
}
