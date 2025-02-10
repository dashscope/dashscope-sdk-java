// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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


public class GenerationCallWithMessages {
  public static void callWithMessage()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    List<Message> msgManager = new ArrayList<>();
    Message systemMsg =
        Message.builder().role(Role.SYSTEM.getValue()).content("You are a helpful assistant.").build();
    Message userMsg = Message.builder().role(Role.USER.getValue()).content("你好，周末去哪里玩？").build();
    msgManager.add(systemMsg);
    msgManager.add(userMsg);
    GenerationParam param =
        GenerationParam.builder().model(Generation.Models.QWEN_PLUS).messages(msgManager)
            .resultFormat(GenerationParam.ResultFormat.MESSAGE)
            .incrementalOutput(true)
            .topP(0.8)
            .enableSearch(true)
            .build();
    GenerationResult result = gen.call(param);
    System.out.println(result);
    msgManager.add(result.getOutput().getChoices().get(0).getMessage());
    System.out.println(JsonUtils.toJson(result));
    param.setPrompt("找个近点的");
    param.setMessages(msgManager);
    result = gen.call(param);
    System.out.println(result);
    System.out.println(JsonUtils.toJson(result));
  }

  public static void streamCallWithMessage()
          throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    Message systemMsg =
            Message.builder().role(Role.SYSTEM.getValue()).content("You are a helpful assistant.").build();
    Message userMsg = Message.builder().role(Role.USER.getValue()).content("9.9和9.11谁大").build();
    GenerationParam param = GenerationParam.builder().model("deepseek-r1")
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .incrementalOutput(true)
                    .build();
    Flowable<GenerationResult> result = gen.streamCall(param);
      result.blockingSubscribe( data -> {
          System.out.println(JsonUtils.toJson(data));
      });
  }

  public static void main(String[] args){
        try {
//          callWithMessage();
            streamCallWithMessage();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
          System.out.println(e.getMessage());
        }
        System.exit(0);
  }
}
