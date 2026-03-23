package wn.gateway.domain;

import java.util.List;

public record CodexReply(List<String> thinkingChunks, String finalAnswer, String sessionId) {

    public CodexReply {
        thinkingChunks = List.copyOf(thinkingChunks);
    }
}
