// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.agentstudio.model.Session;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.SessionResourceAddParam;
import com.alibaba.dashscope.agentstudio.param.SessionResourceListParam;
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

public final class SessionResources {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;

  public SessionResources(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
  }

  public Session.SessionResource add(String sessionId, SessionResourceAddParam param) {
    return AsyncHelper.joinAndUnwrap(addAsync(sessionId, param));
  }

  public Session.SessionResource retrieve(String sessionId, String resourceId) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(sessionId, resourceId));
  }

  public CursorPage<Session.SessionResource> list(
      String sessionId, SessionResourceListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(sessionId, param));
  }

  public AgentStudioDeletionStatus delete(String sessionId, String resourceId) {
    return AsyncHelper.joinAndUnwrap(deleteAsync(sessionId, resourceId));
  }

  // ── async ──

  public CompletableFuture<Session.SessionResource> addAsync(
      String sessionId, SessionResourceAddParam param) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("sessions/%s/resources", sessionId), baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Session.SessionResource.class));
  }

  public CompletableFuture<Session.SessionResource> retrieveAsync(
      String sessionId, String resourceId) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    if (resourceId == null || resourceId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("resourceId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET,
            StringUtils.format("sessions/%s/resources/%s", sessionId, resourceId),
            baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Session.SessionResource.class));
  }

  public CompletableFuture<CursorPage<Session.SessionResource>> listAsync(
      String sessionId, SessionResourceListParam param) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    if (param == null) {
      param = SessionResourceListParam.builder().build();
    }
    final SessionResourceListParam finalParam = param;
    String query = param.toQueryString();
    String path =
        query.isEmpty()
            ? StringUtils.format("sessions/%s/resources", sessionId)
            : StringUtils.format("sessions/%s/resources?%s", sessionId, query);
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<Session.SessionResource>>() {}.getType();
              CursorPage<Session.SessionResource> page =
                  FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          sessionId,
                          SessionResourceListParam.builder()
                              .limit(finalParam.getLimit())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
      String sessionId, String resourceId) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    if (resourceId == null || resourceId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("resourceId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.DELETE,
            StringUtils.format("sessions/%s/resources/%s", sessionId, resourceId),
            baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, AgentStudioDeletionStatus.class));
  }
}
