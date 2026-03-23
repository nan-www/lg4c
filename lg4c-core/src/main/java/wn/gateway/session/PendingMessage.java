package wn.gateway.session;

import java.time.Instant;

import wn.gateway.domain.ConversationKey;
import wn.gateway.domain.MessageState;

public record PendingMessage(
        ConversationKey key,
        String messageId,
        String prompt,
        MessageState state,
        Instant timestamp) {
}
