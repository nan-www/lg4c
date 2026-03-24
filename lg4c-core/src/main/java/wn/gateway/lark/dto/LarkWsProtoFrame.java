package wn.gateway.lark.dto;

import java.util.ArrayList;
import java.util.List;

public record LarkWsProtoFrame(
        long seqId,
        long logId,
        int service,
        int method,
        List<Header> headers,
        String payloadEncoding,
        String payloadType,
        byte[] payload,
        String logIdNew) {

    public LarkWsProtoFrame {
        headers = List.copyOf(headers == null ? List.of() : headers);
        payload = payload == null ? new byte[0] : payload.clone();
    }

    public String headerValue(String key) {
        return headers.stream()
                .filter(header -> header.key().equals(key))
                .map(Header::value)
                .findFirst()
                .orElse(null);
    }

    public int headerValueAsInt(String key, int defaultValue) {
        String value = headerValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public LarkWsProtoFrame withPayload(byte[] nextPayload) {
        return new LarkWsProtoFrame(seqId, logId, service, method, headers, payloadEncoding, payloadType, nextPayload, logIdNew);
    }

    public LarkWsProtoFrame withHeader(String key, String value) {
        List<Header> nextHeaders = new ArrayList<>(headers);
        nextHeaders.add(new Header(key, value));
        return new LarkWsProtoFrame(seqId, logId, service, method, nextHeaders, payloadEncoding, payloadType, payload, logIdNew);
    }

    record Header(String key, String value) {
    }
}
