// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MultiModalConversation {
  /* Auto history messages */
  private final SynchronizeHalfDuplexApi<MultiModalConversationParam> syncApi;
  private final ApiServiceOption serviceOption;

  public static class Models {
    public static final String QWEN_VL_CHAT_V1 = "qwen-vl-chat-v1";
  }

  public MultiModalConversation() {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.NONE)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AIGC.getValue())
            .task(Task.MULTIMODAL_GENERATION.getValue())
            .function(Function.GENERATION.getValue())
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public MultiModalConversation(String protocol) {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.of(protocol))
            .streamingMode(StreamingMode.NONE)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AIGC.getValue())
            .task(Task.MULTIMODAL_GENERATION.getValue())
            .function(Function.GENERATION.getValue())
            .build();
    syncApi =
        new SynchronizeHalfDuplexApi<>(
            ClientOptions.builder().protocol(protocol).build(), serviceOption);
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `MultiModalConversationParam`.
   * @return The output structure of `MultiModalConversationResult`.
   * @throws NoApiKeyException Can not find api key
   */
  public MultiModalConversationResult call(MultiModalConversationParam param)
      throws ApiException, NoApiKeyException {
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    return MultiModalConversationResult.fromDashScopeResult(syncApi.call(param));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `MultiModalConversationParam`.
   * @param callback The callback to receive response, the template class is
   *     `MultiModalConversationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void call(
      MultiModalConversationParam param, ResultCallback<MultiModalConversationResult> callback)
      throws ApiException, NoApiKeyException {
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(MultiModalConversationResult.fromDashScopeResult(message));
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
   * @param param The input param of class `MultiModalConversationParam`.
   * @return A `Flowable` of the output structure.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public Flowable<MultiModalConversationResult> streamCall(MultiModalConversationParam param)
      throws ApiException, NoApiKeyException {
    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    return syncApi
        .streamCall(param)
        .map(item -> MultiModalConversationResult.fromDashScopeResult(item));
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `MultiModalConversationParam`.
   * @param callback The result callback.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException The input field is missing.
   */
  public void streamCall(
      MultiModalConversationParam param, ResultCallback<MultiModalConversationResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            callback.onEvent(MultiModalConversationResult.fromDashScopeResult(msg));
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
