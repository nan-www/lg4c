package wn.gateway.codex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import wn.gateway.domain.CodexReply;
import wn.gateway.domain.ConversationKey;
import wn.gateway.domain.MessageState;
import wn.gateway.session.InMemoryPendingMessageStore;
import wn.gateway.session.PendingMessage;

class ManagedCodexSessionManagerTest {

    @Test
    void restartsProcessAndReplaysLatestPendingMessageAfterFailure() {
        StdioCodexTransport transport = mock(StdioCodexTransport.class);
        StdioCodexProcessSupervisor supervisor = mock(StdioCodexProcessSupervisor.class);
        InMemoryPendingMessageStore pendingStore = new InMemoryPendingMessageStore();
        ManagedCodexSessionManager manager = new ManagedCodexSessionManager(supervisor, transport, pendingStore);
        ConversationKey key = new ConversationKey("u1", "c1");
        CodexReply expectedReply = new CodexReply(List.of("thinking retry"), "answer after retry", "thread-1");

        pendingStore.save(new PendingMessage(key, "m1", "hello", MessageState.SENT_TO_CODEX, Instant.parse("2026-03-21T10:00:00Z")));
        when(transport.send(eq(key), isNull(), eq("hello")))
                .thenThrow(new CodexTransportException("boom"))
                .thenReturn(expectedReply);

        CodexReply reply = manager.send(key, "m1", "hello");

        verify(supervisor, times(2)).ensureStarted();
        verify(transport, times(2)).send(eq(key), isNull(), eq("hello"));
        assertEquals("thread-1", reply.sessionId());
        assertEquals("answer after retry", reply.finalAnswer());
        assertEquals(List.of("thinking retry"), reply.thinkingChunks());
    }

    @Test
    void failsWhenReplayStillCannotSucceed() {
        StdioCodexTransport transport = mock(StdioCodexTransport.class);
        StdioCodexProcessSupervisor supervisor = mock(StdioCodexProcessSupervisor.class);
        InMemoryPendingMessageStore pendingStore = new InMemoryPendingMessageStore();
        ManagedCodexSessionManager manager = new ManagedCodexSessionManager(supervisor, transport, pendingStore);
        ConversationKey key = new ConversationKey("u1", "c1");

        pendingStore.save(new PendingMessage(key, "m1", "hello", MessageState.SENT_TO_CODEX,
                Instant.parse("2026-03-21T10:00:00Z")));
        when(transport.send(eq(key), isNull(), eq("hello")))
                .thenThrow(new CodexTransportException("still broken"));

        assertThrows(CodexTransportException.class, () -> manager.send(key, "m1", "hello"));
        verify(supervisor, times(2)).ensureStarted();
        verify(transport, times(2)).send(eq(key), isNull(), eq("hello"));
    }
}
