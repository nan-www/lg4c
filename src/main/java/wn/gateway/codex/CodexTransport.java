package wn.gateway.codex;

import wn.gateway.domain.CodexReply;
import wn.gateway.domain.ConversationKey;

public interface CodexTransport {

    CodexReply send(ConversationKey key, String existingThreadId, String prompt);
}
