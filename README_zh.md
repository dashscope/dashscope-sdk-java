# dashscope-sdk-java

这是 DashScope 模型的 Java SDK。

## 使用方法

要在您的 Java 系统中使用该 SDK，请在 pom.xml 中添加以下 Maven 依赖：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>{dashscope-sdk-java-version}</version>
</dependency>
```

## 快速开始

### 文本生成

您可以通过以下方式创建文本生成客户端：

```java
Generation generation = new Generation();
```

文本生成接口支持流式和非流式查询。这些查询都接受 `GenerationParam` 作为输入，并返回 `GenerationResult` 作为输出。

以下是各方法的使用示例，以 `qwen-turbo` 模型为例。

#### 支持流式和非流式模式，通过回调接收输出

```java
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.JsonUtils;
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

      generation.call(param, new ReactCallback());
    }
}
```

异常实例是一个 `ApiException` 实例。该异常可能包含两部分：

- 一个 `Status` 实例。该实例包含 status_code（HTTP 错误码）、code（服务器错误码）、message（服务器错误信息）、请求 ID 和使用信息。
- 如果发生异常，`ApiException` 实例可能只包含一个 `Exception` 堆栈跟踪，您可以像平常一样处理它。

#### 仅流式模式，通过响应式 IO 接收

```java
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
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
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
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

决策模型（Decision Model）针对给定的业务状态，批量回答多选一（choice）、是否判断（noul）、有序量表（score）三类问题，详见[决策模型 API 文档](https://platform.qianwenai.com/docs/api-reference/decision-model-api)。

```java
import com.alibaba.dashscope.decision.DecisionModel;
import com.alibaba.dashscope.decision.DecisionModelParam;
import com.alibaba.dashscope.decision.DecisionModelResult;
import com.alibaba.dashscope.decision.DecisionQuestion;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
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
      DecisionModel decisionModel = new DecisionModel();
      DecisionModelResult result = decisionModel.call(param);
      System.out.println(JsonUtils.toJson(result));
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.err.println("An error occurred: " + e.getMessage());
    }
  }
}
```

`call` 方法接受 `DecisionModelParam`，并返回 `DecisionModelResult`，其中 `answers` 按问题 id 给出答案，不支持流式输出。API Key 默认从环境变量 `DASHSCOPE_API_KEY` 获取，也可以通过 `apiKey(...)` 指定；请求地址默认为 `https://maas.qianwenaiapi.com/compatible-mode/v1/systemone`，可通过 `new DecisionModel(baseUrl)` 或环境变量 `DASHSCOPE_HTTP_BASE_URL` 覆盖。