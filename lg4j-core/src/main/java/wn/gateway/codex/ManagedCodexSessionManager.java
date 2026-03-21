package wn.gateway.codex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import wn.gateway.domain.CodexReply;
import wn.gateway.domain.ConversationKey;
import wn.gateway.session.PendingMessage;
import wn.gateway.session.PendingMessageStore;

public class ManagedCodexSessionManager {
    private final CodexProcessSupervisor supervisor;
    private final CodexTransport transport;
    private final PendingMessageStore pendingStore;
    private final Map<String, String> threadIds = new ConcurrentHashMap<>();

    public ManagedCodexSessionManager(CodexProcessSupervisor supervisor, CodexTransport transport, PendingMessageStore pendingStore) {
        this.supervisor = supervisor;
        this.transport = transport;
        this.pendingStore = pendingStore;
    }

    public CodexReply send(ConversationKey key, String messageId, String prompt) {
        supervisor.ensureStarted();
        String existingThreadId = threadIds.get(key.value());
        try {
            CodexReply reply = transport.send(key, existingThreadId, prompt);
            remember(key, reply);
            pendingStore.delete(key, messageId);
            return reply;
        } catch (CodexTransportException firstFailure) {
            supervisor.ensureStarted();
            PendingMessage pendingMessage = pendingStore.latestFor(key)
                    .filter(message -> message.messageId().equals(messageId))
                    .orElseThrow(() -> firstFailure);
            CodexReply reply = transport.send(key, null, pendingMessage.prompt());
            remember(key, reply);
            pendingStore.delete(key, pendingMessage.messageId());
            return reply;
        }
    }

    private void remember(ConversationKey key, CodexReply reply) {
        if (reply.sessionId() != null && !reply.sessionId().isBlank()) {
            threadIds.put(key.value(), reply.sessionId());
        }
    }
}
