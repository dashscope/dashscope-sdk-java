// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol.okhttp;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ErrorType;
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
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import java.io.IOException;
import java.util.Map;
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
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");

  private Status parseStreamEventData(String data) {
    try {
      JsonObject jsonResponse = JsonUtils.parse(data);
      String code = "";
      String message = "";
      String requestId = "";
      if (jsonResponse.has(ApiKeywords.REQUEST_ID)) {
        requestId = jsonResponse.get(ApiKeywords.REQUEST_ID).getAsString();
      }
      if (jsonResponse.has(ApiKeywords.CODE)) {
        code = jsonResponse.get(ApiKeywords.CODE).getAsString();
      }
      if (jsonResponse.has(ApiKeywords.MESSAGE)) {
        message = jsonResponse.get(ApiKeywords.MESSAGE).getAsString();
      }
      return Status.builder()
          .statusCode(400)
          .code(code)
          .message(message)
          .requestId(requestId)
          .isJson(true)
          .build();
    } catch (Throwable e) {
      return Status.builder()
          .statusCode(400)
          .code(ErrorType.RESPONSE_ERROR.getValue())
          .message(data)
          .isJson(false)
          .build();
    }
  }

  private Status parseFailedJson(int statusCode, String body) {
    try {
      JsonObject jsonResponse = JsonUtils.parse(body);
      String code = "";
      String message = "";
      String requestId = "";
      if (jsonResponse.has(ApiKeywords.REQUEST_ID)) {
        requestId = jsonResponse.get(ApiKeywords.REQUEST_ID).getAsString();
      }
      if (jsonResponse.has(ApiKeywords.CODE)) {
        code = jsonResponse.get(ApiKeywords.CODE).getAsString();
      }
      if (jsonResponse.has(ApiKeywords.MESSAGE)) {
        message = jsonResponse.get(ApiKeywords.MESSAGE).getAsString();
      }
      return Status.builder()
          .statusCode(statusCode)
          .code(code)
          .message(message)
          .requestId(requestId)
          .isJson(true)
          .build();
    } catch (Throwable e) {
      return Status.builder()
          .statusCode(statusCode)
          .code(ErrorType.RESPONSE_ERROR.getValue())
          .message(body)
          .isJson(true)
          .build();
    }
  }

  private Status parseFailed(Response response, Throwable th) {
    if (response == null) {
      String message = th == null ? "Get response failed!" : th.getMessage();

      return Status.builder()
          .statusCode(-1)
          .code(ErrorType.NETWORK_ERROR.getValue())
          .message(message)
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
        return Status.builder()
            .statusCode(response.code())
            .code(ErrorType.RESPONSE_ERROR.getValue())
            .message("Failed read response body: " + e.getMessage())
            .isJson(true)
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
        return Status.builder()
            .statusCode(response.code())
            .code(ErrorType.RESPONSE_ERROR.getValue())
            .message(body)
            .isJson(false)
            .build();
      } catch (IOException e) {
        return Status.builder()
            .statusCode(response.code())
            .code(ErrorType.RESPONSE_ERROR.getValue())
            .message("Failed read response body: " + e.getMessage())
            .isJson(true)
            .build();
      }
    } else {
      return Status.builder()
          .statusCode(response.code())
          .code(ErrorType.RESPONSE_ERROR.getValue())
          .message(response.message())
          .isJson(false)
          .build();
    }
  }

  public OkHttpHttpClient(OkHttpClient client) {
    this.client = client;
  }

  private <T extends HalfDuplexParamBase> Request buildRequest(HttpRequest req)
      throws NoApiKeyException, ApiException {
    Request request = null;
    if (req.getHttpMethod() == HttpMethod.GET) {
      HttpUrl.Builder httpBuilder = HttpUrl.parse(req.getUrl()).newBuilder();
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
      requestBuilder.url(req.getUrl()).headers(Headers.of(req.getHeaders()));
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
      requestBuilder.url(req.getUrl()).headers(Headers.of(req.getHeaders()));
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
              .message(String.format("Unsupported method: %s", req.getHttpMethod()))
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
    log.debug(String.format("Event: id %s, type: %s, data: %s", id, eventType, data));
    if (SSEEventType.ERROR.equals(eventType)) {
      Status st = parseStreamEventData(data);
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
      log.debug(String.format("Ignore event id: %s, type: %s, data: %s", id, eventType, data));
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
                          super.onFailure(eventSource, t, response);
                          emitter.onError(new ApiException(parseFailed(response, t), t));
                        }

                        @java.lang.Override
                        public void onClosed(@NotNull EventSource eventSource) {
                          super.onClosed(eventSource);
                          emitter.onComplete();
                        }
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
                log.debug(String.format("Event: id %s, type: %s, data: %s", id, type, data));
                if (SSEEventType.ERROR.equals(type)) {
                  Status st = parseStreamEventData(data);
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
    return false;
  }
}
