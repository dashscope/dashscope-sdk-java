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

### Conversation

You can create a conversation client simply by:

```java
// Use http as the network protocol.
Conversation conversation = new Conversation();
```

This interface also accept a protocol argument:

```java
import com.alibaba.dashscope.common.Protocol;
Conversation conversation = new Conversation(Protocol.HTTP.getValue());
Conversation conversation = new Conversation(Protocol.WEBSOCKET.getValue());
```

The conversation interface supports both stream and non-stream queries. These queries all accept `ConversationParam`  as input, and returns `ConversationResult` as output . Each model has a unique input structure and output structure which derives from the two data classes mentioned above. Please use these sub classes when you are using the coordinating model. 

Here shows the usages of each method, with the examples of `qwen-turbo` model.

#### Support stream and non-stream mode, accept output from callback

```java
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.conversation.qwen.QWenConversationParam;
import com.alibaba.dashscope.aigc.conversation.qwen.QWenConversationResult;
import com.alibaba.dashscope.exception.ApiException;

public class Main {

  public static void main(String[] args) {
    Conversation conversation = new Conversation();

    //QWEN model, if using other model, you can replace the QWenConversationParam here.
    QWenConversationParam param =
            QWenConversationParam.builder()
                    .model(QWenConversationParam.QWEN_TURBO)
                    //there are other arguments supported.
                    .prompt("hello")
                    .apiKey("testKey")
                    .build();

    class ReactCallback extends ResultCallback<ConversationResult> {

      @Override
      public void onEvent(ConversationResult message) {
        QWenConversationResult result = (QWenConversationResult) message;
        // TODO deal with result
      }

      public void onComplete() {
        // TODO all messages received
      }

      public void onError(Exception e) {
        ApiException apiException = (ApiException) e;
        // TODO deal with exception
      }
    }
    conversation.call(param, new ReactCallback());
  }
}

```

The Exception instance in the conversation scenario is a `ApiException` instance. This Exception may contain two parts:

- A `Status` instance. This instance carries a status_code(The http error code), a code(server error code), a message(server error message), the input message id, and the usage information.
- If an exception occurs, the `ApiException` instance may only carry an `Exception` stack trace, you can deal with it as you usually do.

#### Stream only, accept by react io

```java
import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.conversation.qwen.QWenConversationParam;
import com.alibaba.dashscope.common.StreamingMode;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

public class Main {

  public static void main(String[] args) {
    Conversation conversation = new Conversation();

    //QWEN model, if using other model, you can replace the QWenConversationParam here.
    QWenConversationParam param =
            QWenConversationParam.builder().model(QWenConversationParam.QWEN_TURBO).prompt("hello")
                    .mode(StreamingMode.OUT)
                    .apiKey("testKey")
                    .build();

    Flowable<ConversationResult> flowable = conversation.streamCall(param);
    try {
      flowable.blockingForEach(msg -> System.out.println(JsonUtils.toJson(msg)));
    } catch (Throwable e) {
      // TODO deal with error here
    }
  }
}
```

The `streamCall` method accepts a `ConversationParam` , and returns a `Flowable`, which you can get the streaming result by `blockingForEach`, and catch the exception by the try-catch block.

#### Non-stream only

```java
import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.conversation.qwen.QWenConversationParam;

public class Main {

  public static void main(String[] args) {
    Conversation conversation = new Conversation();

    //QWEN model, if using other model, you can replace the QWenConversationParam here.
    QWenConversationParam param =
            QWenConversationParam.builder()
                    .model(QWenConversationParam.QWEN_TURBO)
                    .prompt("hello")
                    .apiKey("testKey")
                    .build();

    ConversationResult result = conversation.call(param);
  }
}
```

The `call` method accepts a `ConversationParam`, and returns a `ConversationResult`, you can also catch the exception with a try-catch block.





