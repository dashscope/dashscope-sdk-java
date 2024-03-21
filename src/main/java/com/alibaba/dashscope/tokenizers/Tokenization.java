// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.tokenizers;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Tokenization {
  private final SynchronizeHalfDuplexApi<HalfDuplexServiceParam> syncApi;
  private final ApiServiceOption serviceOption;

  public static class Models {
    public static final String QWEN_TURBO = "qwen-turbo";
    public static final String QWEN_PLUS = "qwen-plus";
    public static final String QWEN_7B_CHAT = "qwen-7b-chat";
    public static final String QWEN_14B_CHAT = "qwen-14b-chat";
    public static final String LLAMA2_7B_CHAT_V2 = "llama2-7b-chat-v2";
    public static final String LLAMA2_13B_CHAT_V2 = "llama2-13b-chat-v2";
    public static final String TEXT_EMBEDDING_V1 = "text-embedding-v1";
    public static final String TEXT_EMBEDDING_V2 = "text-embedding-v2";
    public static final String QWEN_72B_CHAT = "qwen-72b-chat";
  }

  public Tokenization() {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .isSSE(false)
            .streamingMode(StreamingMode.NONE)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(null)
            .task(null)
            .function("tokenizer")
            .isService(false)
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Tokenization(String protocol) {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.of(protocol))
            .streamingMode(StreamingMode.NONE)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(null)
            .task(null)
            .function("tokenizer")
            .isService(false)
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  /**
   * Call the server to get the whole result, only http protocol
   *
   * @param param The input param of class `HalfDuplexServiceParam`.
   * @return The output structure of `TokenizationOutput`.
   * @throws NoApiKeyException Can not find api key
   * @throws InputRequiredException Missing inputs.
   */
  public TokenizationResult call(HalfDuplexServiceParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    return TokenizationResult.fromDashScopeResult(syncApi.call(param));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `HalfDuplexServiceParam`.
   * @param callback The callback to receive response, the template class is `TokenizationOutput`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  public void call(HalfDuplexServiceParam param, ResultCallback<TokenizationResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(TokenizationResult.fromDashScopeResult(message));
          }

          @Override
          public void onComplete() {
            callback.onComplete();
          }

          @Override
          public void onError(Exception e) {
            callback.onError(e);
          }
        });
  }
}
