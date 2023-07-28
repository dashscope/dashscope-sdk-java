// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

public class Llama2Usage {
  public static void usage()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    String prompt = "Hey, are you conscious? Can you talk to me?";
    GenerationParam param = GenerationParam
        .builder()
        .model("llama2-7b-chat-v2")
        .prompt(prompt)
        .build();
    GenerationResult result = gen.call(param);
    System.out.println(JsonUtils.toJson(result));
  }

  public static void main(String[] args) {
    try {
      usage();
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.out.println(e.getMessage());
    }
    System.exit(0);
  }
}
