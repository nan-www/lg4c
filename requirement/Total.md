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

### Start

你需要参考这个项目中给定的[顺序图](lg4c.puml)

codex 是运行在本机的进程。它只能在用户指定的目录下运行——通常是一个代码仓库的根目录。
且 codex 不能被授予完全访问权限。
### Work 

你需要参考这个项目中给定的[顺序图](lg4c_work.puml)

lg4c 初版需要支持对话记录的功能。写文件的时候尽可能的通过索引、NIO 等手段来提升性能。








