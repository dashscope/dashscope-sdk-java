// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.DeploymentRun;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.DeploymentRunListParam;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public final class DeploymentRuns {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;

  public DeploymentRuns(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
  }

  public DeploymentRun retrieve(String deploymentRunId) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(deploymentRunId));
  }

  public CursorPage<DeploymentRun> list(DeploymentRunListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(param));
  }

  public CompletableFuture<DeploymentRun> retrieveAsync(String deploymentRunId) {
    if (deploymentRunId == null || deploymentRunId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("deploymentRunId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("deployment_runs/%s", deploymentRunId), baseUrl);
    return AsyncHelper.callAsync(api, getParam(), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, DeploymentRun.class));
  }

  public CompletableFuture<CursorPage<DeploymentRun>> listAsync(DeploymentRunListParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    String query = param.toQueryString();
    String path = query.isEmpty() ? "deployment_runs" : "deployment_runs?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(api, getParam(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<DeploymentRun>>() {}.getType();
              CursorPage<DeploymentRun> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          DeploymentRunListParam.builder()
                              .limit(param.getLimit())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  private GeneralGetParam getParam() {
    return GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build();
  }
}
