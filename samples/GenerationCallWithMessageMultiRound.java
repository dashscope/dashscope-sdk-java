// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.List;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;



public class GenerationCallWithMessageMultiRound {
  public static void callWithMessage()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    List<Message> msgManager = new ArrayList<>();
    Message systemMsg =
        Message.builder().role(Role.SYSTEM.getValue()).content("你是达摩院的智能助手机器人").build();
    Message userMsg = Message.builder().role(Role.USER.getValue()).content("就当前的海洋污染的情况，写一份限塑的倡议书提纲，需要有理有据地号召大家克制地使用塑料制品").build();
    msgManager.add(systemMsg);
    msgManager.add(userMsg);
    QwenParam param =
        QwenParam.builder().model(Generation.Models.QWEN_PLUS).messages(msgManager)
            .resultFormat(QwenParam.ResultFormat.MESSAGE)
            .topP(0.8)
            .enableSearch(true)
            .build();
    GenerationResult result = gen.call(param);
    System.out.println(result);
    msgManager.add(result.getOutput().getChoices().get(0).getMessage());
    System.out.println(JsonUtils.toJson(result));
    param.setPrompt("能否缩短一些，只讲三点");
    param.setMessages(msgManager);
    result = gen.call(param);
    System.out.println(result);
    System.out.println(JsonUtils.toJson(result));
  }


  public static void main(String[] args){
        try {
          callWithMessage();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
          System.out.println(e.getMessage());
        }
        System.exit(0);
  }
}
