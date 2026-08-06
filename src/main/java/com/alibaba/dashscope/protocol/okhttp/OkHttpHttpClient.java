// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol.okhttp;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ClientErrorDef;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.HalfDuplexClient;
import com.alibaba.dashscope.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.HttpRequest;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;

@Slf4j
public final class OkHttpHttpClient implements HalfDuplexClient {
  private final OkHttpClient client;
  private final Set<EventSource> activeEventSources =
      Collections.newSetFromMap(new ConcurrentHashMap<>());
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");

  private Status parseStreamEventData(String data, int httpStatusCode) {
    try {
      JsonObject jsonResponse = JsonUtils.parse(data);
      String code = "";
      String message = "";
      String requestId = "";
      if (jsonResponse.has(ApiKeywords.REQUEST_ID)) {
        requestId = jsonResponse.get(ApiKeywords.REQUEST_ID).getAsString();
      }
      if (jsonResponse.has(ApiKeywords.CODE) && !jsonResponse.get(ApiKeywords.CODE).isJsonNull()) {
        code = jsonResponse.get(ApiKeywords.CODE).getAsString();
      }
      if (jsonResponse.has(ApiKeywords.MESSAGE)) {
        message = jsonResponse.get(ApiKeywords.MESSAGE).getAsString();
      }
      int finalStatusCode = resolveErrorStatusCode(httpStatusCode, code);
      return Status.builder()
          .statusCode(finalStatusCode)
          .code(code)
          .message(message)
          .requestId(requestId)
          .isJson(true)
          .build();
    } catch (Throwable e) {
      return Status.builder()
          .statusCode(httpStatusCode)
          .code("")
          .message(data)
          .isJson(false)
          .build();
    }
  }

  private Status parseFailedJson(int httpStatusCode, String body) {
    try {
      JsonObject jsonResponse = JsonUtils.parse(body);
      String code = "";
      String message = "";
      String requestId = "";
      if (jsonResponse.has(ApiKeywords.REQUEST_ID)) {
        requestId = jsonResponse.get(ApiKeywords.REQUEST_ID).getAsString();
      } else if (jsonResponse.has("requestId")) {
        requestId = jsonResponse.get("requestId").getAsString();
      }
      if (jsonResponse.has(ApiKeywords.CODE) && !jsonResponse.get(ApiKeywords.CODE).isJsonNull()) {
        code = jsonResponse.get(ApiKeywords.CODE).getAsString();
      }
      if (jsonResponse.has(ApiKeywords.MESSAGE)) {
        message = jsonResponse.get(ApiKeywords.MESSAGE).getAsString();
      }
      if ((code == null || code.isEmpty())
          && (message == null || message.isEmpty())
          && jsonResponse.has(ApiKeywords.ERROR)
          && jsonResponse.get(ApiKeywords.ERROR).isJsonObject()) {
        JsonObject error = jsonResponse.getAsJsonObject(ApiKeywords.ERROR);
        if (error.has(ApiKeywords.CODE)) {
          code = error.get(ApiKeywords.CODE).getAsString();
        }
        if (error.has(ApiKeywords.MESSAGE)) {
          message = error.get(ApiKeywords.MESSAGE).getAsString();
        }
      }
      return Status.builder()
          .statusCode(finalStatusCode)
          .code(code)
          .message(message)
          .requestId(requestId)
          .isJson(true)
          .build();
    } catch (Throwable e) {
      // Try to extract code/message even if standard parsing failed
      String extractedCode = "";
      String extractedMessage = body;
      try {
        JsonObject json = JsonUtils.parse(body);
        if (json.has(ApiKeywords.CODE)) {
          extractedCode = json.get(ApiKeywords.CODE).getAsString();
        }
        if (json.has(ApiKeywords.MESSAGE)) {
          extractedMessage = json.get(ApiKeywords.MESSAGE).getAsString();
        }
      } catch (Exception ex) {
        // Parsing failed, use defaults
      }

      // If we have a business error code, try to map it to the correct status code
      int finalStatusCode = resolveErrorStatusCode(httpStatusCode, extractedCode);

      return Status.builder()
          .statusCode(finalStatusCode)
          .code(extractedCode.isEmpty() ? "" : extractedCode)
          .message(extractedMessage)
          .isJson(!extractedCode.isEmpty())
          .build();
    }
  }

  /**
   * Resolve the appropriate HTTP status code when a business error code is present. If
   * httpStatusCode is already a non-200 error, use it directly. Otherwise, try to map the business
   * error code to a proper status code.
   */
  private int resolveErrorStatusCode(int httpStatusCode, String errorCode) {
    // If HTTP status is already an error code, use it
    if (httpStatusCode >= 400) {
      return httpStatusCode;
    }
    // HTTP status is 2xx (e.g., SSE stream connected with 200) but we have a business error
    if (errorCode != null && !errorCode.isEmpty()) {
      // Try exact match against ClientErrorDef
      ClientErrorDef errorDef = ClientErrorDef.fromErrorCode(errorCode);
      if (errorDef != null) {
        return errorDef.getStatusCode();
      }
      // Try keyword match for legacy/non-standard error codes
      for (Map.Entry<String, Integer> entry : LEGACY_ERROR_KEYWORDS.entrySet()) {
        if (errorCode.contains(entry.getKey())) {
          return entry.getValue();
        }
      }
      // Has error code but no mapping found - default to 400 (client-side business error)
      return 400;
    }
    return httpStatusCode;
  }

  /** Keyword-to-status mapping for legacy / non-standard error codes. */
  private static final Map<String, Integer> LEGACY_ERROR_KEYWORDS = new java.util.LinkedHashMap<>();

  static {
    LEGACY_ERROR_KEYWORDS.put("InvalidParameter", 400);
    LEGACY_ERROR_KEYWORDS.put("BadRequest", 400);
    LEGACY_ERROR_KEYWORDS.put("DataInspection", 400);
    LEGACY_ERROR_KEYWORDS.put("Inspection", 400);
    LEGACY_ERROR_KEYWORDS.put("Unauthorized", 401);
    LEGACY_ERROR_KEYWORDS.put("ApiKey", 401);
    LEGACY_ERROR_KEYWORDS.put("Forbidden", 403);
    LEGACY_ERROR_KEYWORDS.put("AccessDenied", 403);
    LEGACY_ERROR_KEYWORDS.put("NotFound", 404);
    LEGACY_ERROR_KEYWORDS.put("Throttling", 429);
    LEGACY_ERROR_KEYWORDS.put("RateLimit", 429);
    LEGACY_ERROR_KEYWORDS.put("InternalError", 500);
    LEGACY_ERROR_KEYWORDS.put("SystemError", 500);
  }

  /**
   * Map HTTP status code to corresponding ClientErrorDef. Falls back to INTERNAL_ERROR if no
   * specific mapping found.
   */
  private ClientErrorDef mapStatusCodeToErrorDef(int statusCode) {
    for (ClientErrorDef errorDef : ClientErrorDef.values()) {
      if (errorDef.getStatusCode() == statusCode) {
        return errorDef;
      }
    }
    // Default fallback based on status code ranges
    if (statusCode >= 400 && statusCode < 500) {
      return ClientErrorDef.INVALID_REQUEST;
    } else if (statusCode >= 500) {
      return ClientErrorDef.INTERNAL_ERROR;
    }
    return ClientErrorDef.INTERNAL_ERROR;
  }

  private Status parseFailed(Response response, Throwable th) {
    if (response == null) {
      String message = th == null ? "Get response failed!" : th.getMessage();

      return Status.builder()
          .statusCode(ClientErrorDef.SERVICE_UNAVAILABLE.getStatusCode())
          .code(ClientErrorDef.SERVICE_UNAVAILABLE.getErrorCode())
          .message(
              StringUtils.format(
                  "%s [reason=no_response, detail=%s]",
                  ClientErrorDef.SERVICE_UNAVAILABLE.getErrorMsg(),
                  (message != null ? message : "Unknown")))
          .isJson(false)
          .build();
    }
    String contentType = response.header("Content-Type");
    // process http failed.
    if (contentType != null && (contentType.toLowerCase().contains("application/json"))) {
      String body;
      try {
        body = response.body().string();
      } catch (IOException e) {
        ClientErrorDef errorDef = mapStatusCodeToErrorDef(response.code());
        return Status.builder()
            .statusCode(errorDef.getStatusCode())
            .code(errorDef.getErrorCode())
            .message(
                StringUtils.format(
                    "%s [http_status=%d, reason=body_read_failed, detail=%s]",
                    errorDef.getErrorMsg(), response.code(), e.getMessage()))
            .isJson(false)
            .build();
      }
      return parseFailedJson(response.code(), body);
    } else if (contentType != null && contentType.toLowerCase().contains("text/event-stream")) {
      try {
        String body = response.body().string();
        for (String part : body.split("\n")) {
          part = part.trim();
          if (part.startsWith("data:")) {
            body = part.replace("data:", "");
            return parseFailedJson(response.code(), body);
          }
        }
        ClientErrorDef errorDef = mapStatusCodeToErrorDef(response.code());
        return Status.builder()
            .statusCode(errorDef.getStatusCode())
            .code(errorDef.getErrorCode())
            .message(
                StringUtils.format(
                    "%s [http_status=%d, content_type=text/event-stream, body=%s]",
                    errorDef.getErrorMsg(),
                    response.code(),
                    (body.isEmpty() ? response.message() : body)))
            .isJson(false)
            .build();
      } catch (IOException e) {
        ClientErrorDef errorDef = mapStatusCodeToErrorDef(response.code());
        return Status.builder()
            .statusCode(errorDef.getStatusCode())
            .code(errorDef.getErrorCode())
            .message(
                StringUtils.format(
                    "%s [http_status=%d, reason=sse_body_read_failed, detail=%s]",
                    errorDef.getErrorMsg(), response.code(), e.getMessage()))
            .isJson(false)
            .build();
      }
    } else {
      String body = "";
      try {
        body = response.body().string();
      } catch (IOException e) {
        log.debug("Failed to read non-JSON response body", e);
      }

      // Try to extract code/message from body even if Content-Type is not application/json
      String extractedCode = "";
      String extractedMessage = body.isEmpty() ? response.message() : body;

      try {
        JsonObject json = JsonUtils.parse(body);
        if (json.has(ApiKeywords.CODE)) {
          extractedCode = json.get(ApiKeywords.CODE).getAsString();
        }
        if (json.has(ApiKeywords.MESSAGE)) {
          extractedMessage = json.get(ApiKeywords.MESSAGE).getAsString();
        }
      } catch (Exception ex) {
        // Parsing failed, use defaults
      }

      ClientErrorDef errorDef = mapStatusCodeToErrorDef(response.code());
      return Status.builder()
          .statusCode(response.code())
          .code(extractedCode.isEmpty() ? errorDef.getErrorCode() : extractedCode)
          .message(extractedMessage)
          .isJson(!extractedCode.isEmpty())
          .build();
    }
  }

  public OkHttpHttpClient(OkHttpClient client) {
    this.client = client;
  }

  private <T extends HalfDuplexParamBase> Request buildRequest(HttpRequest req)
      throws NoApiKeyException, ApiException {
    // Validate URL before building request to provide clear error message
    String url = req.getUrl();
    if (url == null || url.isEmpty()) {
      throw new ApiException(
          Status.builder()
              .statusCode(ClientErrorDef.INVALID_URL.getStatusCode())
              .code(ClientErrorDef.INVALID_URL.getErrorCode())
              .message(
                  StringUtils.format(
                      "%s [detail=URL is null or empty]", ClientErrorDef.INVALID_URL.getErrorMsg()))
              .build());
    }
    HttpUrl parsedUrl = HttpUrl.parse(url);
    if (parsedUrl == null) {
      throw new ApiException(
          Status.builder()
              .statusCode(ClientErrorDef.INVALID_URL.getStatusCode())
              .code(ClientErrorDef.INVALID_URL.getErrorCode())
              .message(
                  StringUtils.format(
                      "%s [detail=%s]", ClientErrorDef.INVALID_URL.getErrorMsg(), url))
              .build());
    }

    Request request = null;
    if (req.getHttpMethod() == HttpMethod.GET) {
      HttpUrl.Builder httpBuilder = parsedUrl.newBuilder();
      if (req.getParameters() != null) {
        for (Map.Entry<String, Object> entry : req.getParameters().entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue().toString();
          httpBuilder.addQueryParameter(key, value);
        }
      }
      request =
          new Request.Builder()
              .url(httpBuilder.build())
              .headers(Headers.of(req.getHeaders()))
              .build();
    } else if (req.getHttpMethod() == HttpMethod.POST) {
      Builder requestBuilder = new Request.Builder();
      requestBuilder.url(parsedUrl).headers(Headers.of(req.getHeaders()));
      if (req.getBody() != null) {
        // compatible with okhttp3.x
        // RequestBody.create((String) (req.getBody()), MEDIA_TYPE_APPLICATION_JSON));
        requestBuilder.post(
            RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, (String) (req.getBody())));
      } else {
        requestBuilder.post(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, ""));
      }
      request = requestBuilder.build();
    } else if (req.getHttpMethod() == HttpMethod.DELETE) {
      Builder requestBuilder = new Request.Builder();
      requestBuilder.url(parsedUrl).headers(Headers.of(req.getHeaders()));
      if (req.getBody() != null) {
        requestBuilder.delete(
            // RequestBody.create((String) (req.getBody()), MEDIA_TYPE_APPLICATION_JSON));
            RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, (String) req.getBody()));
      } else {
        requestBuilder.delete();
      }
      request = requestBuilder.build();
    } else {
      Status status =
          Status.builder()
              .statusCode(400)
              .code("BadRequest")
              .message(StringUtils.format("Unsupported method: %s", req.getHttpMethod()))
              .build();
      throw new ApiException(status);
    }
    return request;
  }

  /*
   * Send blocking and get
   */
  @Override
  public DashScopeResult send(HalfDuplexRequest req) throws NoApiKeyException, ApiException {
    try {
      Request request = buildRequest(req.getHttpRequest());
      Response response = client.newCall(request).execute();
      if (!response.isSuccessful()) {
        Status status = parseFailed(response, null);
        throw new ApiException(status);
      }
      return new DashScopeResult()
          .fromResponse(
              Protocol.HTTP,
              NetworkResponse.builder()
                  .headers(response.headers().toMultimap())
                  .message(response.body().string())
                  .httpStatusCode(response.code())
                  .build(),
              req.getIsFlatten(),
              req);
    } catch (ApiException e) {
      throw e;
    } catch (NoApiKeyException e) {
      throw e;
    } catch (Throwable e) {
      throw new ApiException(e);
    }
  }

  @Override
  public void send(HalfDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException {
    Request request = buildRequest(req.getHttpRequest());
    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                callback.onError(e);
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                  if (!response.isSuccessful()) {
                    Status status = parseFailed(response, null);
                    callback.onError(new ApiException(status));
                  } else {
                    callback.onEvent(
                        new DashScopeResult()
                            .fromResponse(
                                Protocol.HTTP,
                                NetworkResponse.builder()
                                    .headers(response.headers().toMultimap())
                                    .message(response.body().string())
                                    .httpStatusCode(response.code())
                                    .build(),
                                req.getIsFlatten(),
                                req));
                    callback.onComplete();
                  }
                }
              }
            });
  }

  private void handleSSEEvent(
      FlowableEmitter<DashScopeResult> emitter,
      String id,
      String eventType,
      String data,
      boolean isFlattenResult,
      Response response,
      HalfDuplexRequest req) {
    log.debug(StringUtils.format("Event: id %s, type: %s, data: %s", id, eventType, data));
    if (SSEEventType.ERROR.equals(eventType)) {
      Status st = parseStreamEventData(data, response.code());
      emitter.onError(new ApiException(st));
    } else if (SSEEventType.DATA.equals(eventType) || SSEEventType.RESULT.equals(eventType)) {
      emitter.onNext(
          new DashScopeResult()
              .fromResponse(
                  Protocol.HTTP,
                  NetworkResponse.builder()
                      .headers(response.headers().toMultimap())
                      .message(data)
                      .event(eventType)
                      .httpStatusCode(response.code())
                      .build(),
                  isFlattenResult,
                  req));
    } else if (SSEEventType.DONE.equals(eventType)) { // event done ignore message
      log.debug(StringUtils.format("Ignore event id: %s, type: %s, data: %s", id, eventType, data));
    } else if (eventType != null) {
      // process assistant events.
      emitter.onNext(
          new DashScopeResult()
              .fromResponse(
                  Protocol.HTTP,
                  NetworkResponse.builder()
                      .headers(response.headers().toMultimap())
                      .message(data)
                      .event(eventType)
                      .httpStatusCode(response.code())
                      .build(),
                  isFlattenResult,
                  req));
    } else if (eventType == null) {
      if (data.equals("[DONE]")) {
        emitter.onComplete();
        return;
      }
      emitter.onNext(
          new DashScopeResult()
              .fromResponse(
                  Protocol.HTTP,
                  NetworkResponse.builder()
                      .headers(response.headers().toMultimap())
                      .message(data)
                      .httpStatusCode(response.code())
                      .build(),
                  isFlattenResult,
                  req));
    }
  }

  @Override
  public Flowable<DashScopeResult> streamOut(HalfDuplexRequest req)
      throws NoApiKeyException, ApiException {
    Flowable<DashScopeResult> flowable =
        Flowable.<DashScopeResult>create(
            emitter -> {
              Request request = buildRequest(req.getHttpRequest());
              AtomicBoolean terminated = new AtomicBoolean(false);
              EventSource eventSource =
                  EventSources.createFactory(client)
                      .newEventSource(
                          request,
                          new EventSourceListener() {
                            private Response response;

                            @java.lang.Override
                            public void onEvent(
                                EventSource eventSource,
                                java.lang.String id,
                                java.lang.String type,
                                java.lang.String data) {
                              handleSSEEvent(
                                  emitter, id, type, data, req.getIsFlatten(), response, req);
                            }

                            @java.lang.Override
                            public void onOpen(
                                @NotNull EventSource eventSource, @NotNull Response response) {
                              this.response = response;
                              super.onOpen(eventSource, response);
                            }

                            @java.lang.Override
                            public void onFailure(
                                @NotNull EventSource eventSource,
                                java.lang.Throwable t,
                                Response response) {
                              this.response = response;
                              terminated.set(true);
                              activeEventSources.remove(eventSource);
                              super.onFailure(eventSource, t, response);
                              emitter.onError(new ApiException(parseFailed(response, t), t));
                            }

                            @java.lang.Override
                            public void onClosed(@NotNull EventSource eventSource) {
                              terminated.set(true);
                              activeEventSources.remove(eventSource);
                              super.onClosed(eventSource);
                              emitter.onComplete();
                            }
                          });
              if (terminated.get()) {
                activeEventSources.remove(eventSource);
              } else {
                activeEventSources.add(eventSource);
                if (terminated.get()) {
                  activeEventSources.remove(eventSource);
                }
              }
              emitter.setCancellable(
                  () -> {
                    activeEventSources.remove(eventSource);
                    eventSource.cancel();
                  });
            },
            BackpressureStrategy.BUFFER);
    return flowable;
  }

  private class SSEEventType {
    public static final String ERROR = "error";
    public static final String DATA = "data";
    public static final String DONE = "done";
    public static final String RESULT = "result";
  }

  @Override
  public void streamOut(HalfDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException {
    Request request = buildRequest(req.getHttpRequest());
    EventSources.createFactory(client)
        .newEventSource(
            request,
            new EventSourceListener() {
              private Response response;

              @java.lang.Override
              public void onEvent(
                  EventSource eventSource,
                  java.lang.String id,
                  java.lang.String type,
                  java.lang.String data) {
                log.debug(StringUtils.format("Event: id %s, type: %s, data: %s", id, type, data));
                if (SSEEventType.ERROR.equals(type)) {
                  Status st = parseStreamEventData(data, response.code());
                  callback.onError(new ApiException(st));
                } else if (SSEEventType.DATA.equals(type) || SSEEventType.RESULT.equals(type)) {
                  callback.onEvent(
                      new DashScopeResult()
                          .fromResponse(
                              Protocol.HTTP,
                              NetworkResponse.builder()
                                  .headers(response.headers().toMultimap())
                                  .message(data)
                                  .event(type)
                                  .httpStatusCode(response.code())
                                  .build(),
                              req.getIsFlatten(),
                              req));
                } else if (type != null) {
                  callback.onEvent(
                      new DashScopeResult()
                          .fromResponse(
                              Protocol.HTTP,
                              NetworkResponse.builder()
                                  .headers(response.headers().toMultimap())
                                  .message(data)
                                  .event(type)
                                  .httpStatusCode(response.code())
                                  .build(),
                              req.getIsFlatten(),
                              req));
                } else if (type == null) {
                  callback.onEvent(
                      new DashScopeResult()
                          .fromResponse(
                              Protocol.HTTP,
                              NetworkResponse.builder()
                                  .headers(response.headers().toMultimap())
                                  .message(data)
                                  .httpStatusCode(response.code())
                                  .build(),
                              req.getIsFlatten(),
                              req));
                }
              }

              @java.lang.Override
              public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                this.response = response;
                callback.onOpen(null);
              }

              @java.lang.Override
              public void onFailure(
                  @NotNull EventSource eventSource, java.lang.Throwable t, Response response) {
                this.response = response;
                callback.onError(new ApiException(parseFailed(response, t), t));
              }

              @java.lang.Override
              public void onClosed(EventSource eventSource) {
                callback.onComplete();
              }
            });
  }

  @Override
  public boolean close(int code, String reason) {
    if (activeEventSources.isEmpty()) {
      return false;
    }
    for (EventSource es : activeEventSources) {
      es.cancel();
    }
    activeEventSources.clear();
    return true;
  }
}
