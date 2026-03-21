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
import wn.gateway.feishu.FeishuGatewayClient;
import wn.gateway.feishu.FeishuGatewayClientFactory;
import wn.gateway.record.ConversationRecorder;
import wn.gateway.record.FileConversationRecorder;
import wn.gateway.session.FileSessionStateStore;
import wn.gateway.session.InMemoryPendingMessageStore;
import wn.gateway.session.PendingMessage;
import wn.gateway.session.SerialConversationDispatcher;
import wn.gateway.session.SessionSnapshot;

@ApplicationScoped
public class GatewayDaemonService {
    @Inject
    ObjectMapper mapper;

    @Inject
    FeishuGatewayClientFactory feishuGatewayClientFactory;

    public void run(GatewayAppConfig config) {
        GatewayRuntimeState.markLive(true);
        AccessPolicy accessPolicy = new AccessPolicy(config);
        FileSessionStateStore stateStore = new FileSessionStateStore(config.recordRoot().getParent().resolve("../state/sessions.json").normalize(), mapper);
        ConversationRecorder recorder = new FileConversationRecorder(config.recordRoot(), stateStore, mapper);
        InMemoryPendingMessageStore pendingStore = new InMemoryPendingMessageStore();
        SerialConversationDispatcher dispatcher = new SerialConversationDispatcher();
        StdioCodexProcessSupervisor supervisor = new StdioCodexProcessSupervisor(config.codexCommand(), config.workspaceRoot());
        ManagedCodexSessionManager sessionManager = new ManagedCodexSessionManager(supervisor, new StdioCodexTransport(supervisor, mapper), pendingStore);
        FeishuGatewayClient feishuClient = feishuGatewayClientFactory.create(config);

        try {
            supervisor.ensureStarted();
            feishuClient.start(message -> handleMessage(message, accessPolicy, recorder, dispatcher, pendingStore, sessionManager, feishuClient));
            GatewayRuntimeState.markReady(supervisor.isAlive() && feishuClient.isConnected());
            Thread.currentThread().join();
        } catch (Exception e) {
            GatewayRuntimeState.markReady(false);
            throw new IllegalStateException("gateway daemon failed", e);
        } finally {
            dispatcher.close();
            try {
                feishuClient.close();
            } catch (IOException ignored) {
                // best effort shutdown
            }
        }
    }

    private void handleMessage(
            InboundMessage message,
            AccessPolicy accessPolicy,
            ConversationRecorder recorder,
            SerialConversationDispatcher dispatcher,
            InMemoryPendingMessageStore pendingStore,
            ManagedCodexSessionManager sessionManager,
            FeishuGatewayClient feishuClient) {
        if (!accessPolicy.isAllowed(message)) {
            feishuClient.sendReply(message, "Access denied by lg4c whitelist.");
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
                feishuClient.sendReply(message, reply.finalAnswer()).join();
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
                feishuClient.sendReply(message, "LG4C failed to process the request.").join();
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
