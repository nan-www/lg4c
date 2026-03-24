package wn.gateway.codex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StdioCodexProcessSupervisor {
    private static final int MAX_STDERR_CHARS = 8192;

    private final List<String> command;
    private final Path workspaceRoot;
    private Process process;
    private final StringBuilder recentStderr = new StringBuilder();

    public StdioCodexProcessSupervisor(List<String> command, Path workspaceRoot) {
        this.command = List.copyOf(command);
        this.workspaceRoot = workspaceRoot;
    }

    public synchronized void ensureStarted() {
        if (isAlive()) {
            return;
        }
        List<String> actualCommand = new ArrayList<>(command);
        actualCommand.add("-s");
        actualCommand.add("workspace-write");
        actualCommand.add("-a");
        actualCommand.add("on-request");
        actualCommand.add("mcp-server");
        ProcessBuilder builder = new ProcessBuilder(actualCommand);
        builder.directory(workspaceRoot.toFile());
        builder.environment().putIfAbsent("OTEL_SDK_DISABLED", "true");
        try {
            process = builder.start();
            clearRecentStderr();
            startErrorDrainer(process);
        } catch (IOException e) {
            throw new CodexTransportException("failed to start codex mcp server", e);
        }
    }

    public synchronized boolean isAlive() {
        return process != null && process.isAlive();
    }

    public synchronized Process process() {
        if (!isAlive()) {
            String stderr = recentStderr();
            if (stderr.isBlank()) {
                throw new CodexTransportException("codex process is not running");
            }
            throw new CodexTransportException("codex process is not running: " + stderr);
        }
        return process;
    }

    private void startErrorDrainer(Process startedProcess) {
        Thread thread = Thread.ofPlatform()
                .daemon(true)
                .name("codex-stderr-drainer")
                .unstarted(() -> drainErrorStream(startedProcess));
        thread.start();
    }

    private void drainErrorStream(Process startedProcess) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(startedProcess.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendRecentStderr(line);
                System.err.println(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private synchronized void appendRecentStderr(String line) {
        if (!recentStderr.isEmpty()) {
            recentStderr.append(System.lineSeparator());
        }
        recentStderr.append(line);
        if (recentStderr.length() > MAX_STDERR_CHARS) {
            recentStderr.delete(0, recentStderr.length() - MAX_STDERR_CHARS);
        }
    }

    private synchronized void clearRecentStderr() {
        recentStderr.setLength(0);
    }

    private synchronized String recentStderr() {
        return recentStderr.toString();
    }
}
