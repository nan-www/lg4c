package wn.gateway.domain;

public enum MessageState {
    RECEIVED,
    SENT_TO_CODEX,
    ANSWER_PERSISTED,
    REPLIED_TO_LARK,
    DONE,
    FAILED
}
