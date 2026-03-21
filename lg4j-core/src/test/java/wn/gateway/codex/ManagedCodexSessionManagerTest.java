package wn.gateway.codex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import wn.gateway.domain.CodexReply;
import wn.gateway.domain.ConversationKey;
import wn.gateway.domain.MessageState;
import wn.gateway.session.InMemoryPendingMessageStore;
import wn.gateway.session.PendingMessage;

class ManagedCodexSessionManagerTest {

    @Test
    void restartsProcessAndReplaysLatestPendingMessageAfterFailure() {
        AtomicInteger attempts = new AtomicInteger();
        FakeCodexTransport transport = new FakeCodexTransport(attempts);
        FakeCodexProcessSupervisor supervisor = new FakeCodexProcessSupervisor();
        InMemoryPendingMessageStore pendingStore = new InMemoryPendingMessageStore();
        ManagedCodexSessionManager manager = new ManagedCodexSessionManager(supervisor, transport, pendingStore);
        ConversationKey key = new ConversationKey("u1", "c1");

        pendingStore.save(new PendingMessage(key, "m1", "hello", MessageState.SENT_TO_CODEX, Instant.parse("2026-03-21T10:00:00Z")));

        CodexReply reply = manager.send(key, "m1", "hello");

        assertEquals(2, supervisor.startCount());
        assertEquals(2, attempts.get());
        assertEquals("thread-1", reply.sessionId());
        assertEquals("answer after retry", reply.finalAnswer());
        assertEquals(List.of("thinking retry"), reply.thinkingChunks());
    }

    @Test
    void failsWhenReplayStillCannotSucceed() {
        AlwaysFailingTransport transport = new AlwaysFailingTransport();
        FakeCodexProcessSupervisor supervisor = new FakeCodexProcessSupervisor();
        InMemoryPendingMessageStore pendingStore = new InMemoryPendingMessageStore();
        ManagedCodexSessionManager manager = new ManagedCodexSessionManager(supervisor, transport, pendingStore);

        pendingStore.save(new PendingMessage(new ConversationKey("u1", "c1"), "m1", "hello", MessageState.SENT_TO_CODEX,
                Instant.parse("2026-03-21T10:00:00Z")));

        assertThrows(CodexTransportException.class, () -> manager.send(new ConversationKey("u1", "c1"), "m1", "hello"));
        assertEquals(2, supervisor.startCount());
    }

    private static final class FakeCodexProcessSupervisor implements CodexProcessSupervisor {
        private int startCount;

        @Override
        public synchronized void ensureStarted() {
            startCount++;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        int startCount() {
            return startCount;
        }
    }

    private static final class FakeCodexTransport implements CodexTransport {
        private final AtomicInteger attempts;

        private FakeCodexTransport(AtomicInteger attempts) {
            this.attempts = attempts;
        }

        @Override
        public CodexReply send(ConversationKey key, String existingThreadId, String prompt) {
            if (attempts.incrementAndGet() == 1) {
                throw new CodexTransportException("boom");
            }
            return new CodexReply(List.of("thinking retry"), "answer after retry", "thread-1");
        }
    }

    private static final class AlwaysFailingTransport implements CodexTransport {
        @Override
        public CodexReply send(ConversationKey key, String existingThreadId, String prompt) {
            throw new CodexTransportException("still broken");
        }
    }
}
