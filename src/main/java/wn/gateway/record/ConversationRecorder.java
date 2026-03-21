package wn.gateway.record;

import java.io.IOException;
import java.util.List;

import wn.gateway.domain.CodexReply;
import wn.gateway.domain.ConversationEvent;
import wn.gateway.domain.ConversationKey;
import wn.gateway.domain.InboundMessage;
import wn.gateway.session.SessionSnapshot;

public interface ConversationRecorder {

    void appendInbound(InboundMessage message) throws IOException;

    void appendThinking(ConversationKey key, String messageId, List<String> thinkingChunks) throws IOException;

    void appendAnswer(ConversationKey key, String messageId, CodexReply reply) throws IOException;

    void appendEvent(ConversationEvent event) throws IOException;

    void saveSession(SessionSnapshot snapshot) throws IOException;
}
