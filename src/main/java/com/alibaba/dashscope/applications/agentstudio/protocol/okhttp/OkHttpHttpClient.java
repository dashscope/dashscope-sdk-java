// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.protocol.okhttp;

import com.alibaba.dashscope.applications.agentstudio.common.DashScopeResult;
import com.alibaba.dashscope.applications.agentstudio.common.ErrorType;
import com.alibaba.dashscope.applications.agentstudio.common.ResultCallback;
import com.alibaba.dashscope.applications.agentstudio.common.Status;
import com.alibaba.dashscope.applications.agentstudio.exception.ApiException;
import com.alibaba.dashscope.applications.agentstudio.exception.NoApiKeyException;
import com.alibaba.dashscope.applications.agentstudio.protocol.HalfDuplexClient;
import com.alibaba.dashscope.applications.agentstudio.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.applications.agentstudio.protocol.HttpMethod;
import com.alibaba.dashscope.applications.agentstudio.protocol.HttpRequest;
import com.alibaba.dashscope.applications.agentstudio.protocol.NetworkResponse;
import com.alibaba.dashscope.applications.agentstudio.protocol.Protocol;
import com.alibaba.dashscope.applications.agentstudio.utils.ApiKeywords;
import com.alibaba.dashscope.applications.agentstudio.utils.JsonUtils;
import com.alibaba.dashscope.applications.agentstudio.utils.StringUtils;
import com.google.gson.JsonObject;
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

/** OkHttp-based half-duplex HTTP client for AgentStudio. */
@Slf4j
public final class OkHttpHttpClient implements HalfDuplexClient {
  private final OkHttpClient client;
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");

  private Status parseFailedJson(int statusCode, String body) {
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
      if (jsonResponse.has(ApiKeywords.CODE)) {
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

  private Status tryParseErrorJson(int statusCode, String body) {
    try {
      JsonObject jsonResponse = JsonUtils.parse(body);
      if (jsonResponse == null
          || (!jsonResponse.has(ApiKeywords.CODE)
              && !jsonResponse.has(ApiKeywords.MESSAGE)
              && !jsonResponse.has(ApiKeywords.ERROR))) {
        return null;
      }
      return parseFailedJson(statusCode, body);
    } catch (Throwable e) {
      return null;
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
      String body = null;
      try {
        body = response.body().string();
      } catch (Throwable e) {
        // The body may be partially consumed or the connection already broken.
      }
      if (body != null) {
        for (String part : body.split("\n")) {
          part = part.trim();
          if (part.startsWith("data:")) {
            return parseFailedJson(response.code(), part.substring("data:".length()).trim());
          }
        }
        // The gateway may write a bare JSON error without SSE framing.
        Status jsonStatus = tryParseErrorJson(response.code(), body);
        if (jsonStatus != null) {
          return jsonStatus;
        }
      }
      String message;
      if (body != null && !body.isEmpty()) {
        message = body;
      } else if (th != null && th.getMessage() != null) {
        message = th.getMessage();
      } else {
        message = response.message();
      }
      return Status.builder()
          .statusCode(response.code())
          .code(ErrorType.RESPONSE_ERROR.getValue())
          .message(message)
          .isJson(false)
          .build();
    } else {
      // The error body may still be JSON even if the content type says otherwise.
      String body = null;
      try {
        if (response.body() != null) {
          body = response.peekBody(16 * 1024).string();
        }
      } catch (Throwable e) {
        // Ignore unreadable bodies and fall back to the HTTP status message.
      }
      if (body != null && !body.isEmpty()) {
        Status jsonStatus = tryParseErrorJson(response.code(), body);
        if (jsonStatus != null) {
          return jsonStatus;
        }
      }
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

  private Request buildRequest(HttpRequest req) throws NoApiKeyException, ApiException {
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
    } else if (req.getHttpMethod() == HttpMethod.PUT) {
      Builder requestBuilder = new Request.Builder();
      requestBuilder.url(req.getUrl()).headers(Headers.of(req.getHeaders()));
      requestBuilder.put(
          RequestBody.create(
              MEDIA_TYPE_APPLICATION_JSON, req.getBody() == null ? "" : (String) req.getBody()));
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
}
