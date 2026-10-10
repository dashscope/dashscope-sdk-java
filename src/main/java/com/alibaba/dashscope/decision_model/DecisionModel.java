// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision_model;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.Constants;

/**
 * The decision model api, answers a batch of choice/noul/score questions against a business state.
 *
 * <p>Document: https://platform.qianwenai.com/docs/api-reference/decision-model-api
 */
public final class DecisionModel {
  private static final String DEFAULT_BASE_URL = "https://maas.qianwenaiapi.com/compatible-mode/v1";

  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  private GeneralServiceOption defaultServiceOption() {
    return GeneralServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .path("systemone")
        .module("decision_model")
        .baseHttpUrl(
            System.getenv().getOrDefault(Constants.DASHSCOPE_HTTP_BASE_URL_ENV, DEFAULT_BASE_URL))
        .build();
  }

  public DecisionModel() {
    serviceOption = defaultServiceOption();
    api = new GeneralApi<>();
  }

  public DecisionModel(String baseUrl) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>();
  }

  public DecisionModel(String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>(connectionOptions);
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `DecisionModelParam`.
   * @return The output structure of `DecisionModelResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing required input fields.
   */
  public DecisionModelResult call(DecisionModelParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    return DecisionModelResult.fromDashScopeResult(api.call(param, serviceOption));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `DecisionModelParam`.
   * @param callback The callback to receive the result of `DecisionModelResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing required input fields.
   */
  public void call(DecisionModelParam param, ResultCallback<DecisionModelResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    api.call(
        param,
        serviceOption,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(DecisionModelResult.fromDashScopeResult(message));
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
