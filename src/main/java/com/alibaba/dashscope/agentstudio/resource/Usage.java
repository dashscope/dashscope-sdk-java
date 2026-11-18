// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.param.UsageQueryParam;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Usage totals and cursor-paginated trends, using the shared GeneralApi HTTP implementation. */
public final class Usage {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;

  public Usage(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
  }

  public Result summary(UsageQueryParam param) {
    return AsyncHelper.joinAndUnwrap(summaryAsync(param));
  }

  public Result trend(UsageQueryParam param) {
    return AsyncHelper.joinAndUnwrap(trendAsync(param));
  }

  public CompletableFuture<Result> summaryAsync(UsageQueryParam param) {
    return query("usage/summary", param);
  }

  public CompletableFuture<Result> trendAsync(UsageQueryParam param) {
    return query("usage/trend", param);
  }

  private CompletableFuture<Result> query(String path, UsageQueryParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(HttpMethod.POST, path, baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Result.class));
  }

  /** Complete data envelope, retaining null metrics, units, breakdowns and opaque page tokens. */
  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Result extends FlattenResultBase {
    private JsonObject data;
  }
}
