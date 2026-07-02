// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.agentstudio.model.Environment;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.EnvironmentCreateParam;
import com.alibaba.dashscope.agentstudio.param.EnvironmentListParam;
import com.alibaba.dashscope.agentstudio.param.EnvironmentUpdateParam;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class Environments {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;

  public Environments(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
  }

  public Environment create(EnvironmentCreateParam param) {
    return AsyncHelper.joinAndUnwrap(createAsync(param));
  }

  public Environment retrieve(String environmentId) {
    return retrieve(environmentId, null, null);
  }

  public Environment retrieve(String environmentId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(environmentId, apiKey, headers));
  }

  public Environment update(String environmentId, EnvironmentUpdateParam param) {
    return AsyncHelper.joinAndUnwrap(updateAsync(environmentId, param));
  }

  public CursorPage<Environment> list(EnvironmentListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(param));
  }

  public Environment archive(String environmentId) {
    return AsyncHelper.joinAndUnwrap(archiveAsync(environmentId));
  }

  public AgentStudioDeletionStatus delete(String environmentId) {
    return delete(environmentId, null, null);
  }

  public AgentStudioDeletionStatus delete(
      String environmentId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(deleteAsync(environmentId, apiKey, headers));
  }

  public CompletableFuture<Environment> createAsync(EnvironmentCreateParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(HttpMethod.POST, "environments", baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Environment.class));
  }

  public CompletableFuture<Environment> retrieveAsync(String environmentId) {
    return retrieveAsync(environmentId, null, null);
  }

  public CompletableFuture<Environment> retrieveAsync(
      String environmentId, String apiKey, Map<String, String> headers) {
    if (environmentId == null || environmentId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("environmentId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("environments/%s", environmentId), baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Environment.class));
  }

  public CompletableFuture<Environment> updateAsync(
      String environmentId, EnvironmentUpdateParam param) {
    if (environmentId == null || environmentId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("environmentId is required!"));
    }
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("environments/%s", environmentId), baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Environment.class));
  }

  public CompletableFuture<CursorPage<Environment>> listAsync(EnvironmentListParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    String query = param.toQueryString();
    String path = query.isEmpty() ? "environments" : "environments?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<Environment>>() {}.getType();
              CursorPage<Environment> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          EnvironmentListParam.builder()
                              .limit(param.getLimit())
                              .includeArchived(param.getIncludeArchived())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<Environment> archiveAsync(String environmentId) {
    if (environmentId == null || environmentId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("environmentId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("environments/%s/archive", environmentId), baseUrl);
    return AsyncHelper.callAsync(
            api,
            AgentStudioConstants.withApiKey(apiKey, EnvironmentUpdateParam.builder().build()),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Environment.class));
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(String environmentId) {
    return deleteAsync(environmentId, null, null);
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
      String environmentId, String apiKey, Map<String, String> headers) {
    if (environmentId == null || environmentId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("environmentId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.DELETE, StringUtils.format("environments/%s", environmentId), baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, AgentStudioDeletionStatus.class));
  }
}
