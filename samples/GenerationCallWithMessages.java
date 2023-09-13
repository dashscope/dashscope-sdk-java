// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageManager;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;



public class GenerationCallWithMessages {
  public static void callWithMessage()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    MessageManager msgManager = new MessageManager(10);
    Message systemMsg =
        Message.builder().role(Role.SYSTEM.getValue()).content("你是达摩院的生活助手机器人。").build();
    Message userMsg = Message.builder().role(Role.USER.getValue()).content("你好，周末去哪里玩？").build();
    msgManager.add(systemMsg);
    msgManager.add(userMsg);
    QwenParam param =
        QwenParam.builder().model(Generation.Models.QWEN_PLUS).messages(msgManager.get())
            .resultFormat(QwenParam.ResultFormat.MESSAGE)
            .topP(0.8)
            .enableSearch(true)
            .build();
    GenerationResult result = gen.call(param);
    System.out.println(result);
    msgManager.add(result);
    System.out.println(JsonUtils.toJson(result));
    param.setPrompt("找个近点的");
    param.setMessages(msgManager.get());
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
