# dashscope-sdk-java

This is the java sdk for the DashScope models.

## Usage

To use the sdk in your java systems, please add the maven dependency in your pom.xml:

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>{dashscope-sdk-java-version}</version>
</dependency>
```

## 3.0 (new generation)

Version 3.0 introduces a new client entry point in the same artifact. The 3.0 implementation
packages live directly under `com.alibaba.dashscope.*`; legacy 2.x APIs other than AgentStudio
have moved to `com.alibaba.dashscope.legacy_2x.*` and are deprecated for removal in 4.0. This
is an intentional FQCN, source, and binary compatibility break: existing 2.x imports must be
updated. Java 8+ is required at runtime (bytecode target 8), and the build baseline is JDK 8
as well: CI builds and tests on JDK 8. Building from source on JDK 9+ also works — the pom's
jdk9-plus profile auto-activates and compiles with release 8 API checking. All public top-level
types under `legacy_2x` carry type-level `@Deprecated`, so javac reports deprecation when they
are used (`-Xlint:deprecation` shows details). Packages retain only `@deprecated` Javadoc tags:
combining a package annotation with that tag fails on Java 8, and package deprecation does not
propagate to types. AgentStudio and `shared` are not deprecated.

```java
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import java.util.Collections;

// apiKey resolves from the config chain: builder > -Ddashscope.api.key >
// DASHSCOPE_API_KEY > ~/.dashscope/config.toml > 1.x key file
// (DASHSCOPE_API_KEY_FILE_PATH or ~/.dashscope/api_key)
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

- Resource tree: `chat / multimodal / apps / images / videos / embeddings / rerank /
  tokenization / audio / fineTunes / deployments / files / models / tingwu / decision /
  agentStudio` — design details live in the internal doc `md/dashscope-3.0-design.md`
  (not shipped in this repository).
- Async variant: `DashScopeAsyncClient.builder()` mirrors the same resource tree, returning
  `CompletableFuture` (streaming surfaces as RxJava `Flowable`).
- Migrating from 2.x: internal doc `md/dashscope-3.0-migration.md` (not shipped in this
  repository; per-class mapping, unchecked-exception model, AgentStudio and tokenizers notes).
- Runnable samples: [examples/README.md](examples/README.md) (four layers, MockWebServer dry-run
  via `mvn -f examples/pom.xml clean test`; CI installs the current SDK with
  `mvn install -DskipTests -Dgpg.skip=true` after root tests, then runs the standalone examples project).
- Everything below this section documents the deprecated 2.x API (`Generation`, `Conversation`,
  …). Its snippets use the current `legacy_2x` imports; these APIs will be removed in 4.0.

## QuickStart

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
