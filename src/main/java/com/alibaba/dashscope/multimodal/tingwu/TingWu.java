// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.multimodal.tingwu;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;

/** The tingwu client. */
public final class TingWu {
  private final SynchronizeHalfDuplexApi<HalfDuplexServiceParam> syncApi;
  private final ApiServiceOption serviceOption;
  private final String DEFAULT_BASE_HTTP_URL =
      "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

  private ApiServiceOption defaultApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .isService(false)
        .baseHttpUrl(DEFAULT_BASE_HTTP_URL)
        .build();
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
        .isService(false)
        .baseHttpUrl(serviceOption.getBaseHttpUrl())
        .baseWebSocketUrl(serviceOption.getBaseWebSocketUrl())
        .build();
  }

  public TingWu() {
    serviceOption = defaultApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TingWu(String protocol) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TingWu(String protocol, String baseUrl) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TingWu(String protocol, String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
  }

  /** Call the server to get the whole result, only http protocol */
  public DashScopeResult call(HalfDuplexServiceParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(false);
    return syncApi.call(param, callOption);
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param.
   * @param callback The callback to receive response.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  public void call(HalfDuplexServiceParam param, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    java.util.Objects.requireNonNull(callback, "callback must not be null");
    param.validate();
    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(false);
    syncApi.call(param, callback, callOption);
  }
}
