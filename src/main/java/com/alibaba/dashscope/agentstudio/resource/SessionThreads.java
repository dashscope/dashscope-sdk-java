// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.message.Message;
import com.alibaba.dashscope.agentstudio.model.Session;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.SessionEventListParam;
import com.alibaba.dashscope.agentstudio.param.SessionThreadListParam;
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

public final class SessionThreads {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;
  private final SessionThreadEvents events;

  public SessionThreads(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
    this.events = new SessionThreadEvents(baseUrl, connectionOptions, apiKey);
  }

  public SessionThreadEvents events() {
    return events;
  }

  // ── sync ──

  public CursorPage<Session.SessionThread> list(String sessionId, SessionThreadListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(sessionId, param));
  }

  public Session.SessionThread retrieve(String sessionId, String threadId) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(sessionId, threadId));
  }

  public Session.SessionThread archive(String sessionId, String threadId) {
    return AsyncHelper.joinAndUnwrap(archiveAsync(sessionId, threadId));
  }

  // ── async ──

  public CompletableFuture<CursorPage<Session.SessionThread>> listAsync(
      String sessionId, SessionThreadListParam param) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    if (param == null) {
      param = SessionThreadListParam.builder().build();
    }
    final SessionThreadListParam finalParam = param;
    String query = param.toQueryString();
    String path =
        query.isEmpty()
            ? StringUtils.format("sessions/%s/threads", sessionId)
            : StringUtils.format("sessions/%s/threads?%s", sessionId, query);
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<Session.SessionThread>>() {}.getType();
              CursorPage<Session.SessionThread> page =
                  FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          sessionId,
                          SessionThreadListParam.builder()
                              .limit(finalParam.getLimit())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<Session.SessionThread> retrieveAsync(String sessionId, String threadId) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    if (threadId == null || threadId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("threadId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET,
            StringUtils.format("sessions/%s/threads/%s", sessionId, threadId),
            baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Session.SessionThread.class));
  }

  public CompletableFuture<Session.SessionThread> archiveAsync(String sessionId, String threadId) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    if (threadId == null || threadId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("threadId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST,
            StringUtils.format("sessions/%s/threads/%s/archive", sessionId, threadId),
            baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Session.SessionThread.class));
  }

  // ── nested: thread-scoped events ──

  public final class SessionThreadEvents {
    private final GeneralApi<HalfDuplexParamBase> threadApi;
    private final String threadBaseUrl;
    private final String threadApiKey;

    SessionThreadEvents(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
      this.threadBaseUrl = baseUrl;
      this.threadApiKey = apiKey;
      this.threadApi =
          connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
    }

    public CursorPage<Message> list(
        String sessionId, String threadId, SessionEventListParam param) {
      return AsyncHelper.joinAndUnwrap(listAsync(sessionId, threadId, param));
    }

    public CompletableFuture<CursorPage<Message>> listAsync(
        String sessionId, String threadId, SessionEventListParam param) {
      if (sessionId == null || sessionId.isEmpty()) {
        return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
      }
      if (threadId == null || threadId.isEmpty()) {
        return AsyncHelper.failedFuture(new InputRequiredException("threadId is required!"));
      }
      if (param == null) {
        param = SessionEventListParam.builder().build();
      }
      final SessionEventListParam finalParam = param;
      String query = param.toQueryString();
      String basePath = StringUtils.format("sessions/%s/threads/%s/events", sessionId, threadId);
      String path = query.isEmpty() ? basePath : basePath + "?" + query;
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(HttpMethod.GET, path, threadBaseUrl);
      return AsyncHelper.callAsync(
              threadApi,
              GeneralGetParam.builder().apiKey(threadApiKey).headers(new HashMap<>()).build(),
              opt)
          .thenApply(
              r -> {
                Type type = new TypeToken<CursorPage<Message>>() {}.getType();
                CursorPage<Message> page = FlattenResultBase.fromDashScopeResult(r, type);
                page.setFetchNext(
                    cursor ->
                        listAsync(
                            sessionId,
                            threadId,
                            SessionEventListParam.builder()
                                .types(finalParam.getTypes())
                                .createdAtGt(finalParam.getCreatedAtGt())
                                .createdAtGte(finalParam.getCreatedAtGte())
                                .createdAtLt(finalParam.getCreatedAtLt())
                                .createdAtLte(finalParam.getCreatedAtLte())
                                .limit(finalParam.getLimit())
                                .order(finalParam.getOrder())
                                .page(cursor)
                                .build()));
                return page;
              });
    }
  }
}
