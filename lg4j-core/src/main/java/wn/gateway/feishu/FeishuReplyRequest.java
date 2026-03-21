package wn.gateway.feishu;

public record FeishuReplyRequest(
        String messageId,
        String chatId,
        String userId,
        String text) {
}
