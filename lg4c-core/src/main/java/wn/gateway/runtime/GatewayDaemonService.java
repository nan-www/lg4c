package wn.gateway.runtime;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import wn.gateway.access.AccessPolicy;
import wn.gateway.codex.ManagedCodexSessionManager;
import wn.gateway.codex.StdioCodexProcessSupervisor;
import wn.gateway.codex.StdioCodexTransport;
import wn.gateway.config.GatewayAppConfig;
import wn.gateway.domain.ConversationEvent;
import wn.gateway.domain.CodexReply;
import wn.gateway.domain.InboundMessage;
import wn.gateway.domain.MessageState;
import wn.gateway.lark.LarkGatewayClientFactory;
import wn.gateway.lark.LarkGatewayClient;
import wn.gateway.record.FileConversationRecorder;
import wn.gateway.session.FileSessionStateStore;
import wn.gateway.session.InMemoryPendingMessageStore;
import wn.gateway.session.PendingMessage;
import wn.gateway.session.SerialConversationDispatcher;
import wn.gateway.session.SessionSnapshot;
import wn.gateway.util.VTFactory;

@ApplicationScoped
public class GatewayDaemonService {
    @Inject
    ObjectMapper mapper;

    @Inject
    LarkGatewayClientFactory larkGatewayClientFactory;

    public void run(GatewayAppConfig config) {
        GatewayRuntimeState.markLive(true);
        AccessPolicy accessPolicy = new AccessPolicy(config);
        FileSessionStateStore stateStore = new FileSessionStateStore(config.recordRoot().getParent().resolve("../state/sessions.json").normalize(), mapper);
        FileConversationRecorder recorder = new FileConversationRecorder(config.recordRoot(), stateStore, mapper);
        InMemoryPendingMessageStore pendingStore = new InMemoryPendingMessageStore();
        SerialConversationDispatcher dispatcher = new SerialConversationDispatcher(
                VTFactory.newVirtualThreadExecutor("conversation-dispatch"),
                false);
        StdioCodexProcessSupervisor supervisor = new StdioCodexProcessSupervisor(config.codexCommand(), config.workspaceRoot());
        ManagedCodexSessionManager sessionManager = new ManagedCodexSessionManager(supervisor, new StdioCodexTransport(supervisor, mapper), pendingStore);
        LarkGatewayClient larkClient = larkGatewayClientFactory.create(config);

        try {
            supervisor.ensureStarted();
            larkClient.start(message -> handleMessage(message, accessPolicy, recorder, dispatcher, pendingStore, sessionManager, larkClient));
            GatewayRuntimeState.markReady(supervisor.isAlive() && larkClient.isConnected());
            Thread.currentThread().join();
        } catch (Exception e) {
            GatewayRuntimeState.markReady(false);
            throw new IllegalStateException("gateway daemon failed", e);
        } finally {
            dispatcher.close();
            try {
                larkClient.close();
            } catch (IOException ignored) {
                // best effort shutdown
            }
        }
    }

    private void handleMessage(
            InboundMessage message,
            AccessPolicy accessPolicy,
            FileConversationRecorder recorder,
            SerialConversationDispatcher dispatcher,
            InMemoryPendingMessageStore pendingStore,
            ManagedCodexSessionManager sessionManager,
            LarkGatewayClient larkClient) {
        if (!accessPolicy.isAllowed(message)) {
            larkClient.sendReply(message, "Access denied by lg4c whitelist.");
            return;
        }

        dispatcher.dispatch(message.conversationKey(), () -> {
            try {
                recorder.appendInbound(message);
                pendingStore.save(new PendingMessage(message.conversationKey(), message.messageId(), message.text(), MessageState.SENT_TO_CODEX, message.eventTime()));

                CodexReply reply = sessionManager.send(message.conversationKey(), message.messageId(), message.text());
                recorder.appendThinking(message.conversationKey(), message.messageId(), reply.thinkingChunks());
                recorder.appendAnswer(message.conversationKey(), message.messageId(), reply);
                recorder.saveSession(new SessionSnapshot(message.conversationKey(), reply.sessionId(), message.messageId(),
                        MessageState.ANSWER_PERSISTED, Instant.now()));
                larkClient.sendReply(message, reply.finalAnswer()).join();
                recorder.appendEvent(new ConversationEvent(message.conversationKey(), message.messageId(), "reply-sent", Instant.now(),
                        MessageState.REPLIED_TO_LARK, "{\"status\":\"ok\"}"));
                recorder.saveSession(new SessionSnapshot(message.conversationKey(), reply.sessionId(), message.messageId(),
                        MessageState.DONE, Instant.now()));
            } catch (Exception e) {
                try {
                    recorder.appendEvent(new ConversationEvent(message.conversationKey(), message.messageId(), "failed", Instant.now(),
                            MessageState.FAILED, "{\"error\":\"" + sanitize(e.getMessage()) + "\"}"));
                } catch (IOException ignored) {
                    // best effort
                }
                larkClient.sendReply(message, "LG4C failed to process the request.").join();
            }
        });
    }

    private String sanitize(String message) {
        if (message == null) {
            return "unknown";
        }
        return message.replace('"', '\'');
    }
}
