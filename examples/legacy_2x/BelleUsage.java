// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;


public class BelleUsage{
  public static void usage()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    GenerationParam param = GenerationParam
    .builder()
    .model("belle-llama-13b-2m-v1")
    .prompt("Human:你好\n\nAssistant:")
    .build();
    GenerationResult result = gen.call(param);
    System.out.println(JsonUtils.toJson(result));
  }
  public static void main(String[] args){
        try {
          usage();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
          System.out.println(e.getMessage());        
        }
        System.exit(0);
  }
}
