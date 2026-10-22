// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.api;

import com.alibaba.dashscope.resources.agentstudio.support.base.HalfDuplexParamBase;
import com.alibaba.dashscope.resources.agentstudio.support.common.DashScopeResult;
import com.alibaba.dashscope.resources.agentstudio.support.common.ResultCallback;
import com.alibaba.dashscope.resources.agentstudio.support.common.Status;
import com.alibaba.dashscope.resources.agentstudio.support.exception.ApiException;
import com.alibaba.dashscope.resources.agentstudio.support.exception.NoApiKeyException;
import com.alibaba.dashscope.resources.agentstudio.support.protocol.ConnectionOptions;
import com.alibaba.dashscope.resources.agentstudio.support.protocol.HttpMethod;
import com.alibaba.dashscope.resources.agentstudio.support.protocol.ServiceOption;
import com.alibaba.dashscope.resources.agentstudio.support.utils.ApiKey;
import com.alibaba.dashscope.resources.agentstudio.support.utils.JsonUtils;
import com.alibaba.dashscope.shared.EnvNames;
import com.alibaba.dashscope.shared.SdkInfo;
import com.alibaba.dashscope.shared.SdkSession;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 3.0 copy of the 2.x GeneralApi HTTP call path for the agentstudio subtree (design §7
 * 仅引用式搬迁): same URL join, header set (auth, user-agent, sdk tracking, workspace), JSON body
 * handling and error mapping as the 2.x GeneralApi→HalfDuplexRequest→OkHttpHttpClient chain for
 * the reachable configuration (HTTP, flatten results, no request encryption — agentstudio params
 * never enable it).
 */
public final class GeneralApi<ParamT extends HalfDuplexParamBase> {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  private static final String SDK_CLIENT = "java-sdk";

  private final OkHttpClient client;

  /** Create default http client. */
  public GeneralApi() {
    this.client = defaultClient();
  }

  /**
   * Create custom http client
   *
   * @param connectionOptions The client option.
   */
  public GeneralApi(ConnectionOptions connectionOptions) {
    this.client = newClient(connectionOptions);
  }

  /**
   * Call the server asynchronously, result delivered to the callback.
   *
   * @param param The input param.
   * @param serviceOption The service options.
   * @param callback The callback to receive the response.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void call(
      ParamT param, ServiceOption serviceOption, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException {
    Request request = buildRequest(param, serviceOption);
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
                try (okhttp3.ResponseBody responseBody = response.body()) {
                  if (!response.isSuccessful()) {
                    callback.onError(new ApiException(parseFailed(response, null)));
                  } else {
                    callback.onEvent(
                        DashScopeResult.fromHttpResponse(
                            response.code(),
                            response.headers().toMultimap(),
                            responseBody != null ? responseBody.string() : "",
                            null));
                    callback.onComplete();
                  }
                }
              }
            });
  }

  private Request buildRequest(ParamT param, ServiceOption serviceOption)
      throws NoApiKeyException, ApiException {
    Map<String, String> headers = buildHttpHeaders(param, serviceOption);
    String url = httpUrl(serviceOption);
    HttpMethod method = serviceOption.getHttpMethod();
    if (method == HttpMethod.GET) {
      HttpUrl.Builder httpBuilder = HttpUrl.parse(url).newBuilder();
      if (param.getParameters() != null) {
        for (Map.Entry<String, Object> entry : param.getParameters().entrySet()) {
          httpBuilder.addQueryParameter(entry.getKey(), entry.getValue().toString());
        }
      }
      return new Request.Builder()
          .url(httpBuilder.build())
          .headers(Headers.of(headers))
          .build();
    }
    JsonObject httpBody = param.getHttpBody();
    String body =
        httpBody == null
            ? null
            : param.shouldSerializeExplicitNulls() ? httpBody.toString() : JsonUtils.toJson(httpBody);
    Request.Builder requestBuilder =
        new Request.Builder().url(url).headers(Headers.of(headers));
    if (method == HttpMethod.POST) {
      requestBuilder.post(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, body == null ? "" : body));
    } else if (method == HttpMethod.PUT) {
      requestBuilder.put(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, body == null ? "" : body));
    } else if (method == HttpMethod.DELETE) {
      if (body != null) {
        requestBuilder.delete(RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, body));
      } else {
        requestBuilder.delete();
      }
    } else {
      Status status =
          Status.builder()
              .statusCode(400)
              .code("BadRequest")
              .message(String.format("Unsupported method: %s", method))
              .build();
      throw new ApiException(status);
    }
    return requestBuilder.build();
  }

  private static String httpUrl(ServiceOption serviceOption) {
    String baseUrl = com.alibaba.dashscope.utils.Constants.baseHttpApiUrl;
    if (serviceOption.getBaseHttpUrl() != null) {
      baseUrl = serviceOption.getBaseHttpUrl();
    }
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    String path = serviceOption.httpUrl();
    // Avoid duplicate path when baseUrl already contains the full path
    if (path != null && !path.isEmpty() && baseUrl.endsWith(path)) {
      return baseUrl;
    }
    return baseUrl + path;
  }

  private Map<String, String> buildHttpHeaders(ParamT param, ServiceOption serviceOption)
      throws NoApiKeyException {
    // Extract and filter custom user agent from param headers
    Map<String, String> paramHeaders = param.getHeaders();
    String customUserAgent = paramHeaders != null ? paramHeaders.get("user-agent") : null;
    Map<String, String> filteredHeaders =
        paramHeaders != null ? new HashMap<>(paramHeaders) : new HashMap<>();
    filteredHeaders.remove("user-agent");

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + ApiKey.getApiKey(param.getApiKey()));
    headers.put("user-agent", userAgent(customUserAgent));
    if (!isSdkHeadersDisabled()) {
      String sdkClient = SDK_CLIENT + "/" + SdkInfo.VERSION;
      String module = serviceOption.getModule();
      if (module != null && !module.isEmpty()) {
        sdkClient += "/" + module;
      }
      headers.put("x-dashscope-sdk-client", sdkClient);
      headers.put("x-dashscope-sdk-session-id", SdkSession.ID);
    }
    if (param.isSecurityCheck()) {
      headers.put("X-DashScope-DataInspection", "enable");
    }
    if (param.getWorkspace() != null && !param.getWorkspace().isEmpty()) {
      headers.put("X-DashScope-WorkSpace", param.getWorkspace());
    }
    headers.put("Content-Type", "application/json");
    if (!filteredHeaders.isEmpty()) {
      headers.putAll(filteredHeaders);
    }
    return headers;
  }

  private static String userAgent(String customUserAgent) {
    String userAgent =
        String.format(
            "dashscope/%s; java/%s; platform/%s; processor/%s",
            SdkInfo.VERSION,
            System.getProperty("java.version"),
            System.getProperty("os.name"),
            System.getProperty("os.arch"));
    if (customUserAgent != null && !customUserAgent.isEmpty()) {
      userAgent += "; " + customUserAgent;
    }
    return userAgent;
  }

  private static boolean isSdkHeadersDisabled() {
    String disable = System.getenv("DASHSCOPE_DISABLE_SDK_HEADERS");
    return "1".equals(disable) || "true".equalsIgnoreCase(disable);
  }

  private static Status parseFailed(Response response, Throwable th) {
    String contentType = response.header("Content-Type");
    // process http failed.
    if (contentType != null && (contentType.toLowerCase().contains("application/json"))) {
      String body;
      try {
        body = response.body().string();
      } catch (IOException e) {
        return Status.builder()
            .statusCode(response.code())
            .code("response_error")
            .message("Failed read response body: " + e.getMessage())
            .isJson(true)
            .build();
      }
      return parseFailedJson(response.code(), body);
    }
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
        .code("response_error")
        .message(response.message())
        .isJson(false)
        .build();
  }

  private static Status parseFailedJson(int statusCode, String body) {
    JsonObject jsonResponse;
    try {
      jsonResponse = JsonUtils.parse(body);
    } catch (Throwable e) {
      return Status.builder()
          .statusCode(statusCode)
          .code("response_error")
          .message(body)
          .isJson(true)
          .build();
    }
    return statusFromErrorJson(statusCode, jsonResponse);
  }

  private static Status tryParseErrorJson(int statusCode, String body) {
    JsonObject jsonResponse;
    try {
      jsonResponse = JsonUtils.parse(body);
    } catch (Throwable e) {
      return null;
    }
    if (jsonResponse == null
        || (!jsonResponse.has("code")
            && !jsonResponse.has("message")
            && !jsonResponse.has("error"))) {
      return null;
    }
    return statusFromErrorJson(statusCode, jsonResponse);
  }

  private static Status statusFromErrorJson(int statusCode, JsonObject jsonResponse) {
    String code = "";
    String message = "";
    String requestId = "";
    if (jsonResponse.has("request_id")) {
      requestId = jsonResponse.get("request_id").getAsString();
    } else if (jsonResponse.has("requestId")) {
      requestId = jsonResponse.get("requestId").getAsString();
    }
    if (jsonResponse.has("code")) {
      code = jsonResponse.get("code").getAsString();
    }
    if (jsonResponse.has("message")) {
      message = jsonResponse.get("message").getAsString();
    }
    if ((code == null || code.isEmpty())
        && (message == null || message.isEmpty())
        && jsonResponse.has("error")
        && jsonResponse.get("error").isJsonObject()) {
      JsonObject error = jsonResponse.getAsJsonObject("error");
      if (error.has("code")) {
        code = error.get("code").getAsString();
      }
      if (error.has("message")) {
        message = error.get("message").getAsString();
      }
    }
    return Status.builder()
        .statusCode(statusCode)
        .code(code)
        .message(message)
        .requestId(requestId)
        .isJson(true)
        .build();
  }

  private static OkHttpClient defaultClient() {
    int poolSize =
        Integer.parseInt(System.getenv().getOrDefault(EnvNames.MAX_CONNECTIONS_HTTP, "100"));
    int perRoute =
        Integer.parseInt(
            System.getenv().getOrDefault(EnvNames.MAX_CONNECTIONS_PER_ROUTE_HTTP, "20"));
    Dispatcher dispatcher = new Dispatcher();
    dispatcher.setMaxRequests(poolSize);
    dispatcher.setMaxRequestsPerHost(perRoute);
    return new OkHttpClient.Builder()
        .connectTimeout(java.time.Duration.ofSeconds(120))
        .writeTimeout(java.time.Duration.ofSeconds(60))
        .readTimeout(java.time.Duration.ofSeconds(300))
        .dispatcher(dispatcher)
        .protocols(java.util.Collections.singletonList(okhttp3.Protocol.HTTP_1_1))
        .connectionPool(new ConnectionPool(poolSize, 300, TimeUnit.SECONDS))
        .build();
  }

  private static OkHttpClient newClient(ConnectionOptions connectionOptions) {
    OkHttpClient.Builder builder = defaultClient().newBuilder();
    if (connectionOptions.hasCustomConnectTimeout()) {
      builder.connectTimeout(connectionOptions.getConnectTimeout());
    }
    if (connectionOptions.hasCustomReadTimeout()) {
      builder.readTimeout(connectionOptions.getReadTimeout());
    }
    if (connectionOptions.hasCustomWriteTimeout()) {
      builder.writeTimeout(connectionOptions.getWriteTimeout());
    }
    if (connectionOptions.getProxy() != null) {
      builder.proxy(connectionOptions.getProxy());
    }
    if (connectionOptions.getProxyAuthenticator() != null) {
      builder.proxyAuthenticator(connectionOptions.getProxyAuthenticator());
    }
    return builder.build();
  }
}
