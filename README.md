# DashScope Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.alibaba/dashscope-sdk-java.svg)](https://central.sonatype.com/artifact/com.alibaba/dashscope-sdk-java)
[![Build and Test](https://github.com/dashscope/dashscope-sdk-java/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/dashscope/dashscope-sdk-java/actions/workflows/build-and-test.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

English | [中文](README_zh.md)

The Java SDK for [DashScope](https://dashscope.aliyuncs.com) models (Alibaba Cloud Model Studio).
This repository is on the **3.0 generation**: a single `DashScopeClient` entry point with a typed
resource tree, blocking SSE streaming, and an async mirror returning `CompletableFuture` /
RxJava `Flowable`. **Java 8+** is required at runtime.

> Upgrading from 2.x? The old APIs moved to `com.alibaba.dashscope.legacy_2x.*` and are deprecated
> for removal in 4.0 — see [Migrating from 2.x](#migrating-from-2x). Legacy documentation is kept
> in the [appendix](#legacy-2x-api-deprecated) at the end of this file.

## Installation

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>3.0.0</version>
</dependency>
```

## Quick Start

Set your API key first: `export DASHSCOPE_API_KEY=sk-...` (get one from the
[Model Studio console](https://bailian.console.aliyun.com/)).

### Synchronous

```java
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import java.util.Collections;

try (DashScopeClient client = DashScopeClient.builder().build()) {
  ChatCompletion result = client.chat().completions().create(
      ChatCompletionCreateParams.builder()
          .model("qwen-plus")
          .messages(Collections.singletonList(Message.user("Hello, how are you?")))
          .build());
  System.out.println(result.outputText());
  System.out.println("request_id: " + result.getRequestId());
}
```

### Streaming (SSE)

`createStreaming` returns a single-use `ResponseStream` — a blocking, typed iterator. Always
close it with try-with-resources so the underlying connection is released. Each chunk's `delta()`
carries the content increment.

```java
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.alibaba.dashscope.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.completions.Delta;
import com.alibaba.dashscope.types.messages.Message;
import java.util.Collections;

try (DashScopeClient client = DashScopeClient.builder().build()) {
  try (ResponseStream<ChatCompletionChunk> stream = client.chat().completions().createStreaming(
      ChatCompletionCreateParams.builder()
          .model("qwen-plus")
          .messages(Collections.singletonList(Message.user("Hello, how are you?")))
          .build())) {
    for (ChatCompletionChunk chunk : stream) {
      Delta delta = chunk.delta();
      if (delta != null && delta.getContent() != null) {
        System.out.print(delta.getContent());
      }
    }
  }
}
```

### Asynchronous

`DashScopeAsyncClient` mirrors the same resource tree: unary calls return `CompletableFuture`,
streaming returns RxJava `Flowable`. The client owns a daemon executor; try-with-resources shuts
it down.

```java
import com.alibaba.dashscope.DashScopeAsyncClient;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

try (DashScopeAsyncClient client = DashScopeAsyncClient.builder().build()) {
  CompletableFuture<ChatCompletion> future = client.chat().completions().create(
      ChatCompletionCreateParams.builder()
          .model("qwen-plus")
          .messages(Collections.singletonList(Message.user("Hello, how are you?")))
          .build());
  future.thenAccept(result -> System.out.println(result.outputText())).join();

  // Streaming: client.chat().completions().createStreaming(params) returns
  // Flowable<ChatCompletionChunk>; consume with blockingForEach or subscribe.
}
```

## Configuration

Every setting in the table below resolves through a config chain: **builder > JVM system
property > environment variable > `~/.dashscope/config.toml` > default**. The API key
additionally falls back to the 1.x key file (`DASHSCOPE_API_KEY_FILE_PATH` or
`~/.dashscope/api_key`), so `DashScopeClient.builder().build()` works with just
`DASHSCOPE_API_KEY` exported.

The config file is a flat `key = value` TOML subset; duration values are in **seconds**:

```toml
# ~/.dashscope/config.toml
api_key = "sk-..."
base_url = "https://dashscope.aliyuncs.com"
workspace = "llm-xxxx"
region = "cn-beijing"
timeout = 300          # seconds
max_retries = 2
ws_ping_interval = 0   # seconds, 0 = off
```

Client-level options (`DashScopeClient.builder()` / `DashScopeAsyncClient.builder()`):

| Builder method | Environment variable | Default | Description |
|---|---|---|---|
| `apiKey(...)` | `DASHSCOPE_API_KEY` | — | API key (config chain above) |
| `baseUrl(...)` | `DASHSCOPE_BASE_URL` / `DASHSCOPE_HTTP_BASE_URL` | region endpoint | HTTP base URL |
| `wsBaseUrl(...)` | `DASHSCOPE_WEBSOCKET_BASE_URL` | region endpoint | WebSocket base URL |
| `workspace(...)` | `DASHSCOPE_WORKSPACE` | — | Workspace id (required by some APIs, e.g. decision) |
| `region(...)` | `DASHSCOPE_REGION` | `cn-beijing` | MaaS gateway region |
| `timeout(...)` | `DASHSCOPE_TIMEOUT` | 5 minutes | Single-request timeout (`Duration`) |
| `maxRetries(...)` | `DASHSCOPE_MAX_RETRIES` | 2 | Automatic retries on retryable failures |
| `raiseOnError(...)` | — | `true` | `false` switches to 2.x-style `isError()` returns |
| `wsPingInterval(...)` | `DASHSCOPE_WS_PING_INTERVAL` | 0 (off) | WebSocket keepalive ping; 20–30 s behind NAT/proxies |
| `httpClient(...)` | — | SDK-owned | Inject your own `OkHttpClient` (you own its lifecycle) |
| `executor(...)` | — | SDK-owned | Inject the async executor (you own its lifecycle) |

Per-call overrides: every resource method has a `(params, RequestOptions)` overload. Null fields
inherit the client config.

```java
import com.alibaba.dashscope.RequestOptions;
import java.time.Duration;

RequestOptions options = RequestOptions.builder()
    .timeout(Duration.ofSeconds(30))
    .maxRetries(0)
    .header("X-Custom-Header", "value")
    .enableEncryption(true)   // DashScope encrypted HTTP channel for this call
    .build();
// client.chat().completions().create(params, options);
```

`RequestOptions` supports `timeout`, `maxRetries`, `apiKey`, `workspace`, extra `headers`,
`enableEncryption`, and `wsQueueCapacity` (bounded WebSocket event buffering).

HTTP wire logging (environment variable only): set `DASHSCOPE_NETWORK_LOGGING_LEVEL` to `BASIC`,
`HEADERS`, or `BODY` (default `NONE`). Credential headers are always redacted.

Telemetry opt-out: set `DASHSCOPE_DISABLE_SDK_HEADERS=1` (or `true`) to drop the
`x-dashscope-sdk-client` / `x-dashscope-sdk-session-id` tracking headers; `User-Agent` and
caller-set headers are unaffected.

## Capabilities

One entry point per capability on `client.` — each links to a minimal runnable example
(full index and run commands: [examples/README.md](examples/README.md)):

| Resource | API path | Example |
|---|---|---|
| Chat | `chat().completions().create / createStreaming` | [ChatCompletionsCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/chat/ChatCompletionsCreate.java) |
| Multimodal | `multimodal().chat().create / createStreaming` | [MultimodalChatCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/multimodal/MultimodalChatCreate.java) |
| Applications | `apps().completions().create / createStreaming` | [AppsCompletionsCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/apps/AppsCompletionsCreate.java) |
| Image generation | `images().generate / jobs().create / wait / fetch` | [ImagesGenerate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/images/ImagesGenerate.java) |
| Video generation | `videos().generate / jobs().*` | [VideosGenerate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/videos/VideosGenerate.java) |
| Embeddings | `embeddings().create` | [EmbeddingsCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/embeddings/EmbeddingsCreate.java) |
| Rerank | `rerank().create` | [RerankCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/rerank/RerankCreate.java) |
| Token counting | `tokenization().count` | [TokenizationCount](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/chat/TokenizationCount.java) |
| Speech recognition / synthesis | `audio().asr() / tts() / voices() / realtime()` | [audio examples](examples/README.md#01-atomic-capabilities-audio-omit-the-atomicaudio-prefix) |
| Fine-tuning | `fineTunes().list / get / create` | [FineTunesCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/finetunes/FineTunesCreate.java) |
| Deployments | `deployments().list / get / create` | [DeploymentsManage](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/deployments/DeploymentsManage.java) |
| Files | `files().upload / list / get / delete` | [FilesManage](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/files/FilesManage.java) |
| Models | `models().list / get` | [ModelsList](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/models/ModelsList.java) |
| TingWu | `tingwu().jobs().create / wait` | [TingWuJobsCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/audio/TingWuJobsCreate.java) |
| Decision model | `decision().create` | [DecisionCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/decision/DecisionCreate.java) |
| AgentStudio | `agentStudio()`: environments / agents / sessions / events | [AgentStudioQuickStart](examples/03_agent_recipes/com/alibaba/dashscope/examples/recipes/agentstudio/AgentStudioQuickStart.java) |

Also available: `client.wsConnection()` for multiplexing several audio tasks over one WebSocket
([AudioConnectionReuse](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/audio/AudioConnectionReuse.java)),
and `TokenizerFactory.qwen()` for offline local BPE tokenization without an API key
([TokenizationLocalCount](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/chat/TokenizationLocalCount.java)).

## Error Handling

3.0 throws **unchecked** exceptions rooted at `com.alibaba.dashscope.errors.DashScopeException`
(a `RuntimeException`) — no more checked `ApiException / NoApiKeyException / InputRequiredException`.
Each exception carries `code`, `statusCode`, `requestId`, `body`, and response `headers`.

- HTTP-status errors: `BadRequestException` (400), `AuthenticationException` (401),
  `PermissionDeniedException` (403), `NotFoundException` (404), `RequestTooLargeException` (413),
  `RateLimitException` (429), `InternalServerException` (5xx), `ServiceUnavailableException`,
  `GatewayTimeoutException`, and the generic `ApiStatusException`.
- Transport errors: `ApiConnectionException`, `ApiTimeoutException`.
- Business error codes (e.g. content filtering): `ContentFilterException` and other
  `DashScopeException` subclasses mapped by `Errors`.
- SDK-side misuse: `SdkException` / `IllegalArgumentException`.

Prefer the 2.x style of inspecting errors on the result? Build the client with
`raiseOnError(false)`: calls then return normally and `result.isError()` (plus `getCode()` /
`getMessage()`) reports business failures. In-stream error frames surface as a final
error-carrying chunk instead of throwing.

## Migrating from 2.x

3.0 is an intentional FQCN, source, and binary compatibility break. Legacy 2.x APIs other than
AgentStudio moved to `com.alibaba.dashscope.legacy_2x.*`, carry type-level `@Deprecated`
(`-Xlint:deprecation` shows details), and will be removed in 4.0. AgentStudio
(`com.alibaba.dashscope.applications.agentstudio.*`) and `shared` are not deprecated.

| 2.x class (`com.alibaba.dashscope.*`) | 3.0 equivalent (`client.*`) |
|---|---|
| `aigc.generation.Generation` | `chat().completions().create / createStreaming` |
| `aigc.multimodalconversation.MultiModalConversation` | `multimodal().chat().create / createStreaming` |
| `embeddings.TextEmbedding` / `BatchTextEmbedding` | `embeddings().create` |
| `rerank.TextReRank` | `rerank().create` |
| `aigc.imagesynthesis.ImageSynthesis` | `images().generate` + `jobs().create / wait / fetch` |
| `aigc.videosynthesis.VideoSynthesis` | `videos().generate` + `jobs().*` |
| `app.Application` | `apps().completions().create / createStreaming` |
| `decision.DecisionModel` | `decision().create` |
| `audio.asr.*` / `audio.tts.*` / `audio.qwen_tts_realtime.*` | `audio().asr() / tts() / realtime()` |
| `multimodal.tingwu.TingWu` | `tingwu().jobs().create / wait` |
| `tokenizers.*` | `tokenization().count` / `TokenizerFactory` |
| `assistants.*` / `threads.*` | no 3.0 equivalent yet — keep the `legacy_2x` imports |

Key behavior changes:

- **Exceptions**: checked `ApiException` family → unchecked `DashScopeException` family
  (see [Error Handling](#error-handling)); `raiseOnError(false)` restores result-style errors.
- **Streaming**: sync `streamCall` returning `Flowable` → `createStreaming` returning a typed
  `ResponseStream` (for-each, try-with-resources). The async client still exposes `Flowable`.
- **Client lifecycle**: per-capability `new Generation()` objects → one `DashScopeClient`
  (or `DashScopeAsyncClient`) in try-with-resources, sharing connection pool and config.
- **AgentStudio**: same business package, new FQCN
  `com.alibaba.dashscope.applications.agentstudio.*`; also reachable via `client.agentStudio()`.
  The 3.0 `baseUrl` does not apply to AgentStudio; `DASHSCOPE_AGENTSTUDIO_URL` overrides its
  endpoint.

The deprecated 2.x quick-start documentation (callback / `Flowable` / non-stream `Generation`
usage, legacy `DecisionModel` endpoint rules) is preserved in the
[legacy appendix](#legacy-2x-api-deprecated); archived 2.x samples are in
[examples/legacy_2x/](examples/legacy_2x/README.md).

## Examples

The [examples/](examples/README.md) project is organized in four layers: atomic capabilities,
advanced engineering (retry/circuit breaking, caching, local mock debugging), agent recipes
(tool calling, RAG, AgentStudio), and real-world scenario harnesses.

```bash
# Install the SDK from this checkout first (examples resolve version 3.0.0)
mvn install -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true

export DASHSCOPE_API_KEY=sk-...
mvn -q -f examples/pom.xml compile exec:java \
  -Dexec.mainClass=com.alibaba.dashscope.examples.atomic.chat.ChatCompletionsCreate
```

Every example also runs offline against MockWebServer (no API key, no charges):

```bash
mvn -f examples/pom.xml clean test
```

## Building from Source

The build baseline is JDK 8 (CI builds and tests on JDK 8; bytecode target 8). Building on
JDK 9+ also works — the pom's `jdk9-plus` profile auto-activates and compiles with `release 8`
API checking.

```bash
mvn clean test          # unit + mock tests
./lint.sh               # google-java-format check (100-column limit)
```

## License

[Apache License 2.0](LICENSE)

## Legacy 2.x API (deprecated)

> **Deprecated.** Everything below documents the legacy 2.x API. Since 3.0 these classes live
> under `com.alibaba.dashscope.legacy_2x.*`, carry type-level `@Deprecated`, and will be
> **removed in 4.0**. New code should use the 3.0 client above.

To keep existing 2.x code compiling on the 3.0 artifact, update imports:

- `com.alibaba.dashscope.aigc.*`, `com.alibaba.dashscope.embeddings.*`, … →
  `com.alibaba.dashscope.legacy_2x.aigc.*`, `com.alibaba.dashscope.legacy_2x.embeddings.*`, …
- AgentStudio is **not** deprecated: it moved to `com.alibaba.dashscope.applications.agentstudio.*`
  and is also reachable via `client.agentStudio()` on the 3.0 client.

Runnable 2.x samples are archived in [examples/legacy_2x/](examples/legacy_2x/README.md).

<details>
<summary>Expand the legacy 2.x quick-start documentation</summary>

### Generation

You can create a generation client simply by:

```java
Generation generation = new Generation();
```

The generation interface supports both stream and non-stream queries. These queries all accept `GenerationParam` as input, and return `GenerationResult` as output.

Here shows the usages of each method, with the examples of `qwen-turbo` model.

#### Support stream and non-stream mode, accept output from callback

```java
import com.alibaba.dashscope.legacy_2x.aigc.generation.Generation;
import com.alibaba.dashscope.legacy_2x.aigc.generation.GenerationParam;
import com.alibaba.dashscope.legacy_2x.aigc.generation.GenerationResult;
import com.alibaba.dashscope.legacy_2x.common.Message;
import com.alibaba.dashscope.legacy_2x.common.ResultCallback;
import com.alibaba.dashscope.legacy_2x.common.Role;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import java.util.Arrays;

public class Main {

  public static void main(String[] args) {
      Generation generation = new Generation();
      GenerationParam param = GenerationParam.builder()
          .apiKey(System.getenv("DASHSCOPE_API_KEY"))
          .model(Generation.Models.QWEN_TURBO)
          .messages(Arrays.asList(
              Message.builder()
                  .role(Role.USER.getValue())
                  .content("Hello, how are you?").build()
          )).build();

      class ReactCallback extends ResultCallback<GenerationResult> {

        @Override
        public void onEvent(GenerationResult message) {
          System.out.println(JsonUtils.toJson(message));
        }

        public void onComplete() {
          // TODO all messages received
        }

        public void onError(Exception e) {
          ApiException apiException = (ApiException) e;
          // TODO deal with exception
        }
      }

      try {
        generation.call(param, new ReactCallback());
      } catch (ApiException | NoApiKeyException | InputRequiredException e) {
        System.err.println("An error occurred: " + e.getMessage());
      }
    }
}
```

The Exception instance is an `ApiException` instance. This Exception may contain two parts:

- A `Status` instance. This instance carries a status_code (the http error code), a code (server error code), a message (server error message), the request id, and the usage information.
- If an exception occurs, the `ApiException` instance may only carry an `Exception` stack trace, you can deal with it as you usually do.

#### Stream only, accept by reactive io

```java
import com.alibaba.dashscope.legacy_2x.aigc.generation.Generation;
import com.alibaba.dashscope.legacy_2x.aigc.generation.GenerationParam;
import com.alibaba.dashscope.legacy_2x.aigc.generation.GenerationResult;
import com.alibaba.dashscope.legacy_2x.common.Message;
import com.alibaba.dashscope.legacy_2x.common.Role;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import io.reactivex.Flowable;
import java.util.Arrays;

public class Main {

  public static void main(String[] args) {
    Generation generation = new Generation();

    Message systemMsg = Message.builder()
        .role(Role.SYSTEM.getValue())
        .content("You are a helpful assistant.")
        .build();
    Message userMsg = Message.builder()
        .role(Role.USER.getValue())
        .content("Hello!")
        .build();
    GenerationParam param = GenerationParam.builder()
        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
        .model(Generation.Models.QWEN_TURBO)
        .messages(Arrays.asList(systemMsg, userMsg))
        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
        .build();

    try {
      Flowable<GenerationResult> result = generation.streamCall(param);
      result.blockingForEach(msg -> System.out.println(JsonUtils.toJson(msg)));
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.err.println("An error occurred: " + e.getMessage());
    }
  }
}
```

The `streamCall` method accepts a `GenerationParam`, and returns a `Flowable`, which you can get the streaming result by `blockingForEach`, and catch the exception by the try-catch block.

#### Non-stream only

```java
import com.alibaba.dashscope.legacy_2x.aigc.generation.Generation;
import com.alibaba.dashscope.legacy_2x.aigc.generation.GenerationParam;
import com.alibaba.dashscope.legacy_2x.aigc.generation.GenerationResult;
import com.alibaba.dashscope.legacy_2x.common.Message;
import com.alibaba.dashscope.legacy_2x.common.Role;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import java.util.Arrays;

public class Main {

  public static void main(String[] args) {
    Generation generation = new Generation();

    Message systemMsg = Message.builder()
        .role(Role.SYSTEM.getValue())
        .content("You are a helpful assistant.")
        .build();
    Message userMsg = Message.builder()
        .role(Role.USER.getValue())
        .content("Hello!")
        .build();
    GenerationParam param = GenerationParam.builder()
        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
        .model(Generation.Models.QWEN_TURBO)
        .messages(Arrays.asList(systemMsg, userMsg))
        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
        .build();

    try {
      GenerationResult result = generation.call(param);
      System.out.println(JsonUtils.toJson(result));
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.err.println("An error occurred: " + e.getMessage());
    }
  }
}
```

The `call` method accepts a `GenerationParam`, and returns a `GenerationResult`, you can also catch the exception with a try-catch block.

### Decision Model

The decision model answers a batch of multiple-choice (choice), yes/no (noul) and ordered-scale (score) questions against a given business state, see the [Decision Model API document](https://docs.bailian.console.aliyun.com/zh/model-studio/decision-model-api).

```java
import com.alibaba.dashscope.legacy_2x.decision.DecisionModel;
import com.alibaba.dashscope.legacy_2x.decision.DecisionModelParam;
import com.alibaba.dashscope.legacy_2x.decision.DecisionModelResult;
import com.alibaba.dashscope.legacy_2x.decision.DecisionQuestion;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

  public static void main(String[] args) {
    // The business state.
    Map<String, Object> state = new HashMap<>();
    state.put("ticket_id", "T-1001");
    state.put("content", "订单支付后超过 24 小时仍未到账，用户无法继续使用核心服务，要求立即处理。");

    // The questions.
    Map<String, String> options = new LinkedHashMap<>();
    options.put("billing", "支付、退款和账单问题");
    options.put("technical", "产品故障和集成问题");
    List<String> levels = Arrays.asList(
        "轻微问题，不影响功能",
        "部分功能受影响，但存在替代方案",
        "核心功能不可用，没有替代方案",
        "造成严重业务或安全影响");
    Map<String, DecisionQuestion> questions = new LinkedHashMap<>();
    questions.put("department", DecisionQuestion.choice("应该由哪个团队处理？", options));
    questions.put("escalate", DecisionQuestion.noul("是否需要立即通知值班人员？"));
    questions.put("severity", DecisionQuestion.score("这个问题有多严重？", levels));

    DecisionModelParam param = DecisionModelParam.builder()
        .model(DecisionModelParam.Models.DECISION_MODEL_PREVIEW)
        .state(state)
        .questions(questions)
        .build();

    try {
      // A workspace id is required: pass it here (new DecisionModel("your-workspace-id"))
      // or set the DASHSCOPE_WORKSPACE environment variable before running.
      DecisionModel decisionModel = new DecisionModel();
      DecisionModelResult result = decisionModel.call(param);
      System.out.println(JsonUtils.toJson(result));
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.err.println("An error occurred: " + e.getMessage());
    }
  }
}
```

The `call` method accepts a `DecisionModelParam`, and returns a `DecisionModelResult` whose `answers` are keyed by question id. Streaming is not supported. The API key is read from the `DASHSCOPE_API_KEY` environment variable by default, or set explicitly with `apiKey(...)`.

The endpoint is workspace-scoped: the base URL is composed as `https://{workspace}.{region}.maas.aliyuncs.com/compatible-mode/v1` (the `systemone` path is appended automatically). A workspace id is **required** — pass it to the constructor (`new DecisionModel(workspace)` or `new DecisionModel(workspace, region)`) or set the `DASHSCOPE_WORKSPACE` environment variable; otherwise constructing the client throws `IllegalArgumentException`. The region defaults to `cn-beijing` and can be set via `DASHSCOPE_API_REGION`.

To target a fully custom endpoint (for example the Token Plan gateway), use the static factory `DecisionModel.withBaseUrl(baseUrl)`. Note that `new DecisionModel(String)` treats its argument as a **workspace id**, not a base URL. You can also override the entire base URL with the `DASHSCOPE_DECISION_BASE_URL` environment variable; the SDK-wide `DASHSCOPE_HTTP_BASE_URL` is intentionally not read here.

</details>
