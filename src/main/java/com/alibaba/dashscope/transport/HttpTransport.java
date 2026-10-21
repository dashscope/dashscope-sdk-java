// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.common.SdkErrorCode;
import com.alibaba.dashscope.config.ResolvedConfig;
import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.transport.sse.SseStream;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 3.0 HTTP transport (design §6): one OkHttpClient (pool maxRequests=100 / perHost=20, keepAlive
 * 5s, env-tunable), application-level retry/auth/UA interceptors, and the dual error layer —
 * non-2xx status and HTTP-200-with-business-code both map through {@link Errors}; with
 * {@code raiseOnError(false)} they instead surface on the returned envelope (2.x-style
 * {@code isError()} semantics). Network failures always raise.
 */
public final class HttpTransport implements AutoCloseable {
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration WRITE_TIMEOUT = Duration.ofSeconds(30);

  private final ResolvedConfig config;
  private final OkHttpClient client;
  private final boolean ownsClient;

  public HttpTransport(ResolvedConfig config) {
    this.config = config;
    if (config.getHttpClient() != null) {
      this.client = config.getHttpClient();
      this.ownsClient = false;
    } else {
      Dispatcher dispatcher = new Dispatcher();
      dispatcher.setMaxRequests(config.getMaxRequests());
      dispatcher.setMaxRequestsPerHost(config.getMaxRequestsPerHost());
      this.client =
          new OkHttpClient.Builder()
              .dispatcher(dispatcher)
              .connectionPool(
                  new ConnectionPool(
                      config.getMaxRequestsPerHost(), config.getKeepAlive().toMillis(),
                      java.util.concurrent.TimeUnit.MILLISECONDS))
              .connectTimeout(CONNECT_TIMEOUT)
              .writeTimeout(WRITE_TIMEOUT)
              .readTimeout(config.getTimeout())
              .callTimeout(config.getTimeout())
              .retryOnConnectionFailure(true)
              .addInterceptor(new AuthInterceptor())
              .addInterceptor(new UserAgentInterceptor())
              .addInterceptor(new RetryInterceptor())
              .build();
      this.ownsClient = true;
    }
  }

  /** Sharing constructor for withOptions()-derived clients: same underlying pool. */
  private HttpTransport(ResolvedConfig config, OkHttpClient shared) {
    this.config = config;
    this.client = shared;
    this.ownsClient = false;
  }

  /** Derive a transport reusing this one's connection pool (design §2 withOptions). */
  public HttpTransport derive(ResolvedConfig newConfig) {
    if (config.getHttpClient() != null || ownsClient) {
      return new HttpTransport(newConfig, client);
    }
    return new HttpTransport(newConfig);
  }

  public ResolvedConfig config() {
    return config;
  }

  /** POST {@code path} (relative to the resolved base URL) with a JSON body. */
  public ResponseEnvelope execute(
      String path, JsonNode body, String module, RequestOptions options) {
    Request request = buildRequest(path, body, module, options, false);
    Response response = send(request, options, false);
    return handleResponse(response, options);
  }

  /** POST with SSE: returns the open stream. The initial HTTP layer always raises on failure
   * (there is no response object to hand back); in-stream error frames follow raiseOnError. */
  public SseStream executeStream(
      String path, JsonNode body, String module, RequestOptions options) {
    Request request = buildRequest(path, body, module, options, true);
    Response response = send(request, options, true);
    if (!response.isSuccessful()) {
      String text = readBodyText(response);
      response.close();
      JsonNode errorBody = JsonSupport.parseErrorBody(text);
      throw Errors.fromStatus(
          response.code(),
          JsonSupport.errorCode(errorBody),
          messageOr(errorBody, text),
          JsonSupport.requestId(errorBody, headersOf(response)),
          JsonSupport.toPlainValue(errorBody),
          headersOf(response));
    }
    return new SseStream(response);
  }

  private Request buildRequest(
      String path, JsonNode body, String module, RequestOptions options, boolean stream) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    String apiKey = opts.getApiKey() != null ? opts.getApiKey() : config.getApiKey();
    if (apiKey == null || apiKey.isEmpty()) {
      throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY);
    }
    String workspace = opts.getWorkspace() != null ? opts.getWorkspace() : config.getWorkspace();

    Request.Builder builder =
        new Request.Builder()
            .url(config.getBaseUrl() + path)
            .tag(RequestTags.Auth.class, new RequestTags.Auth(apiKey, workspace))
            .tag(RequestTags.MaxRetries.class, new RequestTags.MaxRetries(
                opts.getMaxRetries() != null ? opts.getMaxRetries() : config.getMaxRetries()))
            .header("Content-Type", "application/json");
    if (module != null && !module.isEmpty()) {
      builder.tag(RequestTags.Module.class, new RequestTags.Module(module));
    }
    if (stream) {
      builder
          .tag(RequestTags.NoRetry.class, RequestTags.NoRetry.INSTANCE)
          .header("Accept", "text/event-stream")
          .header("Cache-Control", "no-cache")
          .header("X-Accel-Buffering", "no")
          .header("X-DashScope-SSE", "enable");
    } else {
      builder.header("Accept", "application/json; charset=utf-8");
    }
    if (body != null) {
      builder.post(RequestBody.create(body.toString(), JSON));
    } else {
      builder.post(RequestBody.create(new byte[0], null));
    }
    for (Map.Entry<String, String> e : opts.getHeaders().entrySet()) {
      builder.header(e.getKey(), e.getValue());
    }
    return builder.build();
  }

  private Response send(Request request, RequestOptions options, boolean stream) {
    OkHttpClient callClient = clientFor(options == null ? RequestOptions.NONE : options, stream);
    try {
      return callClient.newCall(request).execute();
    } catch (IOException e) {
      throw toException(e);
    }
  }

  /** Per-call timeout override derives a client that shares the pool (OkHttp newBuilder). */
  private OkHttpClient clientFor(RequestOptions options, boolean stream) {
    Duration override = options.getTimeout();
    if (stream) {
      // Streams must not carry a whole-call timeout; readTimeout bounds the gap between frames.
      if (override != null) {
        return client.newBuilder()
            .callTimeout(Duration.ZERO)
            .readTimeout(override)
            .build();
      }
      return client.newBuilder().callTimeout(Duration.ZERO).build();
    }
    if (override != null) {
      return client.newBuilder().callTimeout(override).readTimeout(override).build();
    }
    return client;
  }

  private ResponseEnvelope handleResponse(Response response, RequestOptions options) {
    String text = readBodyText(response);
    Map<String, String> headers = headersOf(response);
    boolean raiseOnError = config.isRaiseOnError();

    if (!response.isSuccessful()) {
      JsonNode errorBody = JsonSupport.parseErrorBody(text);
      ApiStatusException error =
          Errors.fromStatus(
              response.code(),
              JsonSupport.errorCode(errorBody),
              messageOr(errorBody, text),
              JsonSupport.requestId(errorBody, headers),
              JsonSupport.toPlainValue(errorBody),
              headers);
      response.close();
      if (raiseOnError) {
        throw error;
      }
      return new ResponseEnvelope(response.code(), headers, errorBody);
    }

    JsonNode body = text.isEmpty() ? null : JsonSupport.readTree(text);
    if (JsonSupport.isErrorFrame(body)) {
      ApiStatusException error =
          Errors.fromBusinessCode(
              JsonSupport.errorCode(body),
              JsonSupport.errorMessage(body),
              JsonSupport.requestId(body, headers),
              JsonSupport.toPlainValue(body),
              headers);
      if (raiseOnError) {
        throw error;
      }
    }
    return new ResponseEnvelope(response.code(), headers, body);
  }

  private static String readBodyText(Response response) {
    ResponseBody body = response.body();
    if (body == null) {
      return "";
    }
    try {
      return body.string();
    } catch (IOException e) {
      throw toException(e);
    }
  }

  private static Map<String, String> headersOf(Response response) {
    Map<String, String> out = new LinkedHashMap<>();
    for (String name : response.headers().names()) {
      out.put(name, response.header(name));
    }
    return out;
  }

  private static String messageOr(JsonNode body, String fallback) {
    String message = JsonSupport.errorMessage(body);
    return message != null ? message : fallback;
  }

  private static RuntimeException toException(IOException e) {
    if (e instanceof InterruptedIOException) {
      return new ApiTimeoutException("request timed out: " + e.getMessage(), e);
    }
    return new ApiConnectionException("connection error: " + e.getMessage(), e);
  }

  @Override
  public void close() {
    if (ownsClient) {
      client.dispatcher().executorService().shutdown();
      client.connectionPool().evictAll();
    }
  }
}
