// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.agentstudio.model.WebhookEndpoint;
import com.alibaba.dashscope.agentstudio.model.WebhookEndpointList;
import com.alibaba.dashscope.agentstudio.model.WebhookEvent;
import com.alibaba.dashscope.agentstudio.model.WebhookSecretReset;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.WebhookEndpointCreateParam;
import com.alibaba.dashscope.agentstudio.param.WebhookEndpointUpdateParam;
import com.alibaba.dashscope.agentstudio.param.WebhookEventListParam;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/** Managed Agent webhook endpoint APIs. */
public final class WebhookEndpoints {
  /** Relative path of the webhook endpoint collection. */
  private static final String RESOURCE_PATH = "webhook_endpoints";

  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;

  public WebhookEndpoints(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
  }

  public WebhookEndpoint create(WebhookEndpointCreateParam param) {
    return AsyncHelper.joinAndUnwrap(createAsync(param));
  }

  public WebhookEndpoint retrieve(String webhookId) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(webhookId));
  }

  public WebhookEndpoint get(String webhookId) {
    return retrieve(webhookId);
  }

  public WebhookEndpointList list() {
    return AsyncHelper.joinAndUnwrap(listAsync());
  }

  public WebhookEndpoint update(String webhookId, WebhookEndpointUpdateParam param) {
    return AsyncHelper.joinAndUnwrap(updateAsync(webhookId, param));
  }

  public JsonObject delete(String webhookId) {
    return AsyncHelper.joinAndUnwrap(deleteAsync(webhookId));
  }

  public WebhookEndpoint enable(String webhookId) {
    return AsyncHelper.joinAndUnwrap(enableAsync(webhookId));
  }

  public WebhookEndpoint disable(String webhookId) {
    return AsyncHelper.joinAndUnwrap(disableAsync(webhookId));
  }

  public WebhookEvent test(String webhookId) {
    return AsyncHelper.joinAndUnwrap(testAsync(webhookId));
  }

  public WebhookSecretReset resetSecret(String webhookId) {
    return AsyncHelper.joinAndUnwrap(resetSecretAsync(webhookId));
  }

  public CursorPage<WebhookEvent> listEvents(String webhookId) {
    return listEvents(webhookId, WebhookEventListParam.builder().build());
  }

  public CursorPage<WebhookEvent> listEvents(
      String webhookId, WebhookEventListParam param) {
    return AsyncHelper.joinAndUnwrap(listEventsAsync(webhookId, param));
  }

  public CompletableFuture<WebhookEndpoint> createAsync(WebhookEndpointCreateParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption option =
        AgentStudioConstants.newServiceOption(HttpMethod.POST, RESOURCE_PATH, baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), option)
        .thenApply(result -> FlattenResultBase.fromDashScopeResult(result, WebhookEndpoint.class));
  }

  public CompletableFuture<WebhookEndpoint> retrieveAsync(String webhookId) {
    if (isEmpty(webhookId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("webhookId is required!"));
    }
    GeneralServiceOption option =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("%s/%s", RESOURCE_PATH, webhookId), baseUrl);
    return AsyncHelper.callAsync(api, emptyParam(), option)
        .thenApply(result -> FlattenResultBase.fromDashScopeResult(result, WebhookEndpoint.class));
  }

  public CompletableFuture<WebhookEndpoint> getAsync(String webhookId) {
    return retrieveAsync(webhookId);
  }

  public CompletableFuture<WebhookEndpointList> listAsync() {
    GeneralServiceOption option =
        AgentStudioConstants.newServiceOption(HttpMethod.GET, RESOURCE_PATH, baseUrl);
    return AsyncHelper.callAsync(api, emptyParam(), option)
        .thenApply(
            result ->
                FlattenResultBase.fromDashScopeResult(result, WebhookEndpointList.class));
  }

  public CompletableFuture<WebhookEndpoint> updateAsync(
      String webhookId, WebhookEndpointUpdateParam param) {
    if (isEmpty(webhookId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("webhookId is required!"));
    }
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption option =
        AgentStudioConstants.newServiceOption(
            HttpMethod.PUT, StringUtils.format("%s/%s", RESOURCE_PATH, webhookId), baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), option)
        .thenApply(result -> FlattenResultBase.fromDashScopeResult(result, WebhookEndpoint.class));
  }

  public CompletableFuture<JsonObject> deleteAsync(String webhookId) {
    if (isEmpty(webhookId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("webhookId is required!"));
    }
    GeneralServiceOption option =
        AgentStudioConstants.newServiceOption(
            HttpMethod.DELETE, StringUtils.format("%s/%s", RESOURCE_PATH, webhookId), baseUrl);
    return AsyncHelper.callAsync(api, emptyParam(), option)
        .thenApply(result -> (JsonObject) result.getOutput());
  }

  public CompletableFuture<WebhookEndpoint> enableAsync(String webhookId) {
    return endpointActionAsync(webhookId, "enable");
  }

  public CompletableFuture<WebhookEndpoint> disableAsync(String webhookId) {
    return endpointActionAsync(webhookId, "disable");
  }

  public CompletableFuture<WebhookEvent> testAsync(String webhookId) {
    if (isEmpty(webhookId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("webhookId is required!"));
    }
    GeneralServiceOption option = actionOption(webhookId, "test");
    return AsyncHelper.callAsync(api, emptyParam(), option)
        .thenApply(result -> FlattenResultBase.fromDashScopeResult(result, WebhookEvent.class));
  }

  public CompletableFuture<WebhookSecretReset> resetSecretAsync(String webhookId) {
    if (isEmpty(webhookId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("webhookId is required!"));
    }
    GeneralServiceOption option = actionOption(webhookId, "reset_secret");
    return AsyncHelper.callAsync(api, emptyParam(), option)
        .thenApply(
            result -> FlattenResultBase.fromDashScopeResult(result, WebhookSecretReset.class));
  }

  public CompletableFuture<CursorPage<WebhookEvent>> listEventsAsync(String webhookId) {
    return listEventsAsync(webhookId, WebhookEventListParam.builder().build());
  }

  public CompletableFuture<CursorPage<WebhookEvent>> listEventsAsync(
      String webhookId, WebhookEventListParam param) {
    if (isEmpty(webhookId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("webhookId is required!"));
    }
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    String query = param.toQueryString();
    String path = StringUtils.format("%s/%s/events", RESOURCE_PATH, webhookId);
    GeneralServiceOption option =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, query.isEmpty() ? path : path + "?" + query, baseUrl);
    return AsyncHelper.callAsync(api, emptyParam(), option)
        .thenApply(
            result -> {
              Type type = new TypeToken<CursorPage<WebhookEvent>>() {}.getType();
              CursorPage<WebhookEvent> page =
                  FlattenResultBase.fromDashScopeResult(result, type);
              page.setFetchNext(
                  cursor ->
                      listEventsAsync(
                          webhookId,
                          WebhookEventListParam.builder()
                              .limit(param.getLimit())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  private CompletableFuture<WebhookEndpoint> endpointActionAsync(
      String webhookId, String action) {
    if (isEmpty(webhookId)) {
      return AsyncHelper.failedFuture(new InputRequiredException("webhookId is required!"));
    }
    return AsyncHelper.callAsync(api, emptyParam(), actionOption(webhookId, action))
        .thenApply(result -> FlattenResultBase.fromDashScopeResult(result, WebhookEndpoint.class));
  }

  private GeneralServiceOption actionOption(String webhookId, String action) {
    return AgentStudioConstants.newServiceOption(
        HttpMethod.POST,
        StringUtils.format("%s/%s/%s", RESOURCE_PATH, webhookId, action),
        baseUrl);
  }

  private GeneralGetParam emptyParam() {
    return GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build();
  }

  private boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }
}
