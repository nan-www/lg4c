package wn.gateway.codex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import wn.gateway.domain.CodexReply;
import wn.gateway.domain.ConversationKey;

public class StdioCodexTransport {
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final StdioCodexProcessSupervisor supervisor;
    private final ObjectMapper mapper;
    private final AtomicLong requestIds = new AtomicLong(1);
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean initialized;

    public StdioCodexTransport(StdioCodexProcessSupervisor supervisor, ObjectMapper mapper) {
        this.supervisor = supervisor;
        this.mapper = mapper;
    }

    public synchronized CodexReply send(ConversationKey key, String existingThreadId, String prompt) {
        try {
            ensureInitialized();
            JsonNode result;
            if (existingThreadId == null || existingThreadId.isBlank()) {
                result = request("tools/call", toolCall("codex", prompt, null));
            } else {
                result = request("tools/call", toolCall("codex-reply", prompt, existingThreadId));
            }
            return parseReply(result);
        } catch (IOException e) {
            reset();
            throw new CodexTransportException("failed to talk to codex mcp server", e);
        }
    }

    private void ensureInitialized() throws IOException {
        supervisor.ensureStarted();
        if (initialized && reader != null && writer != null) {
            return;
        }
        Process process = supervisor.process();
        reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        request("initialize", initializePayload());
        notifyInitialized();
        request("tools/list", mapper.createObjectNode());
        initialized = true;
    }

    private ObjectNode initializePayload() {
        ObjectNode params = mapper.createObjectNode();
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.putObject("capabilities");
        params.putObject("clientInfo").put("name", "lg4c").put("version", "1.0.0");
        return params;
    }

    private ObjectNode toolCall(String toolName, String prompt, String threadId) {
        ObjectNode params = mapper.createObjectNode();
        params.put("name", toolName);
        ObjectNode arguments = params.putObject("arguments");
        arguments.put("prompt", prompt);
        if (threadId != null) {
            arguments.put("thread_id", threadId);
        }
        return params;
    }

    private JsonNode request(String method, JsonNode params) throws IOException {
        ObjectNode payload = mapper.createObjectNode();
        long id = requestIds.getAndIncrement();
        payload.put("jsonrpc", "2.0");
        payload.put("id", id);
        payload.put("method", method);
        payload.set("params", params);
        writer.write(mapper.writeValueAsString(payload));
        writer.write('\n');
        writer.flush();

        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                continue;
            }
            JsonNode node = mapper.readTree(line);
            if (node.has("id") && node.get("id").asLong() == id) {
                if (node.has("error")) {
                    throw new CodexTransportException(node.get("error").toString());
                }
                return node.path("result");
            }
        }
        throw new CodexTransportException("timed out waiting for " + method);
    }

    private void notifyInitialized() throws IOException {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("jsonrpc", "2.0");
        payload.put("method", "notifications/initialized");
        payload.set("params", mapper.createObjectNode());
        writer.write(mapper.writeValueAsString(payload));
        writer.write('\n');
        writer.flush();
    }

    private CodexReply parseReply(JsonNode result) {
        List<String> texts = new ArrayList<>();
        JsonNode content = result.path("content");
        if (content instanceof ArrayNode arrayNode) {
            for (JsonNode node : arrayNode) {
                if (node.hasNonNull("text")) {
                    texts.add(node.get("text").asText());
                }
            }
        }
        JsonNode structured = result.path("structuredContent");
        String threadId = extractThreadId(result, structured);
        String finalAnswer = texts.isEmpty() ? result.toString() : texts.getLast();
        return new CodexReply(texts, finalAnswer, threadId);
    }

    private String extractThreadId(JsonNode result, JsonNode structured) {
        if (structured.hasNonNull("thread_id")) {
            return structured.get("thread_id").asText();
        }
        if (structured.hasNonNull("threadId")) {
            return structured.get("threadId").asText();
        }
        if (result.hasNonNull("thread_id")) {
            return result.get("thread_id").asText();
        }
        return null;
    }

    private void reset() {
        initialized = false;
        reader = null;
        writer = null;
    }
}
