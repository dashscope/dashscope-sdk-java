// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class GeneralHalfDuplexApi {
  private final SynchronizeHalfDuplexApi<HalfDuplexServiceParam> syncApi;
  private final ApiServiceOption serviceOption;

  public GeneralHalfDuplexApi() {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.OUT)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AIGC.getValue())
            .task(Task.TEXT_GENERATION.getValue())
            .function(Function.GENERATION.getValue())
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public GeneralHalfDuplexApi(String protocol) {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.of(protocol))
            .streamingMode(StreamingMode.OUT)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AIGC.getValue())
            .task(Task.TEXT_GENERATION.getValue())
            .function(Function.GENERATION.getValue())
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public GeneralHalfDuplexApi(ApiServiceOption serviceOption, ConnectionOptions connectionOptions) {
    this.serviceOption = serviceOption;
    syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
  }

  /**
   * Call the server to get the whole result, only http protocol
   *
   * @param param The input param of class `ConversationParam`.
   * @return The output structure of `QWenConversationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws InputRequiredException Missing inputs.
   */
  public DashScopeResult call(HalfDuplexServiceParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    return syncApi.call(param);
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `GenerationParam`.
   * @param callback The callback to receive response, the template class is `GenerationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  public void call(HalfDuplexServiceParam param, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(message);
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
   * Call the server to get the result by stream. http and websocket.
   *
   * @param param The input param of class `ConversationParam`.
   * @return A `Flowable` of the output structure.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  public Flowable<DashScopeResult> streamCall(HalfDuplexServiceParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(true);
    return syncApi.streamCall(param);
  }

  public void streamCall(HalfDuplexServiceParam param, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(true);
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            callback.onEvent(msg);
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
