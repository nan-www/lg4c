package wn.gateway.lark;

public record LarkReplyRequest(
        String messageId,
        String chatId,
        String userId,
        String text) {
}
