package wn.gateway.session;

import java.time.Instant;

import wn.gateway.domain.ConversationKey;
import wn.gateway.domain.MessageState;

public record SessionSnapshot(
        ConversationKey key,
        String threadId,
        String lastMessageId,
        MessageState state,
        Instant lastUpdatedAt) {
}
