// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.config.ResolvedConfig;
import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.DashScopeException;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.PublicErrorCode;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.transport.sse.JobEventStream;
import com.alibaba.dashscope.transport.sse.SseStream;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 3.0 HTTP transport (design §6): one OkHttpClient (pool maxRequests=100 / perHost=20, keepAlive
 * 5s, env-tunable), application-level retry/auth/UA interceptors, and the dual error layer —
 * non-2xx status and HTTP-200-with-business-code both map through {@link Errors}; with {@code
 * raiseOnError(false)} they instead surface on the returned envelope (2.x-style {@code isError()}
 * semantics). Network failures always raise.
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
      // Derive from the injected client rather than using it as-is: the derivation shares the
      // caller's connection pool and dispatcher (lifecycle stays with the caller) while
      // guaranteeing auth/UA/retry interceptors run — without them every request would go out
      // without the Authorization header and fail with 401.
      this.client =
          config
              .getHttpClient()
              .newBuilder()
              .readTimeout(config.getTimeout())
              .callTimeout(config.getTimeout())
              .addInterceptor(new AuthInterceptor())
              .addInterceptor(new UserAgentInterceptor())
              .addInterceptor(new RetryInterceptor())
              .build();
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
                      config.getMaxIdleConnections(),
                      config.getKeepAlive().toMillis(),
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
    this.client =
        shared
            .newBuilder()
            .readTimeout(config.getTimeout())
            .callTimeout(config.getTimeout())
            .build();
    this.ownsClient = false;
  }

  /**
   * Derive a transport reusing this one's connection pool (design §2 withOptions) — unless the
   * override injects a different {@link okhttp3.OkHttpClient}, which is honored by deriving from it
   * (sharing its pool, with interceptors ensured by the constructor).
   */
  public HttpTransport derive(ResolvedConfig newConfig) {
    OkHttpClient injected = newConfig.getHttpClient();
    if (injected != null && injected != client) {
      return new HttpTransport(newConfig);
    }
    // Always share: a derived transport never owns the client, and the owner's close() governs
    // the pool — derived transports must not outlive their owner either way.
    return new HttpTransport(newConfig, client);
  }

  public ResolvedConfig config() {
    return config;
  }

  /** The shared OkHttpClient (same pool + auth/UA/retry interceptors); used by the ws transport. */
  public OkHttpClient okhttp() {
    return client;
  }

  /** POST {@code path} (relative to the resolved base URL) with a JSON body. */
  public ResponseEnvelope execute(
      String path, JsonNode body, String module, RequestOptions options) {
    Request request = buildRequest(path, body, module, options, false, "POST");
    Response response = send(request, options, false);
    return handleResponse(response);
  }

  /** PUT {@code path} with a JSON body (deployments scale). */
  public ResponseEnvelope executePut(
      String path, JsonNode body, String module, RequestOptions options) {
    Request request = buildRequest(path, body, module, options, false, "PUT");
    Response response = send(request, options, false);
    return handleResponse(response);
  }

  /** POST with extra request headers (e.g. {@code X-DashScope-Async: enable} for job submit). */
  public ResponseEnvelope execute(
      String path,
      JsonNode body,
      Map<String, String> extraHeaders,
      String module,
      RequestOptions options) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    RequestOptions.Builder merged =
        RequestOptions.builder()
            .timeout(opts.getTimeout())
            .maxRetries(opts.getMaxRetries())
            .apiKey(opts.getApiKey())
            .workspace(opts.getWorkspace())
            .enableEncryption(opts.getEnableEncryption())
            .headers(opts.getHeaders())
            .headers(extraHeaders);
    RequestOptions mergedOptions = merged.build();
    Request request = buildRequest(path, body, module, mergedOptions, false, "POST");
    Response response = send(request, mergedOptions, false);
    return handleResponse(response);
  }

  /** GET {@code path} with query parameters (jobs fetch/cancel/list). */
  public ResponseEnvelope executeGet(
      String path, Map<String, String> query, String module, RequestOptions options) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    String apiKey = opts.getApiKey() != null ? opts.getApiKey() : config.getApiKey();
    if (apiKey == null || apiKey.isEmpty()) {
      throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY);
    }
    String workspace = opts.getWorkspace() != null ? opts.getWorkspace() : config.getWorkspace();
    HttpUrl.Builder urlBuilder = HttpUrl.get(config.getBaseUrl() + path).newBuilder();
    if (query != null) {
      for (Map.Entry<String, String> e : query.entrySet()) {
        urlBuilder.addQueryParameter(e.getKey(), e.getValue());
      }
    }
    Request.Builder builder =
        new Request.Builder()
            .url(urlBuilder.build())
            .get()
            .tag(RequestTags.Auth.class, new RequestTags.Auth(apiKey, workspace))
            .tag(
                RequestTags.MaxRetries.class,
                new RequestTags.MaxRetries(
                    opts.getMaxRetries() != null ? opts.getMaxRetries() : config.getMaxRetries()))
            .header("Accept", "application/json; charset=utf-8");
    if (module != null && !module.isEmpty()) {
      builder.tag(RequestTags.Module.class, new RequestTags.Module(module));
    }
    for (Map.Entry<String, String> e : opts.getHeaders().entrySet()) {
      builder.header(e.getKey(), e.getValue());
    }
    Response response = send(builder.build(), opts, false);
    return handleResponse(response);
  }

  /** DELETE {@code path} (files/fine-tunes/deployments CRUD). */
  public ResponseEnvelope executeDelete(String path, String module, RequestOptions options) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    String apiKey = opts.getApiKey() != null ? opts.getApiKey() : config.getApiKey();
    if (apiKey == null || apiKey.isEmpty()) {
      throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY);
    }
    String workspace = opts.getWorkspace() != null ? opts.getWorkspace() : config.getWorkspace();
    Request.Builder builder =
        new Request.Builder()
            .url(config.getBaseUrl() + path)
            .delete()
            .tag(RequestTags.Auth.class, new RequestTags.Auth(apiKey, workspace))
            .tag(
                RequestTags.MaxRetries.class,
                new RequestTags.MaxRetries(
                    opts.getMaxRetries() != null ? opts.getMaxRetries() : config.getMaxRetries()))
            .header("Accept", "application/json; charset=utf-8");
    if (module != null && !module.isEmpty()) {
      builder.tag(RequestTags.Module.class, new RequestTags.Module(module));
    }
    for (Map.Entry<String, String> e : opts.getHeaders().entrySet()) {
      builder.header(e.getKey(), e.getValue());
    }
    Response response = send(builder.build(), opts, false);
    return handleResponse(response);
  }

  /**
   * Multipart POST (files upload): form fields first, the file part last; rides the same
   * auth/retry/UA pipeline as JSON posts. Bytes, not a live handle: transport retries must be able
   * to re-send the full body.
   */
  public ResponseEnvelope executeForm(
      String path,
      Map<String, String> fields,
      String fileField,
      String fileName,
      byte[] content,
      String contentType,
      String module,
      RequestOptions options) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    String apiKey = opts.getApiKey() != null ? opts.getApiKey() : config.getApiKey();
    if (apiKey == null || apiKey.isEmpty()) {
      throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY);
    }
    String workspace = opts.getWorkspace() != null ? opts.getWorkspace() : config.getWorkspace();
    MultipartBody.Builder body = new MultipartBody.Builder().setType(MultipartBody.FORM);
    if (fields != null) {
      fields.forEach(body::addFormDataPart);
    }
    body.addFormDataPart(
        fileField, fileName, RequestBody.create(content, MediaType.get(contentType)));
    Request.Builder builder =
        new Request.Builder()
            .url(config.getBaseUrl() + path)
            .post(body.build())
            .tag(RequestTags.Auth.class, new RequestTags.Auth(apiKey, workspace))
            .tag(
                RequestTags.MaxRetries.class,
                new RequestTags.MaxRetries(
                    opts.getMaxRetries() != null ? opts.getMaxRetries() : config.getMaxRetries()))
            .header("Accept", "application/json; charset=utf-8");
    if (module != null && !module.isEmpty()) {
      builder.tag(RequestTags.Module.class, new RequestTags.Module(module));
    }
    for (Map.Entry<String, String> e : opts.getHeaders().entrySet()) {
      builder.header(e.getKey(), e.getValue());
    }
    Response response = send(builder.build(), opts, false);
    return handleResponse(response);
  }

  /**
   * POST with SSE: returns the open stream. The initial HTTP layer always raises on failure (there
   * is no response object to hand back); in-stream error frames follow raiseOnError.
   */
  public SseStream executeStream(
      String path, JsonNode body, String module, RequestOptions options) {
    return executeStream(path, body, module, null, options);
  }

  /** SSE variant carrying a User-Agent suffix (multimodal incremental_to_full parity). */
  public SseStream executeStream(
      String path, JsonNode body, String module, String userAgentSuffix, RequestOptions options) {
    Request request = buildRequest(path, body, module, options, true, "POST");
    if (userAgentSuffix != null && !userAgentSuffix.isEmpty()) {
      request =
          request
              .newBuilder()
              .tag(
                  RequestTags.UserAgentSuffix.class,
                  new RequestTags.UserAgentSuffix(userAgentSuffix))
              .build();
    }
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
    RequestTags.Encryption encryption = request.tag(RequestTags.Encryption.class);
    return new SseStream(response, encryption == null ? null : encryption.config);
  }

  /**
   * GET a job-event SSE endpoint (fine-tunes/deployments): the {@code event:error}/{@code status:}
   * framing, not the plain {@code data:}-only shape of {@link #executeStream}. The initial HTTP
   * layer always raises on failure.
   */
  public JobEventStream executeJobEventStream(String path, String module, RequestOptions options) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    String apiKey = opts.getApiKey() != null ? opts.getApiKey() : config.getApiKey();
    if (apiKey == null || apiKey.isEmpty()) {
      throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY);
    }
    String workspace = opts.getWorkspace() != null ? opts.getWorkspace() : config.getWorkspace();
    Request.Builder builder =
        new Request.Builder()
            .url(config.getBaseUrl() + path)
            .get()
            .tag(RequestTags.Auth.class, new RequestTags.Auth(apiKey, workspace))
            .tag(
                RequestTags.MaxRetries.class,
                new RequestTags.MaxRetries(
                    opts.getMaxRetries() != null ? opts.getMaxRetries() : config.getMaxRetries()))
            .tag(RequestTags.NoRetry.class, RequestTags.NoRetry.INSTANCE)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .header("X-Accel-Buffering", "no");
    if (module != null && !module.isEmpty()) {
      builder.tag(RequestTags.Module.class, new RequestTags.Module(module));
    }
    for (Map.Entry<String, String> e : opts.getHeaders().entrySet()) {
      builder.header(e.getKey(), e.getValue());
    }
    Response response = send(builder.build(), opts, true);
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
    return new JobEventStream(response);
  }

  private Request buildRequest(
      String path,
      JsonNode body,
      String module,
      RequestOptions options,
      boolean stream,
      String httpMethod) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    String apiKey = opts.getApiKey() != null ? opts.getApiKey() : config.getApiKey();
    if (apiKey == null || apiKey.isEmpty()) {
      throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY);
    }
    String workspace = opts.getWorkspace() != null ? opts.getWorkspace() : config.getWorkspace();
    EncryptionConfig encryptionConfig;
    JsonNode wireBody;
    try {
      encryptionConfig = encryptionConfig(body, opts);
      wireBody =
          encryptionConfig == null ? body : EncryptionUtils.encryptInput(body, encryptionConfig);
    } catch (DashScopeException e) {
      throw e;
    } catch (RuntimeException e) {
      throw Errors.fromCatalogEntry(
          PublicErrorCode.RESPONSE_ERROR,
          "failed to prepare encrypted request: " + e.getMessage(),
          null,
          JsonSupport.toPlainValue(body),
          null,
          e);
    }

    Request.Builder builder =
        new Request.Builder()
            // Absolute URLs bypass the resolved base URL (decision's compatible-mode root).
            .url(isAbsolute(path) ? path : config.getBaseUrl() + path)
            .tag(RequestTags.Auth.class, new RequestTags.Auth(apiKey, workspace))
            .tag(
                RequestTags.MaxRetries.class,
                new RequestTags.MaxRetries(
                    opts.getMaxRetries() != null ? opts.getMaxRetries() : config.getMaxRetries()))
            .header("Content-Type", "application/json");
    if (encryptionConfig != null) {
      builder
          .tag(RequestTags.Encryption.class, new RequestTags.Encryption(encryptionConfig))
          .header("X-DashScope-EncryptionKey", encryptionConfig.keyHeader());
    }
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
    if (wireBody != null) {
      builder.method(httpMethod, RequestBody.create(wireBody.toString(), JSON));
    } else {
      builder.method(httpMethod, RequestBody.create(new byte[0], null));
    }
    for (Map.Entry<String, String> e : opts.getHeaders().entrySet()) {
      builder.header(e.getKey(), e.getValue());
    }
    return builder.build();
  }

  private static boolean isAbsolute(String path) {
    return path.startsWith("http://") || path.startsWith("https://");
  }

  private EncryptionConfig encryptionConfig(JsonNode body, RequestOptions options) {
    if (!Boolean.TRUE.equals(options.getEnableEncryption())
        || body == null
        || !body.isObject()
        || !body.has("input")
        || !body.get("input").isObject()) {
      return null;
    }
    RequestOptions keyOptions = options.toBuilder().enableEncryption(false).build();
    return new EncryptionKeys(this).createConfig(keyOptions);
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
        return client.newBuilder().callTimeout(Duration.ZERO).readTimeout(override).build();
      }
      return client.newBuilder().callTimeout(Duration.ZERO).build();
    }
    if (override != null) {
      return client.newBuilder().callTimeout(override).readTimeout(override).build();
    }
    return client;
  }

  private ResponseEnvelope handleResponse(Response response) {
    String text = readBodyText(response);
    Map<String, String> headers = headersOf(response);
    boolean raiseOnError = config.isRaiseOnError();

    if (!response.isSuccessful()) {
      JsonNode errorBody = decryptBody(JsonSupport.parseErrorBody(text), response, headers);
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
      // raiseOnError(false) callers judge failure via isError(), which keys off the envelope
      // "code". Unparseable (gateway HTML error pages) or code-less bodies must still surface
      // as errors, so backfill a synthetic frame when the wire body carried no code.
      JsonNode body = errorBody;
      if (JsonSupport.errorCode(body) == null) {
        body =
            JsonSupport.errorFrame(
                "HTTP_" + response.code(), error.getMessage(), error.getRequestId());
      }
      return new ResponseEnvelope(response.code(), headers, body);
    }

    JsonNode body = text.isEmpty() ? null : JsonSupport.readTree(text);
    body = decryptBody(body, response, headers);
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

  private static JsonNode decryptBody(
      JsonNode body, Response response, Map<String, String> headers) {
    RequestTags.Encryption encryption = response.request().tag(RequestTags.Encryption.class);
    if (encryption == null || body == null) {
      return body;
    }
    try {
      return EncryptionUtils.decryptOutput(body, encryption.config);
    } catch (RuntimeException e) {
      throw Errors.fromCatalogEntry(
          PublicErrorCode.RESPONSE_ERROR,
          "failed to decrypt response output: " + e.getMessage(),
          JsonSupport.requestId(body, headers),
          JsonSupport.toPlainValue(body),
          headers,
          e);
    }
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
      return Errors.timeout(SdkErrorCode.SDK_TIMEOUT, "request: " + e.getMessage(), e);
    }
    return Errors.connection(SdkErrorCode.SDK_CONNECTION_ERROR, "request: " + e.getMessage(), e);
  }

  @Override
  public void close() {
    if (ownsClient) {
      client.dispatcher().executorService().shutdown();
      client.connectionPool().evictAll();
    }
  }
}
