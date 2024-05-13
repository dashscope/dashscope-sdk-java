// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

public class GenerationCallEarlyStop {
  static String modelName = Generation.Models.QWEN_PLUS;

  public static void stopWithTokens()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    List<Message> msgManager = new ArrayList<>();
    Message systemMsg = Message.builder().role(Role.SYSTEM.getValue()).content("You are a helpful assistant.").build();
    Message userMsg = Message.builder().role(Role.USER.getValue()).content("怎么做西红柿炖牛腩好吃").build();
    msgManager.add(systemMsg);
    msgManager.add(userMsg);
    // 老抽 [91777, 99950]
    // 葱花 [102902, 99232]
    QwenParam param = QwenParam.builder().model(modelName).messages(msgManager)
        .resultFormat(QwenParam.ResultFormat.MESSAGE)
        .topP(0.8)
        .enableSearch(true)
        .stopToken(Arrays.asList(91777, 99950))
        .stopToken(Arrays.asList(102902, 99232))
        .build();
    GenerationResult result = gen.call(param);
    System.out.println(result);
    param.setStopTokens(Arrays.asList(Arrays.asList(102902)));
    result = gen.call(param);
    System.out.println(result);
  }

  public static void stopWithStrings()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    List<Message> msgManager = new ArrayList<>();
    Message systemMsg = Message.builder().role(Role.SYSTEM.getValue()).content("You are a helpful assistant.").build();
    Message userMsg = Message.builder().role(Role.USER.getValue()).content("怎么做西红柿炖牛腩好吃").build();
    msgManager.add(systemMsg);
    msgManager.add(userMsg);
    // 老抽 [91777, 99950]
    // 葱花 [102902, 99232]
    QwenParam param = QwenParam.builder().model(modelName).messages(msgManager)
        .resultFormat(QwenParam.ResultFormat.MESSAGE)
        .topP(0.8)
        .enableSearch(true)
        .stopString("老抽")
        .stopString("葱花")
        .build();
    GenerationResult result = gen.call(param);
    System.out.println(result);
    param.setStopStrings(Arrays.asList("葱花"));
    ;
    result = gen.call(param);
    System.out.println(result);
  }

  public static void main(String[] args) {
    try {
      stopWithTokens();
      stopWithStrings();
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.out.println(e.getMessage());
    }
    System.exit(0);
  }
}
