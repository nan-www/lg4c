package wn.gateway.session;

import java.util.Optional;

import wn.gateway.domain.ConversationKey;

public interface PendingMessageStore {

    void save(PendingMessage message);

    Optional<PendingMessage> latestFor(ConversationKey key);

    void delete(ConversationKey key, String messageId);
}
