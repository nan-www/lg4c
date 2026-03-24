package wn.gateway.session;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import wn.gateway.domain.ConversationKey;

@ApplicationScoped
public class InMemoryPendingMessageStore {
    private final Map<String, PendingMessage> messages = new ConcurrentHashMap<>();

    public void save(PendingMessage message) {
        messages.put(key(message.key(), message.messageId()), message);
    }

    public Optional<PendingMessage> latestFor(ConversationKey key) {
        return messages.values().stream()
                .filter(message -> message.key().equals(key))
                .max(Comparator.comparing(PendingMessage::timestamp));
    }

    public void delete(ConversationKey key, String messageId) {
        messages.remove(key(key, messageId));
    }

    private String key(ConversationKey key, String messageId) {
        return key.value() + ":" + messageId;
    }
}
