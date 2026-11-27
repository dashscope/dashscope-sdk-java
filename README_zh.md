# DashScope Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.alibaba/dashscope-sdk-java.svg)](https://central.sonatype.com/artifact/com.alibaba/dashscope-sdk-java)
[![Build and Test](https://github.com/dashscope/dashscope-sdk-java/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/dashscope/dashscope-sdk-java/actions/workflows/build-and-test.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

[English](README.md) | 中文

[DashScope](https://dashscope.aliyuncs.com)（阿里云百炼）模型的 Java SDK。
本仓库为 **3.0 新代际**：统一的 `DashScopeClient` 入口 + 类型化资源树，支持阻塞式 SSE 流式，
并提供返回 `CompletableFuture` / RxJava `Flowable` 的异步镜像客户端。运行期要求 **Java 8+**。

> 从 2.x 升级？旧 API 已迁至 `com.alibaba.dashscope.legacy_2x.*`，均已废弃并将在 4.0 移除——
> 见[从 2.x 迁移](#从-2x-迁移)。旧版文档保存在文末「旧版 2.x API」附录。

## 安装

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>3.0.0</version>
</dependency>
```

## 快速开始

先设置 API Key：`export DASHSCOPE_API_KEY=sk-...`（在[百炼控制台](https://bailian.console.aliyun.com/)获取）。

### 同步调用

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
          .messages(Collections.singletonList(Message.user("你好，最近怎么样？")))
          .build());
  System.out.println(result.outputText());
  System.out.println("request_id: " + result.getRequestId());
}
```

### 流式调用（SSE）

`createStreaming` 返回一次性的 `ResponseStream`——阻塞式类型化迭代器。请务必用
try-with-resources 关闭以释放底层连接。每个 chunk 的 `delta()` 携带内容增量。

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
          .messages(Collections.singletonList(Message.user("你好，最近怎么样？")))
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

### 异步调用

`DashScopeAsyncClient` 提供同一资源树：普通调用返回 `CompletableFuture`，流式返回 RxJava
`Flowable`。客户端持有守护线程池，try-with-resources 会自动关闭。

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
          .messages(Collections.singletonList(Message.user("你好，最近怎么样？")))
          .build());
  future.thenAccept(result -> System.out.println(result.outputText())).join();

  // 流式：client.chat().completions().createStreaming(params) 返回
  // Flowable<ChatCompletionChunk>，可用 blockingForEach 消费或 subscribe。
}
```

## 配置

下表所列配置项都走配置链：**Builder 显式参数 > JVM 系统属性 > 环境变量 >
`~/.dashscope/config.toml` > 默认值**（`wsBaseUrl` 没有 config-file 键；`raiseOnError`、
`httpClient`、`executor` 仅支持 Builder 注入）。API Key 还会额外回退到 1.x 密钥文件
（`DASHSCOPE_API_KEY_FILE_PATH` 或 `~/.dashscope/api_key`），因此只需导出
`DASHSCOPE_API_KEY`，`DashScopeClient.builder().build()` 即可直接使用。

配置文件为扁平的 `key = value` TOML 子集；时长类取值单位为**秒**：

```toml
# ~/.dashscope/config.toml
api_key = "sk-..."
base_url = "https://dashscope.aliyuncs.com"
workspace = "llm-xxxx"
region = "cn-beijing"
timeout = 300          # 秒
max_retries = 2
ws_ping_interval = 0   # 秒，0 = 关闭
```

客户端级配置（`DashScopeClient.builder()` / `DashScopeAsyncClient.builder()`）：

| Builder 方法 | 环境变量 | 默认值 | 说明 |
|---|---|---|---|
| `apiKey(...)` | `DASHSCOPE_API_KEY` | — | API Key（走上述配置链） |
| `baseUrl(...)` | `DASHSCOPE_BASE_URL` / `DASHSCOPE_HTTP_BASE_URL` | 区域端点 | HTTP base URL |
| `wsBaseUrl(...)` | `DASHSCOPE_WEBSOCKET_BASE_URL` | 区域端点 | WebSocket base URL |
| `workspace(...)` | `DASHSCOPE_WORKSPACE` | — | 工作空间 id（部分 API 必填，如决策模型） |
| `region(...)` | `DASHSCOPE_REGION` | `cn-beijing` | MaaS 网关区域 |
| `timeout(...)` | `DASHSCOPE_TIMEOUT` | 5 分钟 | 单请求超时（`Duration`） |
| `maxRetries(...)` | `DASHSCOPE_MAX_RETRIES` | 2 | 可重试失败的自动重试次数 |
| `raiseOnError(...)` | — | `true` | `false` 切换为 2.x 风格的 `isError()` 返回 |
| `wsPingInterval(...)` | `DASHSCOPE_WS_PING_INTERVAL` | 0（关闭） | WebSocket 保活 ping；NAT/代理后建议 20–30 秒 |
| `httpClient(...)` | — | SDK 持有 | 注入自有 `OkHttpClient`（生命周期由调用方管理） |
| `executor(...)` | — | SDK 持有 | 注入异步线程池（生命周期由调用方管理） |

单次调用覆盖：每个资源方法都有 `(params, RequestOptions)` 重载，未设置的字段继承客户端配置。

```java
import com.alibaba.dashscope.RequestOptions;
import java.time.Duration;

RequestOptions options = RequestOptions.builder()
    .timeout(Duration.ofSeconds(30))
    .maxRetries(0)
    .header("X-Custom-Header", "value")
    .enableEncryption(true)   // 本次调用启用 DashScope 加密 HTTP 通道
    .build();
// client.chat().completions().create(params, options);
```

`RequestOptions` 支持 `timeout`、`maxRetries`、`apiKey`、`workspace`、额外 `headers`、
`enableEncryption` 和 `wsQueueCapacity`（WebSocket 事件有界缓冲）。

HTTP 报文日志（仅环境变量）：将 `DASHSCOPE_NETWORK_LOGGING_LEVEL` 设为 `BASIC`、`HEADERS` 或
`BODY`（默认 `NONE`）。凭证类请求头始终脱敏。

遥测关闭：将 `DASHSCOPE_DISABLE_SDK_HEADERS` 设为 `1`（或 `true`）可去掉
`x-dashscope-sdk-client` / `x-dashscope-sdk-session-id` 跟踪头；`User-Agent` 与调用方自设头
不受影响。

## 能力一览

`client.` 上每个能力一个入口——均链接到最小可运行示例
（完整索引与运行命令见 [examples/README_zh.md](examples/README_zh.md)）：

| 资源 | API 路径 | 示例 |
|---|---|---|
| 文本对话 | `chat().completions().create / createStreaming` | [ChatCompletionsCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/chat/ChatCompletionsCreate.java) |
| 多模态 | `multimodal().chat().create / createStreaming` | [MultimodalChatCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/multimodal/MultimodalChatCreate.java) |
| 应用调用 | `apps().completions().create / createStreaming` | [AppsCompletionsCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/apps/AppsCompletionsCreate.java) |
| 文生图 | `images().generate / jobs().create / wait / fetch` | [ImagesGenerate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/images/ImagesGenerate.java) |
| 文生视频 | `videos().generate / jobs().*` | [VideosGenerate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/videos/VideosGenerate.java) |
| 向量化 | `embeddings().create` | [EmbeddingsCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/embeddings/EmbeddingsCreate.java) |
| 重排序 | `rerank().create` | [RerankCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/rerank/RerankCreate.java) |
| Token 计数 | `tokenization().count` | [TokenizationCount](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/chat/TokenizationCount.java) |
| 语音识别 / 合成 | `audio().asr() / tts() / voices() / realtime()` | [音频示例](examples/README_zh.md) |
| 模型微调 | `fineTunes().list / get / create` | [FineTunesCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/finetunes/FineTunesCreate.java) |
| 模型部署 | `deployments().list / get / create` | [DeploymentsManage](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/deployments/DeploymentsManage.java) |
| 文件管理 | `files().upload / list / get / delete` | [FilesManage](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/files/FilesManage.java) |
| 模型列表 | `models().list / get` | [ModelsList](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/models/ModelsList.java) |
| 听悟 | `tingwu().jobs().create / wait` | [TingWuJobsCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/audio/TingWuJobsCreate.java) |
| 决策模型 | `decision().create` | [DecisionCreate](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/decision/DecisionCreate.java) |
| AgentStudio | `agentStudio()`：environments / agents / sessions / events | [AgentStudioQuickStart](examples/03_agent_recipes/com/alibaba/dashscope/examples/recipes/agentstudio/AgentStudioQuickStart.java) |

另有：`client.wsConnection()` 可在一条 WebSocket 上复用多个音频任务
（[AudioConnectionReuse](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/audio/AudioConnectionReuse.java)）；
`TokenizerFactory.qwen()` 提供无需 API Key 的离线本地 BPE 分词
（[TokenizationLocalCount](examples/01_atomic_capabilities/com/alibaba/dashscope/examples/atomic/chat/TokenizationLocalCount.java)）。

## 错误处理

3.0 抛出以 `com.alibaba.dashscope.errors.DashScopeException`（`RuntimeException`）为根的
**unchecked** 异常——不再是 checked 的 `ApiException / NoApiKeyException / InputRequiredException`。
每个异常携带 `code`、`statusCode`、`requestId`、`body` 和响应 `headers`。

- HTTP 状态码错误：`BadRequestException`（400）、`AuthenticationException`（401）、
  `PermissionDeniedException`（403）、`NotFoundException`（404）、`RequestTooLargeException`（413）、
  `RateLimitException`（429）、`InternalServerException`（500）、`ServiceUnavailableException`、
  `GatewayTimeoutException`，以及通用的 `ApiStatusException`。
- 传输层错误：`ApiConnectionException`、`ApiTimeoutException`。
- 业务错误码（如内容审核）：`ContentFilterException` 及其他由 `Errors` 映射的
  `DashScopeException` 子类。
- SDK 侧误用：`SdkException` / `IllegalArgumentException`。

更习惯 2.x 的「在结果上检查错误」？用 `raiseOnError(false)` 构建客户端：调用正常返回，
通过 `result.isError()`（配合 `getCode()` / `getMessage()`）判断业务失败；流内错误帧会以
末尾携带错误的 chunk 呈现，而不是抛异常。

## 从 2.x 迁移

3.0 是有意的 FQCN、源码与二进制破坏性变更。除 AgentStudio 外的旧 2.x API 已迁至
`com.alibaba.dashscope.legacy_2x.*`，均带类型级 `@Deprecated`（`-Xlint:deprecation` 显示详情），
并将在 4.0 移除。AgentStudio（`com.alibaba.dashscope.applications.agentstudio.*`）与
`shared` 不废弃。

| 2.x 类（`com.alibaba.dashscope.*`） | 3.0 等价入口（`client.*`） |
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
| `assistants.*` / `threads.*` | 暂无 3.0 等价物——保留 `legacy_2x` imports |

关键行为变化：

- **异常**：checked 的 `ApiException` 家族 → unchecked 的 `DashScopeException` 家族
  （见[错误处理](#错误处理)）；`raiseOnError(false)` 可恢复结果式错误。
- **流式**：同步 `streamCall` 返回 `Flowable` → `createStreaming` 返回类型化
  `ResponseStream`（for-each + try-with-resources）。异步客户端仍提供 `Flowable`。
- **客户端生命周期**：按能力 `new Generation()` → 一个 try-with-resources 的
  `DashScopeClient`（或 `DashScopeAsyncClient`），共享连接池与配置。
- **AgentStudio**：同一业务包，新 FQCN 为
  `com.alibaba.dashscope.applications.agentstudio.*`；也可经 `client.agentStudio()` 访问。
  3.0 的 `baseUrl` 不作用于 AgentStudio；`DASHSCOPE_AGENTSTUDIO_URL` 可单独指定其端点。

已废弃的 2.x 快速开始文档（回调 / `Flowable` / 非流式 `Generation` 用法、旧版
`DecisionModel` 端点规则）保存在文末「旧版 2.x API」附录；2.x 示例归档于
[examples/legacy_2x/](examples/legacy_2x/README.md)。

## 示例

[examples/](examples/README_zh.md) 工程按四层组织：原子能力、进阶工程（重试/熔断、缓存、
本地 Mock 调试）、Agent 配方（工具调用、RAG、AgentStudio）、真实场景 Harness。

```bash
# 先从本仓库安装 SDK（示例工程解析 3.0.0 版本）
mvn install -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true

export DASHSCOPE_API_KEY=sk-...
mvn -q -f examples/pom.xml compile exec:java \
  -Dexec.mainClass=com.alibaba.dashscope.examples.atomic.chat.ChatCompletionsCreate
```

所有示例也可离线跑在 MockWebServer 上（无需 API Key、不产生费用）：

```bash
mvn -f examples/pom.xml clean test
```

## 从源码构建

构建基线为 JDK 8（CI 在 JDK 8 上构建与测试；字节码 target 8）。在 JDK 9+ 上构建同样可行——
pom 的 `jdk9-plus` profile 会自动激活并以 `release 8` 做 API 校验。

```bash
mvn clean test          # 单元 + Mock 测试
./lint.sh               # google-java-format 检查（100 列限制）
```

## 许可证

[Apache License 2.0](LICENSE)

## 旧版 2.x API（已废弃）

> **已废弃。** 以下内容为旧版 2.x API 文档。自 3.0 起，这些类位于
> `com.alibaba.dashscope.legacy_2x.*`，均带类型级 `@Deprecated`，并将在 **4.0 移除**。
> 新代码请使用上文的 3.0 客户端。

要让存量 2.x 代码在 3.0 artifact 上继续编译，请调整 imports：

- `com.alibaba.dashscope.aigc.*`、`com.alibaba.dashscope.embeddings.*` 等 →
  `com.alibaba.dashscope.legacy_2x.aigc.*`、`com.alibaba.dashscope.legacy_2x.embeddings.*` 等
- AgentStudio **不废弃**：迁至 `com.alibaba.dashscope.applications.agentstudio.*`，
  也可通过 3.0 客户端的 `client.agentStudio()` 访问。

可运行的 2.x 示例归档于 [examples/legacy_2x/](examples/legacy_2x/README.md)。

<details>
<summary>展开旧版 2.x 快速开始文档</summary>

### 文本生成

您可以通过以下方式创建文本生成客户端：

```java
Generation generation = new Generation();
```

文本生成接口支持流式和非流式查询。这些查询都接受 `GenerationParam` 作为输入，并返回 `GenerationResult` 作为输出。

以下是各方法的使用示例，以 `qwen-turbo` 模型为例。

#### 支持流式和非流式模式，通过回调接收输出

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
          // TODO 所有消息已接收
        }

        public void onError(Exception e) {
          ApiException apiException = (ApiException) e;
          // TODO 处理异常
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

异常实例是一个 `ApiException` 实例。该异常可能包含两部分：

- 一个 `Status` 实例。该实例包含 status_code（HTTP 错误码）、code（服务器错误码）、message（服务器错误信息）、请求 ID 和使用信息。
- 如果发生异常，`ApiException` 实例可能只包含一个 `Exception` 堆栈跟踪，您可以像平常一样处理它。

#### 仅流式模式，通过响应式 IO 接收

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

`streamCall` 方法接受 `GenerationParam`，并返回一个 `Flowable`，您可以通过 `blockingForEach` 获取流式结果，并通过 try-catch 块捕获异常。

#### 仅非流式模式

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

`call` 方法接受 `GenerationParam`，并返回 `GenerationResult`，您也可以通过 try-catch 块捕获异常。

### 决策模型

决策模型（Decision Model）针对给定的业务状态，批量回答多选一（choice）、是否判断（noul）、有序量表（score）三类问题，详见[决策模型 API 文档](https://docs.bailian.console.aliyun.com/zh/model-studio/decision-model-api)。

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
    // 业务状态
    Map<String, Object> state = new HashMap<>();
    state.put("ticket_id", "T-1001");
    state.put("content", "订单支付后超过 24 小时仍未到账，用户无法继续使用核心服务，要求立即处理。");

    // 问题列表
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
      // 必须提供 workspace：可在此传入（new DecisionModel("你的-workspace-id")），
      // 或在运行前设置 DASHSCOPE_WORKSPACE 环境变量。
      DecisionModel decisionModel = new DecisionModel();
      DecisionModelResult result = decisionModel.call(param);
      System.out.println(JsonUtils.toJson(result));
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.err.println("An error occurred: " + e.getMessage());
    }
  }
}
```

`call` 方法接受 `DecisionModelParam`，并返回 `DecisionModelResult`，其中 `answers` 按问题 id 给出答案，不支持流式输出。API Key 默认从环境变量 `DASHSCOPE_API_KEY` 获取，也可以通过 `apiKey(...)` 指定。

请求地址是 workspace 作用域的：base URL 由 `https://{workspace}.{region}.maas.aliyuncs.com/compatible-mode/v1` 拼接而成（`systemone` 路径会自动追加）。**必须**提供 workspace id——通过构造函数传入（`new DecisionModel(workspace)` 或 `new DecisionModel(workspace, region)`），或设置环境变量 `DASHSCOPE_WORKSPACE`；否则构造客户端时会抛出 `IllegalArgumentException`。region 默认为 `cn-beijing`，可通过环境变量 `DASHSCOPE_API_REGION` 设置。

若要指向完全自定义的端点（例如 Token Plan 网关），请使用静态工厂方法 `DecisionModel.withBaseUrl(baseUrl)`。注意 `new DecisionModel(String)` 会把参数当作 **workspace id**，而不是 base URL。你也可以用环境变量 `DASHSCOPE_DECISION_BASE_URL` 覆盖整个 base URL；此处的实现有意不读取全局的 `DASHSCOPE_HTTP_BASE_URL`。

</details>
