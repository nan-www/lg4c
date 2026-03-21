package wn.gateway.codex;

public interface CodexProcessSupervisor {

    void ensureStarted();

    boolean isAlive();
}
