// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.DollyParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;


public class DollyUsage {
  public static void dollyQuickStart()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    DollyParam param = DollyParam.builder().model(Generation.Models.DOLLY_12B_V2).prompt("如何做土豆炖猪脚?").build();
    GenerationResult result = gen.call(param);
    System.out.println(JsonUtils.toJson(result));
  }

  public static void main(String[] args){
        try {
          dollyQuickStart();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
          System.out.println(e.getMessage());
        }
        System.exit(0);
  }
}
