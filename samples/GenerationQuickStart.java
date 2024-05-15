// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationParam.ResultFormat;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

public class GenerationQuickStart {
  public static void qwenQuickStart()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation(Protocol.HTTP.getValue());
    GenerationParam param = GenerationParam.builder().model("nufhfdj").resultFormat("message").prompt("如何做土豆炖猪脚?")
        .topP(0.8).seed(100).build();
    Flowable<GenerationResult> result = gen.streamCall(param);
    result.blockingForEach(msg->{
      System.out.println(msg);
    });
    System.out.println(JsonUtils.toJson(result));
  }

  public static void qwenQuickStartCallback()
      throws NoApiKeyException, ApiException, InputRequiredException, InterruptedException {
    Generation gen = new Generation();
    QwenParam param = QwenParam.builder().model(Generation.Models.QWEN_PLUS).prompt("如何做土豆炖猪脚?")
        .topP(0.8).build();
    Semaphore semaphore = new Semaphore(0);
    gen.call(param, new ResultCallback<GenerationResult>() {

      @Override
      public void onEvent(GenerationResult message) {
        System.out.println(message);
      }

      @Override
      public void onError(Exception ex) {
        System.out.println(ex.getMessage());
        semaphore.release();
      }

      @Override
      public void onComplete() {
        System.out.println("onComplete");
        semaphore.release();
      }

    });
    semaphore.acquire();
  }

  public static void quickStartWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
    Generation gen = new Generation();
    List<Message> messageManager = new ArrayList<>();
    messageManager.add(Message.builder().role(Role.USER.getValue()).content("今天天气好吗？").build());
    messageManager
        .add(Message.builder().role(Role.ASSISTANT.getValue()).content("今天天气不错，要出去玩玩嘛？").build());
    messageManager.add(Message.builder().role(Role.USER.getValue()).content("那你有什么地方推荐？").build());
    QwenParam param = QwenParam.builder().model(Conversation.Models.QWEN_PLUS)
        .messages(messageManager).topP(0.8).resultFormat(ResultFormat.MESSAGE).enableSearch(true).build();
    GenerationResult result = gen.call(param);
    System.out.println(result);
  }

  public static void main(String[] args) {
    try {   
      qwenQuickStart();
      qwenQuickStartCallback();
    } catch (ApiException | NoApiKeyException | InputRequiredException | InterruptedException e) {
      System.out.println(String.format("Exception %s", e.getMessage()));
    }
    System.exit(0);
  }
}
