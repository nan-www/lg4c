package wn.gateway.record;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import wn.gateway.domain.CodexReply;
import wn.gateway.domain.ConversationEvent;
import wn.gateway.domain.ConversationKey;
import wn.gateway.domain.InboundMessage;
import wn.gateway.domain.MessageState;
import wn.gateway.session.FileSessionStateStore;
import wn.gateway.session.SessionSnapshot;

public class FileConversationRecorder {
    private final Path recordRoot;
    private final FileSessionStateStore stateStore;
    private final ObjectMapper mapper;
    private final Map<String, LocalDate> conversationDates = new ConcurrentHashMap<>();

    public FileConversationRecorder(Path recordRoot, FileSessionStateStore stateStore, ObjectMapper mapper) {
        this.recordRoot = recordRoot;
        this.stateStore = stateStore;
        this.mapper = mapper.copy().registerModule(new JavaTimeModule());
    }

    public void appendInbound(InboundMessage message) throws IOException {
        LocalDate date = LocalDate.ofInstant(message.eventTime(), ZoneOffset.UTC);
        conversationDates.put(message.conversationKey().value(), date);
        appendMarkdown(message.conversationKey(), """
                ## User
                %s

                """.formatted(message.text()));
        appendEvent(new ConversationEvent(message.conversationKey(), message.messageId(), "inbound", message.eventTime(), MessageState.RECEIVED,
                mapper.writeValueAsString(message)));
    }

    public void appendThinking(ConversationKey key, String messageId, List<String> thinkingChunks) throws IOException {
        appendMarkdown(key, """
                ## Thinking
                %s

                """.formatted(String.join("\n", thinkingChunks)));
        appendEvent(new ConversationEvent(key, messageId, "thinking", Instant.now(), MessageState.SENT_TO_CODEX,
                mapper.writeValueAsString(thinkingChunks)));
    }

    public void appendAnswer(ConversationKey key, String messageId, CodexReply reply) throws IOException {
        appendMarkdown(key, """
                ## Answer
                %s

                """.formatted(reply.finalAnswer()));
        appendEvent(new ConversationEvent(key, messageId, "answer", Instant.now(), MessageState.ANSWER_PERSISTED,
                mapper.writeValueAsString(reply)));
    }

    public void appendEvent(ConversationEvent event) throws IOException {
        Path eventsFile = ensureConversationDir(event.conversationKey(), event.timestamp()).resolve("events.ndjson");
        appendLine(eventsFile, mapper.writeValueAsString(event));
    }

    public void saveSession(SessionSnapshot snapshot) throws IOException {
        stateStore.save(snapshot);
    }

    private void appendMarkdown(ConversationKey key, String text) throws IOException {
        Path conversationFile = ensureConversationDir(key, Instant.now()).resolve("conversation.md");
        appendLine(conversationFile, text);
    }

    private void appendLine(Path file, String line) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, line.endsWith("\n") ? line : line + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }

    private Path ensureConversationDir(ConversationKey key, Instant fallbackTime) {
        LocalDate date = conversationDates.computeIfAbsent(key.value(), ignored -> LocalDate.ofInstant(fallbackTime, ZoneOffset.UTC));
        return recordRoot.resolve(date.toString()).resolve(key.userId()).resolve(key.chatId());
    }
}
