package wn.gateway.domain;

import java.time.Instant;

public record ConversationEvent(
        ConversationKey conversationKey,
        String messageId,
        String type,
        Instant timestamp,
        MessageState messageState,
        String payload) {
}
