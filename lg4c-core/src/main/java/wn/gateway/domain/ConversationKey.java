package wn.gateway.domain;

public record ConversationKey(String userId, String chatId) {

    public String value() {
        return userId + ":" + chatId;
    }
}
