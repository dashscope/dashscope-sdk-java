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
          // TODO all messages received
        }

        public void onError(Exception e) {
          ApiException apiException = (ApiException) e;
          // TODO deal with exception
        }
      }

      generation.call(param, new ReactCallback());
    }
}
```

The Exception instance is an `ApiException` instance. This Exception may contain two parts:

- A `Status` instance. This instance carries a status_code (the http error code), a code (server error code), a message (server error message), the request id, and the usage information.
- If an exception occurs, the `ApiException` instance may only carry an `Exception` stack trace, you can deal with it as you usually do.

#### Stream only, accept by reactive io

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

The `streamCall` method accepts a `GenerationParam`, and returns a `Flowable`, which you can get the streaming result by `blockingForEach`, and catch the exception by the try-catch block.

#### Non-stream only

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

The `call` method accepts a `GenerationParam`, and returns a `GenerationResult`, you can also catch the exception with a try-catch block.

### Decision Model

The decision model answers a batch of multiple-choice (choice), yes/no (noul) and ordered-scale (score) questions against a given business state, see the [Decision Model API document](https://platform.qianwenai.com/docs/api-reference/decision-model-api).

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
      DecisionModel decisionModel = new DecisionModel();
      DecisionModelResult result = decisionModel.call(param);
      System.out.println(JsonUtils.toJson(result));
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.err.println("An error occurred: " + e.getMessage());
    }
  }
}
```

The `call` method accepts a `DecisionModelParam`, and returns a `DecisionModelResult` whose `answers` are keyed by question id. Streaming is not supported. The API key is read from the `DASHSCOPE_API_KEY` environment variable by default, or set explicitly with `apiKey(...)`; the request url defaults to `https://maas.qianwenaiapi.com/compatible-mode/v1/systemone`, override it with `new DecisionModel(baseUrl)` or the `DASHSCOPE_HTTP_BASE_URL` environment variable.
