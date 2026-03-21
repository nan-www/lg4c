# What I want
我希望实现一个可以运行在本地电脑的网关。这个网关需要作为守护进程静默运行在PC上。

## Building environment
> 我已经在这个项目里给你搭建好了开发环境以及所需要的依赖
1. Program language: Java(JDK25)
2. Output: 可执行文件——借助 GraalVM
3. Framework: Quarkus
4. Target system: Mac OS (Arm)

## Why I want to build it

实现 用户 -> 飞书机器人 -> 飞书 server -> gateway（这个项目）-[MCP protocal]> code agent(codex)

##  Gateway Lifecycle
> 它需要作为一个守护进程运行在系统上。e.g. Mac os: caffeinate ${process}

### Before Start

校验本地是否存在 codex,如果没有打印错误日志到控制台并退出。

### After Start
1. 与飞书 server 建立 websocket 链接。如果失败重试 3 次后报错退出
2. 与本地的 codex MCP server 建连（初版先考虑只启动一个 codex）
3. 如果没有则需要用户给一个文件路径来作为 codex MCP server 的根目录
4. 通过shell拉起 codex MCP server 并链接

### 





