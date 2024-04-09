// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.conversation;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

/** @deprecated use `Generation` instead */
@Slf4j
public final class Conversation {
  /* Auto history messages */
  private final SynchronizeHalfDuplexApi<ConversationParam> syncApi;
  private final ApiServiceOption serviceOption;

  public static class Models {
    /** @deprecated use QWEN_TURBO instead */
    @Deprecated public static final String QWEN_V1 = "qwen-v1";

    public static final String QWEN_TURBO = "qwen-turbo";

    /** @deprecated use QWEN_PLUS instead */
    @Deprecated public static final String QWEN_PLUG_V1 = "qwen-plus-v1";

    public static final String QWEN_PLUS = "qwen-plus";
    public static final String QWEN_MAX = "qwen-max";
  }

  public ApiServiceOption defaultApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.OUT)
        .outputMode(OutputMode.ACCUMULATE)
        .taskGroup(TaskGroup.AIGC.getValue())
        .task(Task.TEXT_GENERATION.getValue())
        .function(Function.GENERATION.getValue())
        .build();
  }

  public Conversation() {
    serviceOption = defaultApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Conversation(String protocol) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Conversation(String protocol, String baseUrl) {
    serviceOption = defaultApiServiceOption();
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Conversation(String protocol, String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultApiServiceOption();
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `ConversationParam`.
   * @return The output structure of `QWenConversationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws InputRequiredException The input fields are missing.
   */
  public ConversationResult call(ConversationParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    return ConversationResult.fromDashScopeResult(syncApi.call(param));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `ConversationParam`.
   * @param callback The callback to receive response, the template class is `ConversationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException The input fields are missing.
   */
  public void call(ConversationParam param, ResultCallback<ConversationResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(ConversationResult.fromDashScopeResult(message));
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

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `ConversationParam`.
   * @return A `Flowable` of the output structure.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException The input fields are missing.
   */
  public Flowable<ConversationResult> streamCall(ConversationParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    return syncApi.streamCall(param).map(item -> ConversationResult.fromDashScopeResult(item));
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `ConversationParam`.
   * @param callback The result callback.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException The input fields are missing.
   */
  public void streamCall(ConversationParam param, ResultCallback<ConversationResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            callback.onEvent(ConversationResult.fromDashScopeResult(msg));
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
