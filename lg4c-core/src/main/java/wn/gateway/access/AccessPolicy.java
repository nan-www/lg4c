package wn.gateway.access;

import java.util.Set;

import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.InboundMessage;

public class AccessPolicy {
    private final Set<String> allowedUsers;
    private final Set<String> allowedChats;

    public AccessPolicy(GatewayAppConfig config) {
        this.allowedUsers = Set.copyOf(config.allowedUsers());
        this.allowedChats = Set.copyOf(config.allowedChats());
    }

    public boolean isAllowed(InboundMessage message) {
        return allowedUsers.contains(message.userId()) && allowedChats.contains(message.chatId());
    }
}
