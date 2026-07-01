// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.Agent;
import com.alibaba.dashscope.agentstudio.model.AgentVersion;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.AgentCreateParam;
import com.alibaba.dashscope.agentstudio.param.AgentListParam;
import com.alibaba.dashscope.agentstudio.param.AgentUpdateParam;
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

public final class Agents {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;

  public Agents(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
  }

  public Agent create(AgentCreateParam param) {
    return AsyncHelper.joinAndUnwrap(createAsync(param));
  }

  public Agent retrieve(String agentId) {
    return retrieve(agentId, null, null, null);
  }

  public Agent retrieve(String agentId, Integer version) {
    return retrieve(agentId, version, null, null);
  }

  public Agent retrieve(String agentId, String apiKey, Map<String, String> headers) {
    return retrieve(agentId, null, apiKey, headers);
  }

  public Agent retrieve(
      String agentId, Integer version, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(agentId, version, apiKey, headers));
  }

  public Agent update(String agentId, AgentUpdateParam param) {
    return AsyncHelper.joinAndUnwrap(updateAsync(agentId, param));
  }

  public CursorPage<Agent> list(AgentListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(param));
  }

  public Agent archive(String agentId) {
    return AsyncHelper.joinAndUnwrap(archiveAsync(agentId));
  }

  public CursorPage<AgentVersion> listVersions(String agentId, AgentListParam param) {
    return AsyncHelper.joinAndUnwrap(listVersionsAsync(agentId, param));
  }

  public CompletableFuture<Agent> createAsync(AgentCreateParam param) {
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(HttpMethod.POST, "agents", baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Agent.class));
  }

  public CompletableFuture<Agent> retrieveAsync(String agentId) {
    return retrieveAsync(agentId, null, null, null);
  }

  public CompletableFuture<Agent> retrieveAsync(String agentId, Integer version) {
    return retrieveAsync(agentId, version, null, null);
  }

  public CompletableFuture<Agent> retrieveAsync(
      String agentId, String apiKey, Map<String, String> headers) {
    return retrieveAsync(agentId, null, apiKey, headers);
  }

  public CompletableFuture<Agent> retrieveAsync(
      String agentId, Integer version, String apiKey, Map<String, String> headers) {
    if (agentId == null || agentId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("agentId is required!"));
    }
    String path = StringUtils.format("agents/%s", agentId);
    if (version != null) {
      path += "?version=" + version;
    }
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Agent.class));
  }

  public CompletableFuture<Agent> updateAsync(String agentId, AgentUpdateParam param) {
    if (agentId == null || agentId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("agentId is required!"));
    }
    try {
      param.validate();
    } catch (InputRequiredException e) {
      return AsyncHelper.failedFuture(e);
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("agents/%s", agentId), baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Agent.class));
  }

  public CompletableFuture<CursorPage<Agent>> listAsync(AgentListParam param) {
    String query = param.toQueryString();
    String path = query.isEmpty() ? "agents" : "agents?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<Agent>>() {}.getType();
              CursorPage<Agent> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          AgentListParam.builder()
                              .limit(param.getLimit())
                              .includeArchived(param.getIncludeArchived())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<Agent> archiveAsync(String agentId) {
    if (agentId == null || agentId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("agentId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("agents/%s/archive", agentId), baseUrl);
    return AsyncHelper.callAsync(
            api, AgentStudioConstants.withApiKey(apiKey, AgentUpdateParam.builder().build()), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Agent.class));
  }

  public CompletableFuture<CursorPage<AgentVersion>> listVersionsAsync(
      String agentId, AgentListParam param) {
    if (agentId == null || agentId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("agentId is required!"));
    }
    String query = param != null ? param.toQueryString() : "";
    String path = StringUtils.format("agents/%s/versions", agentId);
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, query.isEmpty() ? path : path + "?" + query, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<AgentVersion>>() {}.getType();
              CursorPage<AgentVersion> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listVersionsAsync(
                          agentId,
                          AgentListParam.builder()
                              .limit(param != null ? param.getLimit() : null)
                              .includeArchived(param != null ? param.getIncludeArchived() : null)
                              .page(cursor)
                              .build()));
              return page;
            });
  }
}
