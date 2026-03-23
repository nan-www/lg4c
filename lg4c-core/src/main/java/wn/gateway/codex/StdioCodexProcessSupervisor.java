package wn.gateway.codex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StdioCodexProcessSupervisor {
    private final List<String> command;
    private final Path workspaceRoot;
    private Process process;

    public StdioCodexProcessSupervisor(List<String> command, Path workspaceRoot) {
        this.command = List.copyOf(command);
        this.workspaceRoot = workspaceRoot;
    }

    public synchronized void ensureStarted() {
        if (isAlive()) {
            return;
        }
        List<String> actualCommand = new ArrayList<>(command);
        actualCommand.add("mcp-server");
        actualCommand.add("-C");
        actualCommand.add(workspaceRoot.toString());
        actualCommand.add("-s");
        actualCommand.add("workspace-write");
        actualCommand.add("-a");
        actualCommand.add("on-request");
        ProcessBuilder builder = new ProcessBuilder(actualCommand);
        builder.directory(workspaceRoot.toFile());
        builder.environment().putIfAbsent("OTEL_SDK_DISABLED", "true");
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new CodexTransportException("failed to start codex mcp server", e);
        }
    }

    public synchronized boolean isAlive() {
        return process != null && process.isAlive();
    }

    public synchronized Process process() {
        if (!isAlive()) {
            throw new CodexTransportException("codex process is not running");
        }
        return process;
    }
}
