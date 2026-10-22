// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageContentBase;
import com.alibaba.dashscope.common.MessageContentText;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class GenerationCallWithCacheControl {

  private static final String MODEL = System.getenv("MODEL_NAME");

  public static void streamCall()
          throws NoApiKeyException, ApiException, InputRequiredException {
    Generation gen = new Generation();
    Message systemMsg =
            Message.builder().role(Role.SYSTEM.getValue()).content("You are a helpful assistant.").build();

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 1024; i++) {
      sb.append("abc");
    }
    MessageContentBase content = MessageContentText.builder()
            .type("text")
            .text(sb + "床前明月光，后几句是什么？")
            .cacheControl(MessageContentText.CacheControl.builder()
                    .type("ephemeral")
                    .ttl("5m")
                    .build())
            .build();
    Message userMsg = Message.builder()
            .role(Role.USER.getValue())
            .contents(Collections.singletonList(content))
            .build();

    GenerationParam param = GenerationParam.builder().model(MODEL)
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
          streamCall();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
          System.out.println(e.getMessage());
        }
        System.exit(0);
  }
}
