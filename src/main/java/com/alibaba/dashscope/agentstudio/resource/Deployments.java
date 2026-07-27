// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.Deployment;
import com.alibaba.dashscope.agentstudio.model.DeploymentRun;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.DeploymentCreateParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentListParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentRunListParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentUpdateParam;
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

public final class Deployments {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;

  public Deployments(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
  }

  public Deployment create(DeploymentCreateParam param) {
    return AsyncHelper.joinAndUnwrap(createAsync(param));
  }

  public Deployment retrieve(String deploymentId) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(deploymentId));
  }

  public Deployment update(String deploymentId, DeploymentUpdateParam param) {
    return AsyncHelper.joinAndUnwrap(updateAsync(deploymentId, param));
  }

  public CursorPage<Deployment> list(DeploymentListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(param));
  }

  public Deployment pause(String deploymentId) {
    return AsyncHelper.joinAndUnwrap(pauseAsync(deploymentId));
  }

  public Deployment unpause(String deploymentId) {
    return AsyncHelper.joinAndUnwrap(unpauseAsync(deploymentId));
  }

  public Deployment archive(String deploymentId) {
    return AsyncHelper.joinAndUnwrap(archiveAsync(deploymentId));
  }

  public DeploymentRun run(String deploymentId) {
    return AsyncHelper.joinAndUnwrap(runAsync(deploymentId));
  }

  public CursorPage<DeploymentRun> listRuns(String deploymentId, DeploymentRunListParam param) {
    return AsyncHelper.joinAndUnwrap(listRunsAsync(deploymentId, param));
  }

  public CompletableFuture<Deployment> createAsync(DeploymentCreateParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(HttpMethod.POST, "deployments", baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Deployment.class));
  }

  public CompletableFuture<Deployment> retrieveAsync(String deploymentId) {
    if (isBlank(deploymentId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("deploymentId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("deployments/%s", deploymentId), baseUrl);
    return AsyncHelper.callAsync(api, getParam(), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Deployment.class));
  }

  public CompletableFuture<Deployment> updateAsync(
      String deploymentId, DeploymentUpdateParam param) {
    if (isBlank(deploymentId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("deploymentId is required!"));
    }
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("deployments/%s", deploymentId), baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Deployment.class));
  }

  public CompletableFuture<CursorPage<Deployment>> listAsync(DeploymentListParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    String query = param.toQueryString();
    String path = query.isEmpty() ? "deployments" : "deployments?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(api, getParam(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<Deployment>>() {}.getType();
              CursorPage<Deployment> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          DeploymentListParam.builder()
                              .agentId(param.getAgentId())
                              .keyword(param.getKeyword())
                              .status(param.getStatus())
                              .includeArchived(param.getIncludeArchived())
                              .limit(param.getLimit())
                              .createdAtGte(param.getCreatedAtGte())
                              .createdAtLte(param.getCreatedAtLte())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<Deployment> pauseAsync(String deploymentId) {
    return lifecycleAsync(deploymentId, "pause");
  }

  public CompletableFuture<Deployment> unpauseAsync(String deploymentId) {
    return lifecycleAsync(deploymentId, "unpause");
  }

  public CompletableFuture<Deployment> archiveAsync(String deploymentId) {
    return lifecycleAsync(deploymentId, "archive");
  }

  public CompletableFuture<DeploymentRun> runAsync(String deploymentId) {
    if (isBlank(deploymentId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("deploymentId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("deployments/%s/run", deploymentId), baseUrl);
    DeploymentUpdateParam param = DeploymentUpdateParam.builder().build();
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, DeploymentRun.class));
  }

  public CompletableFuture<CursorPage<DeploymentRun>> listRunsAsync(
      String deploymentId, DeploymentRunListParam param) {
    if (isBlank(deploymentId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("deploymentId is required!"));
    }
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    String query = param.toQueryString();
    String basePath = StringUtils.format("deployments/%s/runs", deploymentId);
    String path = query.isEmpty() ? basePath : basePath + "?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(api, getParam(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<DeploymentRun>>() {}.getType();
              CursorPage<DeploymentRun> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listRunsAsync(
                          deploymentId,
                          DeploymentRunListParam.builder()
                              .limit(param.getLimit())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  private CompletableFuture<Deployment> lifecycleAsync(String deploymentId, String action) {
    if (isBlank(deploymentId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("deploymentId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST,
            StringUtils.format("deployments/%s/%s", deploymentId, action),
            baseUrl);
    DeploymentUpdateParam param = DeploymentUpdateParam.builder().build();
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Deployment.class));
  }

  private GeneralGetParam getParam() {
    return GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isEmpty();
  }
}
