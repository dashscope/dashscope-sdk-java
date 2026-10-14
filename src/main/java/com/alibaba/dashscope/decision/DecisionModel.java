// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision;

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
 * Unofficial Java binding for the Bailian Decision Model API ("TypeSafe System One").
 *
 * <p>The upstream doc currently ships only cURL and Python ({@code typesafe-sdk}) examples; the
 * Java surface here may evolve as the official spec matures.
 *
 * <p>Document: https://docs.bailian.console.aliyun.com/zh/model-studio/decision-model-api
 *
 * <p>The endpoint is workspace-scoped: {@code
 * https://{WorkspaceId}.{region}.maas.aliyuncs.com/compatible-mode/v1/systemone}. A workspace id
 * must be supplied either via the constructor or the {@value #ENV_WORKSPACE} environment variable.
 */
public final class DecisionModel {

  /** Hostname template for the workspace-scoped decision model gateway. */
  public static final String BASE_URL_TEMPLATE =
      "https://%s.%s.maas.aliyuncs.com/compatible-mode/v1";

  /** Region used when neither the constructor nor {@value #ENV_REGION} provides one. */
  public static final String DEFAULT_REGION = "cn-beijing";

  /** Environment variable holding the Bailian workspace id. */
  public static final String ENV_WORKSPACE = "DASHSCOPE_WORKSPACE";

  /** Environment variable holding the region. */
  public static final String ENV_REGION = Constants.DASHSCOPE_API_REGION_ENV;

  /**
   * Dedicated environment variable that fully overrides the base URL. Do not reuse {@link
   * Constants#DASHSCOPE_HTTP_BASE_URL_ENV} here — that variable is bound to the {@code
   * dashscope.aliyuncs.com/api/v1} shape used by the rest of the SDK.
   */
  public static final String ENV_BASE_URL = "DASHSCOPE_DECISION_BASE_URL";

  /** Documented Bailian regions that host the decision model gateway. */
  public static final class Regions {
    public static final String CN_BEIJING = "cn-beijing";
    public static final String AP_SOUTHEAST_1 = "ap-southeast-1";

    private Regions() {}
  }

  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  private static GeneralServiceOption defaultServiceOption() {
    return GeneralServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .path("systemone")
        .module("decision")
        .build();
  }

  /**
   * Compose the workspace-scoped base URL. Honors {@link #ENV_BASE_URL} first; otherwise both
   * arguments may be null/empty, in which case the corresponding environment variable (and finally
   * {@link #DEFAULT_REGION}) is consulted.
   *
   * @throws IllegalArgumentException if neither a workspace id nor an explicit base URL override
   *     can be resolved.
   */
  public static String resolveBaseUrl(String workspace, String region) {
    String override = System.getenv(ENV_BASE_URL);
    if (override != null && !override.isEmpty()) {
      return override;
    }
    String ws = workspace;
    if (ws == null || ws.isEmpty()) {
      ws = System.getenv(ENV_WORKSPACE);
    }
    if (ws == null || ws.isEmpty()) {
      throw new IllegalArgumentException(
          "workspace is required. Pass it to the DecisionModel constructor, set the "
              + ENV_WORKSPACE
              + " or "
              + ENV_BASE_URL
              + " env var, or use DecisionModel.withBaseUrl(...) for a custom endpoint.");
    }
    String r = region;
    if (r == null || r.isEmpty()) {
      r = System.getenv(ENV_REGION);
    }
    if (r == null || r.isEmpty()) {
      r = DEFAULT_REGION;
    }
    return String.format(BASE_URL_TEMPLATE, ws, r);
  }

  /** Resolve workspace and region from the environment. */
  public DecisionModel() {
    this(null, null, null);
  }

  /** Use the given workspace with the region resolved from the environment. */
  public DecisionModel(String workspace) {
    this(workspace, null, null);
  }

  /** Use the given workspace and region. */
  public DecisionModel(String workspace, String region) {
    this(workspace, region, null);
  }

  public DecisionModel(String workspace, String region, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(resolveBaseUrl(workspace, region));
    api = connectionOptions == null ? new GeneralApi<>() : new GeneralApi<>(connectionOptions);
  }

  /** Internal: build a client from an already-populated service option. */
  private DecisionModel(GeneralServiceOption option, ConnectionOptions connectionOptions) {
    this.serviceOption = option;
    this.api = connectionOptions == null ? new GeneralApi<>() : new GeneralApi<>(connectionOptions);
  }

  /**
   * Build a client that targets an explicit base URL, bypassing workspace/region resolution.
   * Intended for tests, self-hosted gateways, and the Token Plan endpoint ({@code
   * https://token-plan.cn-beijing.maas.aliyuncs.com/compatible-mode/v1}).
   */
  public static DecisionModel withBaseUrl(String baseUrl) {
    return withBaseUrl(baseUrl, null);
  }

  public static DecisionModel withBaseUrl(String baseUrl, ConnectionOptions connectionOptions) {
    if (baseUrl == null || baseUrl.isEmpty()) {
      throw new IllegalArgumentException("baseUrl must not be null or empty");
    }
    GeneralServiceOption option = defaultServiceOption();
    option.setBaseHttpUrl(baseUrl);
    return new DecisionModel(option, connectionOptions);
  }

  /**
   * Call the server to get the whole result. The decision model does not stream — this is a single
   * request/response round trip.
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
    return DecisionModelResult.fromDashScopeResult(api.call(param, serviceOption));
  }

  /**
   * Call the server and deliver the result through {@code callback}. The decision model returns a
   * single response body, so the callback receives exactly one {@link
   * ResultCallback#onEvent(Object)} before {@link ResultCallback#onComplete()}.
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
