package wn.gateway.codex;

public class CodexTransportException extends RuntimeException {
    public CodexTransportException(String message) {
        super(message);
    }

    public CodexTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
