// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.rerank;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.TaskGroup;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TextReRank {

  private final SynchronizeHalfDuplexApi<TextReRankParam> syncApi;
  private final ApiServiceOption serviceOption;

  public static class Models {
    public static final String GTE_RERANK_V2 = "gte-rerank-v2";
  }

  private ApiServiceOption defaultApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .outputMode(OutputMode.ACCUMULATE)
        .taskGroup(TaskGroup.RERANK.getValue())
        .task("text-rerank")
        .function("text-rerank")
        .build();
  }

  public TextReRank() {
    serviceOption = defaultApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TextReRank(String protocol) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TextReRank(String protocol, String baseUrl) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TextReRank(String protocol, String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
  }

  /** Creates a copy of the shared serviceOption for thread-safe per-call usage. */
  private ApiServiceOption copyServiceOption() {
    return ApiServiceOption.builder()
        .protocol(serviceOption.getProtocol())
        .httpMethod(serviceOption.getHttpMethod())
        .streamingMode(serviceOption.getStreamingMode())
        .outputMode(serviceOption.getOutputMode())
        .taskGroup(serviceOption.getTaskGroup())
        .task(serviceOption.getTask())
        .function(serviceOption.getFunction())
        .baseHttpUrl(serviceOption.getBaseHttpUrl())
        .baseWebSocketUrl(serviceOption.getBaseWebSocketUrl())
        .build();
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `TextReRankParam`.
   * @return The output structure of `TextReRankResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public TextReRankResult call(TextReRankParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(false);
    callOption.setStreamingMode(StreamingMode.NONE);
    return TextReRankResult.fromDashScopeResult(syncApi.call(param, callOption));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `TextReRankParam`.
   * @param callback The callback to receive response, the template class is `TextReRankResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void call(TextReRankParam param, ResultCallback<TextReRankResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    java.util.Objects.requireNonNull(callback, "callback must not be null");
    param.validate();
    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(false);
    callOption.setStreamingMode(StreamingMode.NONE);
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(TextReRankResult.fromDashScopeResult(message));
          }

          @Override
          public void onComplete() {
            callback.onComplete();
          }

          @Override
          public void onError(Exception e) {
            callback.onError(e);
          }
        },
        callOption);
  }
}
