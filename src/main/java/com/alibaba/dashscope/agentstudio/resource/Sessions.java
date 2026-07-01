// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.agentstudio.model.Session;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.SessionCreateParam;
import com.alibaba.dashscope.agentstudio.param.SessionListParam;
import com.alibaba.dashscope.agentstudio.param.SessionUpdateParam;
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

public final class Sessions {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;
  private final SessionEvents events;

  public Sessions(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
    this.events = new SessionEvents(baseUrl, connectionOptions, apiKey);
  }

  public SessionEvents events() {
    return events;
  }

  public Session create(SessionCreateParam param) {
    return AsyncHelper.joinAndUnwrap(createAsync(param));
  }

  public Session retrieve(String sessionId) {
    return retrieve(sessionId, null, null);
  }

  public Session retrieve(String sessionId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(sessionId, apiKey, headers));
  }

  public Session update(String sessionId, SessionUpdateParam param) {
    return AsyncHelper.joinAndUnwrap(updateAsync(sessionId, param));
  }

  public CursorPage<Session> list(SessionListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(param));
  }

  public Session archive(String sessionId) {
    return AsyncHelper.joinAndUnwrap(archiveAsync(sessionId));
  }

  public AgentStudioDeletionStatus delete(String sessionId) {
    return delete(sessionId, null, null);
  }

  public AgentStudioDeletionStatus delete(
      String sessionId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(deleteAsync(sessionId, apiKey, headers));
  }

  public CompletableFuture<Session> createAsync(SessionCreateParam param) {
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(HttpMethod.POST, "sessions", baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Session.class));
  }

  public CompletableFuture<Session> retrieveAsync(String sessionId) {
    return retrieveAsync(sessionId, null, null);
  }

  public CompletableFuture<Session> retrieveAsync(
      String sessionId, String apiKey, Map<String, String> headers) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("sessions/%s", sessionId), baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Session.class));
  }

  public CompletableFuture<Session> updateAsync(String sessionId, SessionUpdateParam param) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("sessions/%s", sessionId), baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Session.class));
  }

  public CompletableFuture<CursorPage<Session>> listAsync(SessionListParam param) {
    String query = param.toQueryString();
    String path = query.isEmpty() ? "sessions" : "sessions?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<Session>>() {}.getType();
              CursorPage<Session> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          SessionListParam.builder()
                              .limit(param.getLimit())
                              .agentId(param.getAgentId())
                              .statuses(param.getStatuses())
                              .createdAtGt(param.getCreatedAtGt())
                              .createdAtGte(param.getCreatedAtGte())
                              .createdAtLt(param.getCreatedAtLt())
                              .createdAtLte(param.getCreatedAtLte())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<Session> archiveAsync(String sessionId) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("sessions/%s/archive", sessionId), baseUrl);
    return AsyncHelper.callAsync(
            api, AgentStudioConstants.withApiKey(apiKey, SessionUpdateParam.builder().build()), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Session.class));
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(String sessionId) {
    return deleteAsync(sessionId, null, null);
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
      String sessionId, String apiKey, Map<String, String> headers) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.DELETE, StringUtils.format("sessions/%s", sessionId), baseUrl);
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
