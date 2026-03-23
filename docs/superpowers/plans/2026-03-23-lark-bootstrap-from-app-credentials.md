# Lark Bootstrap From App Credentials Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let `lg4j-core` connect to the Feishu bot when the user provides only `APP_ID` and `APP_SECRET`, without requiring manual websocket or reply endpoint configuration.

**Architecture:** Keep the existing Quarkus-native-friendly thin adapter and add a small internal Lark bootstrap layer. That layer discovers the websocket endpoint, manages access tokens, sends replies through the official Feishu HTTP API, and exposes only narrow DTOs and interfaces to the rest of the gateway.

**Tech Stack:** Quarkus, Jackson, Quarkus WebSocket client, Quarkus REST client reactive, JUnit 5

---

## Chunk 1: Config And Bootstrap Contract

### Task 1: Make Feishu Endpoint Settings Optional

**Files:**
- Modify: `lg4j-core/src/main/java/wn/gateway/config/GatewayAppConfig.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/config/GatewayConfigStore.java`
- Modify: `lg4j-core/src/main/java/wn/cli/GatewayDaemonCommand.java`
- Test: `lg4j-core/src/test/java/wn/gateway/config/GatewayAppConfigTest.java`
- Test: `lg4j-core/src/test/java/wn/GatewayCommandTest.java`

- [ ] **Step 1: Write the failing config validation test**

Add a test case in `GatewayAppConfigTest` that builds a config with `feishuAppId` and `feishuAppSecret`, but without `feishuWebsocketUrl` and `feishuReplyUrl`, then calls `validate()` and expects success.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl lg4j-core -Dtest=GatewayAppConfigTest test`
Expected: FAIL because `validate()` still requires `gateway.feishu.websocket-url` and `gateway.feishu.reply-url`.

- [ ] **Step 3: Relax the config model**

Update `GatewayAppConfig` so:
- `feishuWebsocketUrl` and `feishuReplyUrl` are nullable advanced override fields.
- `validate()` requires only `app-id`, `app-secret`, `allowed-users`, and `allowed-chats`.
- The public meaning of the fields becomes "optional override", not "required user input".

- [ ] **Step 4: Update YAML load/save semantics**

Adjust `GatewayConfigStore` so:
- Missing `websocketUrl` and `replyUrl` deserialize cleanly.
- Saved config omits or leaves null advanced endpoint fields.
- Existing config files that still contain those keys remain readable.

- [ ] **Step 5: Update CLI bootstrap contract**

Change `GatewayDaemonCommand` so:
- `--feishu-websocket-url` and `--feishu-reply-url` are no longer part of the required bootstrap path.
- They remain available only as advanced escape hatches.
- `--bootstrap` help text and runtime errors reflect the new contract.

- [ ] **Step 6: Run tests to verify the contract**

Run: `mvn -pl lg4j-core -Dtest=GatewayAppConfigTest,GatewayCommandTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add lg4j-core/src/main/java/wn/gateway/config/GatewayAppConfig.java \
        lg4j-core/src/main/java/wn/gateway/config/GatewayConfigStore.java \
        lg4j-core/src/main/java/wn/cli/GatewayDaemonCommand.java \
        lg4j-core/src/test/java/wn/gateway/config/GatewayAppConfigTest.java \
        lg4j-core/src/test/java/wn/GatewayCommandTest.java
git commit -m "refactor: make lark endpoints optional in bootstrap config"
```

### Task 2: Add A Dedicated Lark Bootstrap Settings Surface

**Files:**
- Modify: `lg4j-core/src/main/java/wn/gateway/config/GatewayAppConfig.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/config/GatewayConfigStore.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/LarkEnvironment.java`
- Create: `lg4j-core/src/test/java/wn/gateway/config/GatewayConfigStoreTest.java`

- [ ] **Step 1: Write the failing config-store roundtrip test**

Add `GatewayConfigStoreTest` that saves and reloads config containing only app credentials plus optional advanced overrides, then asserts values survive roundtrip.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl lg4j-core -Dtest=GatewayConfigStoreTest test`
Expected: FAIL because no dedicated environment/override surface exists yet.

- [ ] **Step 3: Introduce an internal environment abstraction**

Create `LarkEnvironment` to centralize:
- Feishu base domain
- Websocket endpoint override
- Reply API override
- Future expert-level knobs

Use it from `GatewayAppConfig` instead of scattering endpoint override meaning across raw strings.

- [ ] **Step 4: Wire YAML serialization**

Teach `GatewayConfigStore` to read/write the new environment structure while preserving backward compatibility with old `websocketUrl` and `replyUrl` keys.

- [ ] **Step 5: Run focused tests**

Run: `mvn -pl lg4j-core -Dtest=GatewayConfigStoreTest,GatewayAppConfigTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add lg4j-core/src/main/java/wn/gateway/config/GatewayAppConfig.java \
        lg4j-core/src/main/java/wn/gateway/config/GatewayConfigStore.java \
        lg4j-core/src/main/java/wn/gateway/lark/LarkEnvironment.java \
        lg4j-core/src/test/java/wn/gateway/config/GatewayConfigStoreTest.java \
        lg4j-core/src/test/java/wn/gateway/config/GatewayAppConfigTest.java
git commit -m "refactor: centralize lark environment overrides"
```

## Chunk 2: Discovery And Reply Transport

### Task 3: Add Feishu Endpoint Discovery Client

**Files:**
- Create: `lg4j-core/src/main/java/wn/gateway/lark/bootstrap/LarkEndpointDiscoveryApi.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/bootstrap/LarkEndpointDiscoveryRequest.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/bootstrap/LarkEndpointDiscoveryResponse.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/bootstrap/LarkWsBootstrapResult.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/bootstrap/LarkEndpointDiscoveryService.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/bootstrap/DefaultLarkEndpointDiscoveryService.java`
- Test: `lg4j-core/src/test/java/wn/gateway/lark/LarkEndpointDiscoveryServiceTest.java`

- [ ] **Step 1: Write the failing discovery service test**

Add a unit test that stubs the discovery API response and asserts the service returns:
- websocket URL
- connection metadata needed for runtime behavior
- server-provided client config if present

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl lg4j-core -Dtest=LarkEndpointDiscoveryServiceTest test`
Expected: FAIL because discovery service types do not exist.

- [ ] **Step 3: Implement a thin discovery API**

Create a Quarkus REST client for Feishu endpoint discovery. Keep request/response DTOs narrow and Jackson-based. Do not mirror the full SDK type graph.

- [ ] **Step 4: Implement the service wrapper**

`DefaultLarkEndpointDiscoveryService` should:
- call the discovery API using app credentials
- validate non-success responses
- map the payload into `LarkWsBootstrapResult`
- honor manual websocket override when configured

- [ ] **Step 5: Run tests**

Run: `mvn -pl lg4j-core -Dtest=LarkEndpointDiscoveryServiceTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add lg4j-core/src/main/java/wn/gateway/lark/bootstrap \
        lg4j-core/src/test/java/wn/gateway/lark/LarkEndpointDiscoveryServiceTest.java
git commit -m "feat: add feishu websocket endpoint discovery"
```

### Task 4: Replace Reply Proxy With Official Reply API And Token Provider

**Files:**
- Create: `lg4j-core/src/main/java/wn/gateway/lark/auth/LarkTenantAccessTokenApi.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/auth/LarkTenantAccessTokenRequest.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/auth/LarkTenantAccessTokenResponse.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/auth/LarkAccessTokenProvider.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/auth/CachedLarkAccessTokenProvider.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/lark/LarkReplyApi.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/lark/DefaultLarkReplyApiFactory.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/lark/LarkReplyRequest.java`
- Create: `lg4j-core/src/test/java/wn/gateway/lark/CachedLarkAccessTokenProviderTest.java`
- Create: `lg4j-core/src/test/java/wn/gateway/lark/LarkReplyApiContractTest.java`

- [ ] **Step 1: Write the failing token-provider test**

Add tests for:
- cache hit before expiry
- refresh after expiry threshold
- failure propagation when token fetch fails

- [ ] **Step 2: Write the failing reply API contract test**

Add a test that asserts outbound reply transport uses bearer token authorization and the official Feishu request body shape instead of the current custom proxy headers.

- [ ] **Step 3: Run tests to verify failure**

Run: `mvn -pl lg4j-core -Dtest=CachedLarkAccessTokenProviderTest,LarkReplyApiContractTest test`
Expected: FAIL because the current reply path still sends `X-Feishu-App-Id` and `X-Feishu-App-Secret`.

- [ ] **Step 4: Implement token retrieval and caching**

Build `CachedLarkAccessTokenProvider` with:
- in-memory cached token
- expiration margin
- synchronized refresh
- clear failure messages for credential and network failures

- [ ] **Step 5: Rewrite reply transport**

Change `LarkReplyApi` and `DefaultLarkReplyApiFactory` so replies are sent directly to the official Feishu endpoint using:
- `Authorization: Bearer <tenant_access_token>`
- the official message reply payload

Remove dependence on the conceptual `reply-url` bootstrap requirement. Keep only an expert override path in `LarkEnvironment`.

- [ ] **Step 6: Run focused tests**

Run: `mvn -pl lg4j-core -Dtest=CachedLarkAccessTokenProviderTest,LarkReplyApiContractTest,LarkGatewayClientFactoryTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add lg4j-core/src/main/java/wn/gateway/lark/auth \
        lg4j-core/src/main/java/wn/gateway/lark/LarkReplyApi.java \
        lg4j-core/src/main/java/wn/gateway/lark/DefaultLarkReplyApiFactory.java \
        lg4j-core/src/main/java/wn/gateway/lark/LarkReplyRequest.java \
        lg4j-core/src/test/java/wn/gateway/lark/CachedLarkAccessTokenProviderTest.java \
        lg4j-core/src/test/java/wn/gateway/lark/LarkReplyApiContractTest.java \
        lg4j-core/src/test/java/wn/gateway/lark/LarkGatewayClientFactoryTest.java
git commit -m "feat: send replies through official feishu api"
```

## Chunk 3: WebSocket Runtime Behavior

### Task 5: Teach The Gateway Client To Bootstrap And Maintain The WebSocket Session

**Files:**
- Modify: `lg4j-core/src/main/java/wn/gateway/lark/QuarkusLarkGatewayClient.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/lark/DefaultLarkGatewayClientFactory.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/lark/DefaultLarkWebSocketConnector.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/runtime/LarkClientRuntimeConfig.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/runtime/LarkHeartbeatScheduler.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/runtime/LarkReconnectPolicy.java`
- Test: `lg4j-core/src/test/java/wn/gateway/lark/QuarkusLarkGatewayClientTest.java`
- Test: `lg4j-core/src/test/java/wn/gateway/lark/QuarkusLarkGatewayClientVirtualThreadTest.java`

- [ ] **Step 1: Write the failing gateway client tests**

Add tests for:
- startup fetches the websocket endpoint through discovery
- incoming message parsing still extracts the current minimal inbound fields
- websocket close/error triggers reconnect scheduling
- heartbeat is scheduled from server config or defaults

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl lg4j-core -Dtest=QuarkusLarkGatewayClientTest,QuarkusLarkGatewayClientVirtualThreadTest test`
Expected: FAIL because the current client connects directly and has no bootstrap or reconnect policy abstraction.

- [ ] **Step 3: Refactor the client around injected collaborators**

Make `QuarkusLarkGatewayClient` depend on:
- `LarkEndpointDiscoveryService`
- `LarkAccessTokenProvider`
- reconnect policy
- heartbeat scheduler

Keep the existing inbound parsing strategy narrow and Jackson-tree-based.

- [ ] **Step 4: Implement minimal lifecycle behavior**

Add:
- startup endpoint discovery
- connect with discovered URL
- heartbeat timer
- reconnect on close/error with bounded backoff
- clean shutdown of timers and websocket session

Avoid generic event-model binding and keep all message handling in the current thin parsing style.

- [ ] **Step 5: Run focused tests**

Run: `mvn -pl lg4j-core -Dtest=QuarkusLarkGatewayClientTest,QuarkusLarkGatewayClientVirtualThreadTest,QuarkusNetworkStackTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add lg4j-core/src/main/java/wn/gateway/lark \
        lg4j-core/src/main/java/wn/gateway/lark/runtime \
        lg4j-core/src/test/java/wn/gateway/lark/QuarkusLarkGatewayClientTest.java \
        lg4j-core/src/test/java/wn/gateway/lark/QuarkusLarkGatewayClientVirtualThreadTest.java \
        lg4j-core/src/test/java/wn/gateway/lark/QuarkusNetworkStackTest.java
git commit -m "feat: bootstrap and maintain feishu websocket sessions"
```

### Task 6: Add Explicit Error Classification For Bootstrap Failures

**Files:**
- Create: `lg4j-core/src/main/java/wn/gateway/lark/LarkBootstrapException.java`
- Create: `lg4j-core/src/main/java/wn/gateway/lark/LarkErrorCategory.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/lark/bootstrap/DefaultLarkEndpointDiscoveryService.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/lark/auth/CachedLarkAccessTokenProvider.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/lark/QuarkusLarkGatewayClient.java`
- Test: `lg4j-core/src/test/java/wn/gateway/lark/LarkBootstrapExceptionTest.java`

- [ ] **Step 1: Write the failing exception-mapping test**

Cover at least:
- invalid credentials
- missing bot permissions
- network timeout / DNS failure
- malformed platform response

- [ ] **Step 2: Run test to verify failure**

Run: `mvn -pl lg4j-core -Dtest=LarkBootstrapExceptionTest test`
Expected: FAIL because no typed bootstrap exception exists.

- [ ] **Step 3: Implement typed error mapping**

Create a small exception hierarchy that preserves:
- operator-facing error category
- safe message for CLI output
- original cause for logs

- [ ] **Step 4: Wire the mapping**

Use the typed exception flow in discovery, token fetch, and websocket startup so caller code can distinguish configuration from platform and network failures.

- [ ] **Step 5: Run tests**

Run: `mvn -pl lg4j-core -Dtest=LarkBootstrapExceptionTest,LarkEndpointDiscoveryServiceTest,CachedLarkAccessTokenProviderTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add lg4j-core/src/main/java/wn/gateway/lark/LarkBootstrapException.java \
        lg4j-core/src/main/java/wn/gateway/lark/LarkErrorCategory.java \
        lg4j-core/src/main/java/wn/gateway/lark/bootstrap/DefaultLarkEndpointDiscoveryService.java \
        lg4j-core/src/main/java/wn/gateway/lark/auth/CachedLarkAccessTokenProvider.java \
        lg4j-core/src/main/java/wn/gateway/lark/QuarkusLarkGatewayClient.java \
        lg4j-core/src/test/java/wn/gateway/lark/LarkBootstrapExceptionTest.java
git commit -m "feat: classify feishu bootstrap failures"
```

## Chunk 4: UX, Migration, And Verification

### Task 7: Update Doctor/Bootstrap UX And Backward Compatibility

**Files:**
- Modify: `lg4j-core/src/main/java/wn/cli/GatewayDoctorCommand.java`
- Modify: `lg4j-core/src/main/java/wn/cli/GatewayDaemonCommand.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/bootstrap/BootstrapService.java`
- Modify: `lg4j-core/src/main/java/wn/gateway/config/GatewayConfigStore.java`
- Test: `lg4j-core/src/test/java/wn/GatewayCommandTest.java`
- Test: `lg4j-core/src/test/java/wn/gateway/bootstrap/BootstrapServiceTest.java`

- [ ] **Step 1: Write the failing doctor/bootstrap tests**

Add coverage for:
- bootstrap success with only `app-id` and `app-secret`
- doctor reporting a successful bootstrap chain
- old config files with endpoint keys loading with deprecation messaging

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl lg4j-core -Dtest=GatewayCommandTest,BootstrapServiceTest test`
Expected: FAIL because doctor/bootstrap still model websocket/reply URLs as primary inputs.

- [ ] **Step 3: Implement user-facing messaging**

Update bootstrap and doctor output to say:
- the only required Feishu credentials are `APP_ID` and `APP_SECRET`
- endpoint discovery and token management are automatic
- manual endpoints are expert overrides

- [ ] **Step 4: Implement compatibility warning path**

When legacy config includes `websocketUrl` or `replyUrl`:
- continue honoring them
- print a clear deprecation warning
- suggest re-running bootstrap to simplify config

- [ ] **Step 5: Run focused tests**

Run: `mvn -pl lg4j-core -Dtest=GatewayCommandTest,BootstrapServiceTest,GatewayConfigStoreTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add lg4j-core/src/main/java/wn/cli/GatewayDoctorCommand.java \
        lg4j-core/src/main/java/wn/cli/GatewayDaemonCommand.java \
        lg4j-core/src/main/java/wn/gateway/bootstrap/BootstrapService.java \
        lg4j-core/src/main/java/wn/gateway/config/GatewayConfigStore.java \
        lg4j-core/src/test/java/wn/GatewayCommandTest.java \
        lg4j-core/src/test/java/wn/gateway/bootstrap/BootstrapServiceTest.java \
        lg4j-core/src/test/java/wn/gateway/config/GatewayConfigStoreTest.java
git commit -m "feat: simplify bootstrap UX for feishu credentials"
```

### Task 8: Run The Full Core Verification Pass

**Files:**
- Modify: `lg4j-core/src/test/java/wn/gateway/lark/QuarkusNetworkStackTest.java`
- Optionally modify: `lg4j-core/README.md`
- Optionally modify: `docs/superpowers/specs/` if a design doc is later added

- [ ] **Step 1: Add final guardrails to the network stack test**

Extend `QuarkusNetworkStackTest` to assert:
- no accidental `java.net.http`
- no `com.larksuite.oapi` dependency appears in `lg4j-core/pom.xml`
- Quarkus-native-friendly client dependencies remain the foundation

- [ ] **Step 2: Run the module test suite**

Run: `mvn -pl lg4j-core test`
Expected: PASS

- [ ] **Step 3: Run native-oriented verification**

Run: `mvn -pl lg4j-core -Dnative test`
Expected: PASS, or if the environment cannot build native locally, capture the exact blocker and do not claim native success.

- [ ] **Step 4: Commit**

```bash
git add lg4j-core/src/test/java/wn/gateway/lark/QuarkusNetworkStackTest.java \
        lg4j-core/pom.xml \
        lg4j-core/README.md
git commit -m "test: lock lark integration to native-friendly dependencies"
```

## Execution Notes

- Keep all new Feishu DTOs narrow and explicit. Do not import or recreate the official SDK's large generated model tree.
- Preserve the current inbound message extraction style in `QuarkusLarkGatewayClient`: only parse the fields the gateway actually needs.
- Prefer composition over growing `QuarkusLarkGatewayClient` into a monolith. Discovery, token caching, heartbeat, and reconnect policy should each have their own class.
- If Feishu protocol details force binary frames or explicit ACK behavior beyond the current websocket client assumptions, add that support in a dedicated runtime component rather than spreading protocol state into business code.
- Do not add Gson, Apache HttpClient, OkHttp, Guava, or the official Lark SDK to `lg4j-core`.

## Suggested Execution Order

1. Task 1
2. Task 2
3. Task 3
4. Task 4
5. Task 5
6. Task 6
7. Task 7
8. Task 8
