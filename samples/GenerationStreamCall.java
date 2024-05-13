// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.concurrent.Semaphore;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

public class GenerationStreamCall {
  public static void streamCall()
      throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    GenerationParam param = GenerationParam.builder().model(Generation.Models.QWEN_PLUS)
        .prompt("就当前的海洋污染的情况，写一份限塑的倡议书提纲，需要有理有据地号召大家克制地使用塑料制品").topP(0.8).build();
    Flowable<GenerationResult> result = gen.streamCall(param);
    result.blockingForEach(message -> {
      System.out.println(JsonUtils.toJson(message));
    });
  }

  public static void streamCallWithCallback()
      throws NoApiKeyException, ApiException, InputRequiredException,InterruptedException {
    Generation gen = new Generation();
    GenerationParam param = GenerationParam.builder().model(Generation.Models.QWEN_PLUS)
        .prompt("就当前的海洋污染的情况，写一份限塑的倡议书提纲，需要有理有据地号召大家克制地使用塑料制品").topP(0.8).build();
    Semaphore semaphore = new Semaphore(0);
    gen.streamCall(param, new ResultCallback<GenerationResult>() {

      @Override
      public void onEvent(GenerationResult message) {
        System.out.println(message);
      }
      @Override
      public void onError(Exception err){
        System.out.println(String.format("Exception: %s", err.getMessage()));
        semaphore.release();
      }

      @Override
      public void onComplete(){
        System.out.println("Completed");
        semaphore.release();
      }
      
    });
    semaphore.acquire();

  }
  public static void main(String[] args) {
    try {
      streamCall();
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.out.println(e.getMessage());
    }
    try {
      streamCallWithCallback();
    } catch (ApiException | NoApiKeyException | InputRequiredException | InterruptedException e) {
      System.out.println(e.getMessage());
    }
    System.exit(0);
  }
}
