package wn.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine.Option;

public class GatewayLaunchOptions {
    @Option(names = "--home", defaultValue = "${sys:user.home}")
    Path home;

    @Option(names = "--workspace")
    Path workspace;

    @Option(names = "--codex-command", split = ",", defaultValue = "codex")
    List<String> codexCommand = new ArrayList<>();

    @Option(names = "--feishu-app-id")
    String feishuAppId;

    @Option(names = "--feishu-app-secret")
    String feishuAppSecret;

    @Option(names = "--feishu-base-url")
    String feishuBaseUrl;

    @Option(names = "--feishu-websocket-url")
    String feishuWebsocketUrl;

    @Option(names = "--feishu-reply-url")
    String feishuReplyUrl;

    @Option(names = "--allowed-user")
    List<String> allowedUsers = new ArrayList<>();

    @Option(names = "--allowed-chat")
    List<String> allowedChats = new ArrayList<>();

    @Option(names = "--logging-level", defaultValue = "INFO")
    String loggingLevel;
}
