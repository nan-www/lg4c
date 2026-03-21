package wn.gateway.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import wn.gateway.domain.CodexReply;
import wn.gateway.domain.ConversationEvent;
import wn.gateway.domain.ConversationKey;
import wn.gateway.domain.InboundMessage;
import wn.gateway.domain.MessageState;
import wn.gateway.session.FileSessionStateStore;
import wn.gateway.session.SessionSnapshot;

class FileConversationRecorderTest {

    @TempDir
    Path tempDir;

    @Test
    void recorderAppendsMarkdownAndStructuredEventsAndSessionState() throws IOException {
        Path recordRoot = tempDir.resolve("records");
        Path stateFile = tempDir.resolve("state/sessions.json");
        FileSessionStateStore sessionStateStore = new FileSessionStateStore(stateFile, new ObjectMapper());
        FileConversationRecorder recorder = new FileConversationRecorder(recordRoot, sessionStateStore, new ObjectMapper());

        ConversationKey key = new ConversationKey("ou_123", "oc_456");
        InboundMessage message = new InboundMessage("ou_123", "oc_456", "om_1", "hello codex", Instant.parse("2026-03-21T10:15:30Z"));

        recorder.appendInbound(message);
        recorder.appendThinking(key, "om_1", List.of("thinking 1", "thinking 2"));
        recorder.appendAnswer(key, "om_1", new CodexReply(List.of("thinking 1", "thinking 2"), "final answer", "thread-1"));
        recorder.appendEvent(new ConversationEvent(key, "om_1", "reply-sent", Instant.parse("2026-03-21T10:15:35Z"), MessageState.REPLIED_TO_LARK,
                "{\"answer\":\"final answer\"}"));
        recorder.saveSession(new SessionSnapshot(key, "thread-1", "om_1", MessageState.REPLIED_TO_LARK, Instant.parse("2026-03-21T10:15:35Z")));

        Path conversationFile = recordRoot.resolve("2026-03-21/ou_123/oc_456/conversation.md");
        Path eventsFile = recordRoot.resolve("2026-03-21/ou_123/oc_456/events.ndjson");

        assertTrue(Files.exists(conversationFile));
        assertTrue(Files.exists(eventsFile));

        String markdown = Files.readString(conversationFile);
        assertTrue(markdown.contains("hello codex"));
        assertTrue(markdown.contains("thinking 1"));
        assertTrue(markdown.contains("final answer"));

        List<String> events = Files.readAllLines(eventsFile);
        assertEquals(4, events.size());
        assertTrue(events.get(0).contains("\"messageState\":\"RECEIVED\""));
        assertTrue(events.get(2).contains("\"messageState\":\"ANSWER_PERSISTED\""));

        List<SessionSnapshot> sessions = sessionStateStore.loadAll();
        assertEquals(1, sessions.size());
        assertEquals("thread-1", sessions.getFirst().threadId());
    }
}
