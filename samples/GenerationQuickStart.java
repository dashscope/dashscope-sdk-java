// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.concurrent.Semaphore;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

public class GenerationQuickStart {
  public static void qwenQuickStart()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    QwenParam param = QwenParam.builder().model(Generation.Models.QWEN_PLUS_V1).prompt("如何做土豆炖猪脚?")
        .topP(0.8).build();
    GenerationResult result = gen.call(param);
    System.out.println(JsonUtils.toJson(result));
  }

  public static void qwenQuickStartCallback()
      throws NoApiKeyException, ApiException, InputRequiredException, InterruptedException {
    Generation gen = new Generation();
    QwenParam param = QwenParam.builder().model(Generation.Models.QWEN_PLUS_V1).prompt("如何做土豆炖猪脚?")
        .topP(0.8).build();
    Semaphore semaphore = new Semaphore(0);
    gen.call(param, new ResultCallback<GenerationResult>() {

      @Override
      public void onEvent(GenerationResult message) {
        System.out.println(message);
      }
      @Override
      public void onError(Exception ex){
        System.out.println(ex.getMessage());
        semaphore.release();
      }
      @Override
      public void onComplete(){
        System.out.println("onComplete");
        semaphore.release();
      }
      
    });
    semaphore.acquire();
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
