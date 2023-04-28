# dashscope-sdk-java

This is the java sdk for the DashScope models.

## Usage

To use the sdk in your java systems, please add the maven dependency in your pom.xml:

```xml
<dependency>
    <groupId>org.alibaba.damo.dashscope</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>{dashscope-sdk-java-version}</version>
</dependency>
```

## QuickStart

You can call Conversation model with this sample code:

```java
import com.alibaba.fastjson.JSON;
import com.dashscope.conversation.Conversation;
import com.dashscope.conversation.ConversationResultBase;
import com.dashscope.conversation.qwen.QWenConversationParam;
import io.reactivex.Flowable;

public class Main {

    public static void main(String[] args) {
        Conversation api = new Conversation();

        QWenConversationParam param =
                QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1).prompt("hello")
                        .stream(true)
                        .maxLength(10)
                        .topP(0.8)
                        .build();

        Flowable<ConversationResultBase> flowable = api.streamCall(param);
        flowable.blockingForEach(msg -> System.out.println(JSON.toJSONString(msg)));
    }
}
```