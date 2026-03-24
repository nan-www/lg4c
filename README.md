# lg4c

`lg4c` 是一个本地网关守护进程，用来把飞书消息事件接入本地 Codex 工作区。

运行时它主要负责三件事：
- 通过官方 Lark SDK 接收飞书 `im.message.receive_v1` 事件
- 把收到的消息转换成本地工作区里的处理任务
- 通过现有的回复链路把处理结果回发到飞书

主运行模块是 `lg4c-core`，`example` 模块里保留了官方 Lark SDK 的独立示例代码。

## First Run

`lg4c` 的本地配置默认保存在 `~/.lg4c/config/application.yml`。

首次启动时，直接运行程序本身，不需要额外加 `daemon` 子命令。程序会交互式收集以下信息：
- `APPID`
- `APPSecret`
- `WorkSpace`

首次配置完成后，会自动写入本地配置并继续启动。

如果你使用 native 可执行文件，常用命令如下：

```bash
./lg4c-core/target/lg4c
./lg4c-core/target/lg4c daemon
./lg4c-core/target/lg4c doctor
```

如果你使用 JAR 方式运行，常用命令如下：

```bash
java -jar lg4c-core/target/quarkus-app/quarkus-run.jar
java -jar lg4c-core/target/quarkus-app/quarkus-run.jar daemon
java -jar lg4c-core/target/quarkus-app/quarkus-run.jar doctor
```

## Build

### Prerequisites

- JDK 25
- 如果要构建本地可执行文件，需要提前安装 GraalVM `native-image`
- 仓库已经自带 Maven Wrapper，不需要额外安装 Maven

### Run Tests

```bash
./mvnw -q -pl lg4c-core test
```

### Build JAR

默认构建会生成 Quarkus fast-jar 目录，主业务 JAR 的名字已经调整为 `lg4c.jar`：

```bash
./mvnw -q -pl lg4c-core clean package -DskipTests
```

主要产物：
- `lg4c-core/target/quarkus-app/quarkus-run.jar`
- `lg4c-core/target/quarkus-app/app/lg4c.jar`
- `lg4c-core/target/quarkus-app/lib/`

运行方式：

```bash
java -jar lg4c-core/target/quarkus-app/quarkus-run.jar doctor
```

如果你需要单文件 uber-jar：

```bash
./mvnw -q -pl lg4c-core clean package -DskipTests -Dquarkus.package.jar.type=uber-jar
```

主要产物：
- `lg4c-core/target/lg4c.jar`

### Build Native Executable

构建本地可执行文件：

```bash
./mvnw -q -pl lg4c-core package -Dnative -DskipTests
```

主要产物：
- `lg4c-core/target/lg4c`

运行方式：

```bash
./lg4c-core/target/lg4c doctor
```
