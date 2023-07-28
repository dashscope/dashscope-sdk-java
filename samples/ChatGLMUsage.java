// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.Arrays;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.GeneralHalfDuplexApi;
import com.alibaba.dashscope.utils.JsonUtils;

public class ChatGLMUsage {
  public static void usage()
      throws NoApiKeyException, ApiException, InputRequiredException {
    GeneralHalfDuplexApi gen = new GeneralHalfDuplexApi();
    ChatGLMParam param = ChatGLMParam.builder().model("chatglm-6b-v2").prompt("介绍下杭州").history(Arrays.asList()).build();
    DashScopeResult result = gen.call(param);
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
