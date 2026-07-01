// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.message.Message;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.SessionEventListParam;
import com.alibaba.dashscope.agentstudio.param.SessionEventSendParam;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.ApiKey;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public final class SessionEvents implements Closeable {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;
  private final OkHttpClient streamClientTemplate;

  public SessionEvents(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
    this.streamClientTemplate =
        new OkHttpClient.Builder()
            .connectTimeout(AgentStudioConstants.DEFAULT_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(AgentStudioConstants.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(AgentStudioConstants.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build();
  }

  private String resolveStreamBaseUrl() {
    if (baseUrl != null && !baseUrl.isEmpty()) {
      return baseUrl;
    }
    String envUrl = System.getenv(AgentStudioConstants.ENV_BASE_URL);
    if (envUrl == null || envUrl.isEmpty()) {
      envUrl = System.getenv(AgentStudioConstants.ENV_BASE_URL_ALT);
    }
    if (envUrl != null && !envUrl.isEmpty()) {
      return envUrl;
    }
    return Constants.baseHttpApiUrl;
  }

  public JsonObject send(String sessionId, List<JsonObject> events) {
    return AsyncHelper.joinAndUnwrap(sendAsync(sessionId, events));
  }

  public CursorPage<Message> list(String sessionId, SessionEventListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(sessionId, param));
  }

  public CompletableFuture<JsonObject> sendAsync(String sessionId, List<JsonObject> events) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    if (events == null || events.isEmpty()) {
      return AsyncHelper.failedFuture(
          new IllegalArgumentException("events must contain at least 1 entry"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("sessions/%s/events", sessionId), baseUrl);
    SessionEventSendParam param = SessionEventSendParam.builder().input(events).build();
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(
            result -> {
              Object output = result.getOutput();
              if (output instanceof JsonElement && ((JsonElement) output).isJsonObject()) {
                return ((JsonElement) output).getAsJsonObject();
              }
              return new JsonObject();
            });
  }

  public CompletableFuture<CursorPage<Message>> listAsync(
      String sessionId, SessionEventListParam param) {
    if (sessionId == null || sessionId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("sessionId is required!"));
    }
    String query = param.toQueryString();
    String path = StringUtils.format("sessions/%s/events", sessionId);
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, query.isEmpty() ? path : path + "?" + query, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<Message>>() {}.getType();
              CursorPage<Message> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          sessionId,
                          SessionEventListParam.builder()
                              .types(param.getTypes())
                              .createdAtGt(param.getCreatedAtGt())
                              .createdAtGte(param.getCreatedAtGte())
                              .createdAtLt(param.getCreatedAtLt())
                              .createdAtLte(param.getCreatedAtLte())
                              .limit(param.getLimit())
                              .order(param.getOrder())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public AgentStudioEventStream stream(String sessionId) {
    return stream(sessionId, AgentStudioConstants.DEFAULT_TIMEOUT_MS);
  }

  public AgentStudioEventStream stream(String sessionId, long timeoutMs) {
    String url = resolveStreamBaseUrl();
    if (!url.endsWith("/")) {
      url += "/";
    }
    url += StringUtils.format("sessions/%s/events/stream", sessionId);

    String resolvedKey = resolveApiKey();
    OkHttpClient client =
        streamClientTemplate.newBuilder().readTimeout(timeoutMs, TimeUnit.MILLISECONDS).build();

    Request request =
        new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + resolvedKey)
            .header("Accept", "text/event-stream")
            .get()
            .build();

    return new AgentStudioEventStream(client, request, timeoutMs);
  }

  @Override
  public void close() {
    streamClientTemplate.dispatcher().executorService().shutdown();
    streamClientTemplate.connectionPool().evictAll();
  }

  private String resolveApiKey() {
    try {
      return ApiKey.getApiKey(this.apiKey);
    } catch (NoApiKeyException e) {
      throw new IllegalStateException("No API key found. Set DASHSCOPE_API_KEY or pass apiKey.", e);
    }
  }
}
