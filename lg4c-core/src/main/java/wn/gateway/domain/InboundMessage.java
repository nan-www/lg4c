package wn.gateway.domain;

import java.time.Instant;

public record InboundMessage(String userId, String chatId, String messageId, String text, Instant eventTime) {

    public ConversationKey conversationKey() {
        return new ConversationKey(userId, chatId);
    }
}
