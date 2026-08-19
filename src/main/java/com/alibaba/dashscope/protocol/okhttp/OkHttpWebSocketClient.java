// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol.okhttp;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.JsonObject;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.Request.Builder;
import okio.ByteString;

@Slf4j
public class OkHttpWebSocketClient extends WebSocketListener
    implements HalfDuplexClient, FullDuplexClient {
  // we will try 3 times for connection, only for retryable failures.
  private static final int MAX_CONNECTION_TIMES =
      intSetting(
          "dashscope.websocket.max.connect.attempts",
          "DASHSCOPE_WEBSOCKET_MAX_CONNECT_ATTEMPTS",
          3);
  // base delay of the exponential backoff applied between two retryable handshake attempts.
  private static final long RETRY_BACKOFF_MILLIS =
      intSetting(
          "dashscope.websocket.retry.backoff.millis",
          "DASHSCOPE_WEBSOCKET_RETRY_BACKOFF_MILLIS",
          1000);
  // upper bound of the backoff delay, whatever the attempt number is.
  private static final long MAX_RETRY_BACKOFF_MILLIS = 10000L;
  // how long one handshake attempt may take before it is abandoned.
  private static final int CONNECT_WAIT_SECONDS =
      intSetting(
          "dashscope.websocket.connect.wait.seconds",
          "DASHSCOPE_WEBSOCKET_CONNECT_WAIT_SECONDS",
          60);
  // okhttp reports the status line in the handshake failure message when the upgrade is refused,
  // e.g. "Expected HTTP 101 response but was '403 Forbidden'".
  private static final Pattern CLIENT_ERROR_STATUS_LINE = Pattern.compile("was '4\\d\\d ");
  private OkHttpClient client;
  private WebSocket webSocketClient;
  // indicate the websocket connection is established.
  private AtomicBoolean isOpen = new AtomicBoolean(false);
  protected AtomicBoolean isClosed = new AtomicBoolean(false);
  // indicate the first response is received.
  protected AtomicBoolean isFirstMessage = new AtomicBoolean(false);
  // used for get request response
  protected volatile FlowableEmitter<DashScopeResult> responseEmitter;
  // is the result is flatten format.
  private boolean isFlattenResult;
  private volatile FlowableEmitter<DashScopeResult> connectionEmitter;

  private AtomicBoolean passTaskStarted = new AtomicBoolean(false);

  // Disposable for the streaming data subscription, used to cancel upstream when stopping
  protected volatile Disposable streamingDataDisposable;

  // Terminal error parked until a subscriber is attached to the response stream.
  private final AtomicReference<Throwable> pendingResponseError = new AtomicReference<>(null);

  public OkHttpWebSocketClient(OkHttpClient client, boolean passTaskStarted) {
    this.client = client;
    this.passTaskStarted.set(passTaskStarted);
  }

  private Request buildConnectionRequest(
      String apiKey,
      boolean isSecurityCheck,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl)
      throws NoApiKeyException {
    // Extract and filter custom user agent from param headers
    String customUserAgent = customHeaders != null ? customHeaders.get("user-agent") : null;
    Map<String, String> filteredHeaders =
        customHeaders != null ? new java.util.HashMap<>(customHeaders) : new java.util.HashMap<>();
    filteredHeaders.remove("user-agent");

    // build the request builder.
    Builder bd = new Request.Builder();
    bd.headers(
        Headers.of(
            DashScopeHeaders.buildWebSocketHeaders(
                apiKey, isSecurityCheck, workspace, filteredHeaders, customUserAgent)));
    String url = Constants.baseWebsocketApiUrl;
    if (baseWebSocketUrl != null) {
      url = baseWebSocketUrl;
    }
    // Validate URL before building request to provide clear error message
    if (url == null || url.isEmpty()) {
      throw new ApiException(
          Status.builder()
              .statusCode(PublicErrorCode.INVALID_URL.getStatusCode())
              .code(PublicErrorCode.INVALID_URL.getErrorCode())
              .message(
                  StringUtils.format(
                      "%s [detail=URL is null or empty]",
                      PublicErrorCode.INVALID_URL.getErrorMsg()))
              .build());
    }
    // HttpUrl.parse() only supports http/https schemes.
    // WebSocket URLs use ws/wss schemes, so we convert them for validation.
    String urlForValidation = url;
    if (urlForValidation.startsWith("ws://")) {
      urlForValidation = "http://" + urlForValidation.substring("ws://".length());
    } else if (urlForValidation.startsWith("wss://")) {
      urlForValidation = "https://" + urlForValidation.substring("wss://".length());
    }
    HttpUrl parsedUrl = HttpUrl.parse(urlForValidation);
    if (parsedUrl == null) {
      throw new ApiException(
          Status.builder()
              .statusCode(PublicErrorCode.INVALID_URL.getStatusCode())
              .code(PublicErrorCode.INVALID_URL.getErrorCode())
              .message(
                  StringUtils.format(
                      "%s [detail=%s]", PublicErrorCode.INVALID_URL.getErrorMsg(), url))
              .build());
    }
    // Use bd.url(String) which handles ws:// and wss:// schemes internally
    Request request = bd.url(url).build();
    return request;
  }

  public boolean close(int code, String reason) {
    /**
     * close websocket connection see.
     * https://square.github.io/okhttp/3.x/okhttp/okhttp3/WebSocket.html
     */
    isClosed.set(true);
    releaseConnectionWaiter();
    if (webSocketClient != null) {
      return webSocketClient.close(code, reason);
    } else {
      return true;
    }
  }

  public void cancel() {
    // Set isClosed BEFORE cancel to suppress onFailure error propagation
    isClosed.set(true);
    releaseConnectionWaiter();
    // Dispose upstream subscription to stop sending data
    Disposable d = streamingDataDisposable;
    if (d != null && !d.isDisposed()) {
      d.dispose();
    }
    if (webSocketClient != null) {
      webSocketClient.cancel();
    }
  }

  /**
   * Completes the connection emitter so that the thread blocked in establishWebSocketClient()
   * returns immediately instead of waiting for the connect timeout. Safe to call at any time:
   * completing an already-terminated emitter is a no-op, and the isClosed checks downstream prevent
   * any message from being sent on the aborted connection.
   */
  private void releaseConnectionWaiter() {
    FlowableEmitter<DashScopeResult> emitter = this.connectionEmitter;
    if (emitter != null && !emitter.isCancelled()) {
      emitter.onComplete();
    }
  }

  private void establishWebSocketClient(
      String apiKey,
      boolean isSecurityCheck,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl) {
    int attempts = 0;
    String errorMessage = "";
    int httpStatusCode = 0;
    while (true) {
      // Bail out immediately if cancel() has been called
      if (isClosed.get()) {
        log.debug("Connection cancelled, stop reconnecting.");
        return;
      }
      try {
        Flowable<DashScopeResult> flowable =
            Flowable.<DashScopeResult>create(
                emitter -> {
                  this.connectionEmitter = emitter;
                  try {
                    if (client == null) {
                      client = OkHttpClientFactory.getOkHttpClient();
                    }
                    webSocketClient =
                        client.newWebSocket(
                            buildConnectionRequest(
                                apiKey,
                                isSecurityCheck,
                                workspace,
                                customHeaders,
                                baseWebSocketUrl),
                            this);
                  } catch (Throwable ex) {
                    this.connectionEmitter.onError(ex);
                  }
                },
                BackpressureStrategy.BUFFER);
        // wait for connection establish
        flowable.timeout(CONNECT_WAIT_SECONDS, TimeUnit.SECONDS).blockingSubscribe();
        return;
      } catch (Throwable ex) {
        // Unwrap RxJava-wrapped exceptions to find the original ApiException.
        Throwable unwrapped = ex;
        while (unwrapped != null && !(unwrapped instanceof ApiException)) {
          unwrapped = unwrapped.getCause();
        }

        // Client-side errors (e.g. invalid URL, invalid API key) should not be retried or wrapped.
        // Rethrow immediately so the caller sees the original error code.
        if (unwrapped instanceof ApiException) {
          ApiException apiEx = (ApiException) unwrapped;
          // Only rethrow 4xx errors directly; 5xx errors should still be retried.
          if (apiEx.getStatus() != null
              && apiEx.getStatus().getStatusCode() >= 400
              && apiEx.getStatus().getStatusCode() < 500) {
            throw apiEx;
          }
        }
        attempts += 1;
        errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.toString();
        log.error(errorMessage);
        if (errorMessage.contains(Constants.NO_API_KEY_ERROR)) {
          throw ex;
        }
        httpStatusCode = extractHttpStatusCode(ex);
        // The server answered the handshake with a client error (401, 403, 429, ...): retrying
        // cannot change the outcome and would only add pressure on a server already refusing us,
        // so give up immediately.
        if (isClientError(httpStatusCode, errorMessage)) {
          log.warn(
              "Websocket handshake refused with http status {}, will not retry: {}",
              httpStatusCode,
              errorMessage);
          break;
        }
        if (attempts >= MAX_CONNECTION_TIMES) {
          // No point in waiting after the last attempt.
          break;
        }
        // Check again before sleeping
        if (isClosed.get()) {
          log.debug("Connection cancelled during retry, stop reconnecting.");
          return;
        }
        if (!sleepBeforeRetry(attempts)) {
          return;
        }
      }
    }
    // The handshake status code only drives the retry decision above: callers rely on the fixed
    // websocket failure status code, so it must not leak into the reported status.
    throw new ApiException(
        Status.builder()
            .statusCode(PublicErrorCode.SERVICE_UNAVAILABLE.getStatusCode())
            .code(PublicErrorCode.SERVICE_UNAVAILABLE.getErrorCode())
            .message(
                StringUtils.format(
                    "%s [originalError=%s]",
                    PublicErrorCode.SERVICE_UNAVAILABLE.getErrorMsg(), errorMessage))
            .build());
  }

  /**
   * Walks the cause chain looking for the HTTP status code carried by a refused handshake. RxJava
   * wraps the exception raised inside the connection emitter before it reaches the caller of
   * blockingSubscribe(), hence the walk instead of a plain instanceof check.
   *
   * @return the HTTP status code, or 0 when no handshake response was received.
   */
  private static int extractHttpStatusCode(Throwable ex) {
    Throwable cause = ex;
    int depth = 0;
    while (cause != null && depth < 8) {
      if (cause instanceof WebSocketConnectException) {
        return ((WebSocketConnectException) cause).httpStatusCode;
      }
      cause = cause.getCause();
      depth += 1;
    }
    return 0;
  }

  /** A 4xx handshake response is a final decision from the server, so it must not be retried. */
  private static boolean isClientError(int httpStatusCode, String errorMessage) {
    if (httpStatusCode >= 400 && httpStatusCode < 500) {
      return true;
    }
    // Fallback for the cases where the response object is not available.
    return errorMessage != null && CLIENT_ERROR_STATUS_LINE.matcher(errorMessage).find();
  }

  /**
   * Waits before the next handshake attempt, using an exponential backoff with jitter so that a
   * burst of sessions failing at the same time does not retry in lockstep.
   *
   * @return false when the wait was interrupted and the caller should give up.
   */
  private static boolean sleepBeforeRetry(int attempts) {
    long delay = RETRY_BACKOFF_MILLIS << Math.min(attempts - 1, 16);
    delay = Math.min(delay, MAX_RETRY_BACKOFF_MILLIS);
    long jitter = delay / 4;
    long sleepMillis = delay - jitter + ThreadLocalRandom.current().nextLong(2 * jitter + 1);
    try {
      Thread.sleep(sleepMillis);
      return true;
    } catch (InterruptedException e) {
      // Respect interruption - exit the loop
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * Reads an int setting from the system property first, then from the environment variable, and
   * falls back to the default value when both are absent or malformed.
   */
  protected static int intSetting(String systemProperty, String envName, int defaultValue) {
    String value = System.getProperty(systemProperty);
    if (value == null || value.trim().isEmpty()) {
      value = System.getenv(envName);
    }
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ex) {
      log.warn("Invalid value '{}' for {}, fallback to {}", value, envName, defaultValue);
      return defaultValue;
    }
  }

  /**
   * Signals a refused WebSocket handshake. Carries the HTTP status code answered by the server so
   * that the retry logic can tell a permanent client error (403 Forbidden, 429 Too Many Requests,
   * ...) from a transient network failure.
   */
  static final class WebSocketConnectException extends Exception {
    private static final long serialVersionUID = 1L;

    /** The HTTP status code of the handshake response, 0 when no response was received. */
    final int httpStatusCode;

    WebSocketConnectException(String message, int httpStatusCode, Throwable cause) {
      super(message, cause);
      this.httpStatusCode = httpStatusCode;
    }
  }

  @Override
  public void onClosed(WebSocket webSocket, int code, String reason) {
    // Invoked when both peers have indicated that no more messages will be
    // transmitted and the connection has been successfully released. No further
    // calls to this
    // listener will be made.
    log.debug(
        StringUtils.format("WebSocket %s closed: %d, %s", webSocket.toString(), code, reason));
    isOpen.set(false);
  }

  @Override
  public void onClosing(WebSocket webSocket, int code, String reason) {
    // Invoked when the remote peer has indicated that no more incoming messages
    // will be
    // transmitted.
    // 服务端异常也会close code 1001需要处理
    // RFC 6455
    // Endpoints MAY use the following pre-defined status codes when sending a Close
    // frame.
    // 1000 indicates a normal closure, meaning that the purpose for which the
    // connection was established has been fulfilled.
    // 1001 indicates that an endpoint is "going away", such as a server going down
    // or a browser having navigated away from a page.
    // 1002 indicates that an endpoint is terminating the connection due to a
    // protocol error.
    // 1003 indicates that an endpoint is terminating the connection because it has
    // received a type of data it cannot accept (e.g., an
    // endpoint that understands only text data MAY send this if it receives a
    // binary message)
    webSocket.close(code, null);
    log.debug(StringUtils.format("Websocket is closing, code: %s, reasion: %s", code, reason));
    if (responseEmitter != null && !responseEmitter.isCancelled()) {
      responseEmitter.onComplete();
    } else { // close on idle, such as server close the connection.
      ;
    }
  }

  /**
   * Parse WebSocket handshake failure response body to extract error details. Returns an
   * ApiException with the original error code/message if parsing succeeds, otherwise returns null.
   */
  private ApiException parseWebSocketHandshakeError(
      int httpStatusCode, String responseBody, Throwable cause) {
    if (responseBody == null || responseBody.isEmpty()) {
      return null;
    }

    try {
      JsonObject jsonResponse = JsonUtils.parse(responseBody);
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

      // If we have a business error code, use it directly with the HTTP status code
      if (!code.isEmpty()) {
        Status status =
            Status.builder()
                .statusCode(httpStatusCode)
                .code(code)
                .message(message)
                .requestId(requestId)
                .isJson(true)
                .build();
        return new ApiException(status, cause);
      }
    } catch (Throwable e) {
      log.debug("Failed to parse WebSocket handshake error response as JSON", e);
    }

    return null;
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    // Invoked when a web socket has been closed due to an error reading from or
    // writing to the network.
    // Both outgoing and incoming messages may have been lost. No further calls to
    // this listener will be made.

    if (isClosed.get()) {
      log.debug("called close before but not working, close again in onFailure.");
      close(1001, "call closed before");
      // Do not leave establishWebSocketClient() waiting for the 60s timeout.
      releaseConnectionWaiter();
      return;
    }

    String responseBody = "";
    int httpStatusCode = 0;
    // Get response body if there is.
    if (response != null) {
      httpStatusCode = response.code();
      ResponseBody body = response.body();
      if (body != null) {
        try {
          responseBody = body.string();
        } catch (IOException ex) {
          log.error(ex.getMessage());
        }
      }
    }
    String failureMessage =
        StringUtils.format(
            "Websocket failure %s, cause: %s, body: %s",
            t.getMessage(), t.getCause(), responseBody);
    if (httpStatusCode > 0) {
      // Appended, not inlined, to keep the message prefix stable for log based diagnostics.
      failureMessage = failureMessage + ", http status: " + httpStatusCode;
    }
    log.error(failureMessage);
    isOpen.set(false);

    // Try to parse the response body for structured error information
    ApiException parsedException = parseWebSocketHandshakeError(httpStatusCode, responseBody, t);

    if (connectionEmitter != null && !connectionEmitter.isCancelled()) {
      // Wrap the parsed ApiException (when available) so the caller sees the structured error
      // code, while WebSocketConnectException still carries the http status for retry decisions.
      Throwable connectError =
          parsedException != null ? parsedException : new Exception(failureMessage, t);
      connectionEmitter.onError(
          new WebSocketConnectException(failureMessage, httpStatusCode, connectError));
    } else if (responseEmitter != null && !responseEmitter.isCancelled()) {
      // error on request
      if (parsedException != null) {
        responseEmitter.onError(parsedException);
      } else {
        responseEmitter.onError(new Exception(failureMessage, t));
      }
    } else {
      log.error(failureMessage);
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    if (isClosed.get()) {
      log.debug("called close before but not working, close again in onMessage.");
      close(1001, "call closed before");
      return;
    }
    log.debug(text);
    // Invoked when a text (type 0x1) message has been received.
    if (!isFirstMessage.get()) {
      log.debug("Receive first package.");
      isFirstMessage.set(true);
    }
    try {
      // Check different message.
      WebSocketResponse response = JsonUtils.fromJson(text, WebSocketResponse.class);
      switch (response.header.event) {
        case TASK_STARTED:
          // if has payload, call onNext.
          if (response.payload.output != null || response.payload.usage != null) {
            try {
              responseEmitter.onNext(
                  new DashScopeResult()
                      .fromResponse(
                          Protocol.WEBSOCKET,
                          NetworkResponse.builder().message(text).httpStatusCode(200).build(),
                          isFlattenResult));
            } catch (Exception e) {
              log.error("Failed to create result for TASK_STARTED", e);
              if (!responseEmitter.isCancelled()) {
                responseEmitter.onError(e);
              }
            }
          } else if (passTaskStarted.get()) {
            try {
              DashScopeResult start_message =
                  new DashScopeResult()
                      .fromResponse(
                          Protocol.WEBSOCKET,
                          NetworkResponse.builder().message(text).httpStatusCode(200).build(),
                          isFlattenResult);
              start_message.setEvent(WebSocketEventType.TASK_STARTED.getValue());
              responseEmitter.onNext(start_message);
            } catch (Exception e) {
              log.error("Failed to create start_message for TASK_STARTED", e);
              if (!responseEmitter.isCancelled()) {
                responseEmitter.onError(e);
              }
            }
          }
          break;
        case TASK_FAILED:
          log.error(StringUtils.format("Receive task_failed message: %s", text));
          Status st =
              Status.builder()
                  .code(response.header.code)
                  .message(response.header.message)
                  .requestId(response.header.taskId)
                  .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
                  .isJson(true)
                  .build();
          // throw new ApiException(st);
          if (!responseEmitter.isCancelled()) {
            responseEmitter.onError(new ApiException(st));
          } else {
            log.error(StringUtils.format("Something wrong, receive task failed message: %s", text));
          }
          break;
        case TASK_FINISHED:
          // check the payload and usage is null.
          if (response.payload.output != null || response.payload.usage != null) {
            try {
              responseEmitter.onNext(
                  new DashScopeResult()
                      .fromResponse(
                          Protocol.WEBSOCKET,
                          NetworkResponse.builder().message(text).httpStatusCode(200).build(),
                          isFlattenResult));
            } catch (Exception e) {
              log.error("[DEBUG] Failed to create result for TASK_FINISHED", e);
              if (!responseEmitter.isCancelled()) {
                responseEmitter.onError(e);
              }
            }
          }
          responseEmitter.onComplete();
          break;
        case RESULT_GENERATED:
          // get payload and usage.
          try {
            responseEmitter.onNext(
                new DashScopeResult()
                    .fromResponse(
                        Protocol.WEBSOCKET,
                        NetworkResponse.builder().message(text).httpStatusCode(200).build(),
                        isFlattenResult));
          } catch (Exception e) {
            log.error("Failed to create result for RESULT_GENERATED", e);
            if (!responseEmitter.isCancelled()) {
              responseEmitter.onError(e);
            }
          }
          break;
        default:
          // Protocol layer error: received undefined event type.
          // This is SDK-level handling, not an API standard error code.
          // Kept as hardcoded string to avoid polluting global ErrorType enum.
          responseEmitter.onError(
              new ApiException(
                  Status.builder()
                      .code("UnknownMessage")
                      .message(StringUtils.format("Receive unknown message: %s", text))
                      .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
                      .build()));
      }
    } catch (Throwable ex) {
      // Protocol layer error: JSON deserialization failed.
      // This is SDK-level handling for malformed messages, not an API standard error.
      // Kept as hardcoded string to maintain separation from business-layer errors.
      responseEmitter.onError(
          new ApiException(
              Status.builder()
                  .code("MessageFormatError")
                  .message(
                      StringUtils.format("Receive message: %s, json deserialize exception", text))
                  .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
                  .build()));
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, ByteString bytes) {
    // Invoked when a binary (type 0x2) message has been received.
    if (isClosed.get()) {
      log.debug("called close before but not working, close again in onMessage.");
      close(1001, "call closed before");
      return;
    }
    if (!isFirstMessage.get()) {
      log.debug("Receive first binary package.");
      isFirstMessage.set(true);
    }
    responseEmitter.onNext(
        new DashScopeResult()
            .fromResponse(
                Protocol.WEBSOCKET,
                NetworkResponse.builder().binary(bytes.asByteBuffer()).build(),
                isFlattenResult));
  }

  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    // the connection has been accepted by the remote peer and may begin
    // transmitting messages
    // Invoked when a web socket has been accepted by the remote peer and may begin
    // transmitting
    // messages..
    if (isClosed.get()) {
      log.debug("called close before but not working, close again in onOpen.");
      close(1001, "call closed before");
      // Do not leave establishWebSocketClient() waiting for the 60s timeout.
      releaseConnectionWaiter();
      return;
    }
    isOpen.set(true);
    if (connectionEmitter != null && !connectionEmitter.isCancelled()) {
      connectionEmitter.onComplete();
    }
  }

  protected void sendTextWithRetry(
      String apiKey,
      boolean isSecurityCheck,
      String message,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl) {
    // Guard: skip if already cancelled
    if (isClosed.get()) {
      log.debug("sendTextWithRetry skipped: connection already closed.");
      return;
    }
    // simple retry with fixed delay, no strategy
    if (!isOpen.get()) {
      establishWebSocketClient(apiKey, isSecurityCheck, workspace, customHeaders, baseWebSocketUrl);
    }
    if (isClosed.get()) {
      return;
    }
    int maxRetries = 3;
    if (passTaskStarted.get()) {
      // when pass througn task started, no need to retry.
      if (webSocketClient == null) {
        log.warn("webSocketClient is null, cannot send message.");
        return;
      }
      log.info("Sending message: " + message);
      Boolean isOk = webSocketClient.send(message);
      if (!isOk) {
        log.warn("Send request failed, return without retry.");
      }
      return;
    }
    int retryCount = 0;
    while (retryCount < maxRetries) {
      if (isClosed.get()) {
        return;
      }
      if (webSocketClient == null) {
        log.warn("webSocketClient is null, cannot send message.");
        return;
      }
      log.debug("Sending message: " + message);
      Boolean isOk = webSocketClient.send(message);
      if (isOk) {
        break;
      } else {
        establishWebSocketClient(
            apiKey, isSecurityCheck, workspace, customHeaders, baseWebSocketUrl);
        log.warn(
            StringUtils.format(
                "Send request failed, the connection may closed, will reconnect and send again"));
      }
      Observable.timer(5000, TimeUnit.MILLISECONDS).blockingSingle();
      ++retryCount;
    }
  }

  protected void sendBinaryWithRetry(
      String apiKey,
      boolean isSecurityCheck,
      ByteString message,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl) {
    // Guard: skip if already cancelled
    if (isClosed.get()) {
      return;
    }
    if (!isOpen.get()) {
      establishWebSocketClient(apiKey, isSecurityCheck, workspace, customHeaders, baseWebSocketUrl);
    }
    if (isClosed.get()) {
      return;
    }
    int maxRetries = 3;
    int retryCount = 0;
    while (retryCount < maxRetries) {
      if (isClosed.get()) {
        return;
      }
      if (webSocketClient == null) {
        log.warn("webSocketClient is null, cannot send binary message.");
        return;
      }
      Boolean isOk = webSocketClient.send(message);
      if (isOk) {
        break;
      } else {
        establishWebSocketClient(
            apiKey, isSecurityCheck, workspace, customHeaders, baseWebSocketUrl);
        log.warn(
            StringUtils.format(
                "Send request failed, the connection may closed, will reconnect and send again"));
      }
      Observable.timer(5000, TimeUnit.MILLISECONDS).blockingSingle();
      ++retryCount;
    }
  }

  private void sendBatchRequest(HalfDuplexRequest req) {
    if (req.getWebsocketBinaryData() != null) {
      // send start-task.
      sendTextWithRetry(
          req.getApiKey(),
          req.isSecurityCheck(),
          JsonUtils.toJson(req.getStartTaskMessage()),
          req.getWorkspace(),
          req.getHeaders(),
          req.getBaseWebSocketUrl());
      // send binary data.
      sendBinaryWithRetry(
          req.getApiKey(),
          req.isSecurityCheck(),
          ByteString.of(req.getWebsocketBinaryData()),
          req.getWorkspace(),
          req.getHeaders(),
          req.getBaseWebSocketUrl());
    } else {
      // data and start-task in same package.
      sendTextWithRetry(
          req.getApiKey(),
          req.isSecurityCheck(),
          JsonUtils.toJson(req.getStartTaskMessage()),
          req.getWorkspace(),
          req.getHeaders(),
          req.getBaseWebSocketUrl());
    }
  }

  @Override
  public DashScopeResult send(HalfDuplexRequest req) {
    // send the request out.
    if (req.getStreamingMode() == StreamingMode.NONE
        || req.getStreamingMode() == StreamingMode.IN) {
      Flowable<DashScopeResult> flowable =
          Flowable.<DashScopeResult>create(
              emitter -> {
                this.responseEmitter = emitter;
                this.isFlattenResult = req.getIsFlatten();
              },
              BackpressureStrategy.BUFFER);
      flowable.subscribe().dispose();
      sendBatchRequest(req);
      return flowable.blockingSingle();
    } else {
      throw new ApiException(
          Status.builder()
              .code("Invalid call")
              .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
              .message("Please use streamOut interface of websocket.")
              .build());
    }
  }

  @Override
  public void send(HalfDuplexRequest req, ResultCallback<DashScopeResult> callback) {
    if (req.getStreamingMode() == StreamingMode.NONE
        || req.getStreamingMode() == StreamingMode.IN) {
      // Create flowable and subscribe with callback directly
      // No need for the initial subscribe().dispose() pattern which causes emitter to be disposed
      Flowable<DashScopeResult> flowable =
          Flowable.<DashScopeResult>create(
              emitter -> {
                this.responseEmitter = emitter;
                this.isFlattenResult = req.getIsFlatten();
              },
              BackpressureStrategy.BUFFER);

      // Subscribe first to initialize responseEmitter
      Disposable subscription =
          flowable.subscribe(
              msg -> {
                callback.onEvent(msg);
              },
              err -> {
                callback.onError(new ApiException(err));
              },
              new Action() {
                @Override
                public void run() throws Exception {
                  callback.onComplete();
                }
              });

      // Now send the request - responseEmitter is already initialized and active
      sendBatchRequest(req);

      // Note: Don't dispose here - let the WebSocket lifecycle manage the subscription
      // The subscription will be completed when onClosing/onFailure is called
    } else {
      throw new ApiException(
          Status.builder()
              .code("Invalid call")
              .statusCode(Constants.DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE)
              .message("Please use streamOut interface of websocket.")
              .build());
    }
  }

  @Override
  public Flowable<DashScopeResult> streamOut(HalfDuplexRequest req) {
    // Set receive
    Flowable<DashScopeResult> flowable =
        Flowable.<DashScopeResult>create(
            emitter -> {
              this.responseEmitter = emitter;
              this.isFlattenResult = req.getIsFlatten();
            },
            BackpressureStrategy.BUFFER);
    flowable.subscribe().dispose();
    // send the request out.
    sendBatchRequest(req);
    return flowable;
  }

  @Override
  public void streamOut(HalfDuplexRequest req, ResultCallback<DashScopeResult> callback) {
    Flowable<DashScopeResult> flowable = streamOut(req);
    flowable.subscribe(
        msg -> {
          callback.onEvent(msg);
        },
        err -> {
          callback.onError(new ApiException(err));
        },
        new Action() {
          @Override
          public void run() throws Exception {
            callback.onComplete();
          }
        });
  }

  /**
   * Hook method called before sending the start message. Subclasses may override to add additional
   * logging or pre-processing.
   */
  protected void onBeforeSendStartMessage(JsonObject startMessage) {
    // no-op by default
  }

  /**
   * Publishes a terminal error on the response stream. When no subscriber is attached yet, the
   * error is parked and replayed as soon as one subscribes: a failure raised early, a handshake
   * refused by the server for instance, can otherwise be reported before the caller of duplex() had
   * the chance to subscribe, which would leave that caller waiting forever.
   */
  protected void emitResponseError(Throwable error) {
    FlowableEmitter<DashScopeResult> emitter = this.responseEmitter;
    if (emitter != null && !emitter.isCancelled()) {
      emitter.onError(error);
      return;
    }
    pendingResponseError.set(error);
    // A subscriber may have attached in the meantime, do not let the parked error be forgotten.
    drainPendingResponseError();
  }

  /** Replays the parked terminal error, if any, to the currently attached subscriber. */
  private void drainPendingResponseError() {
    FlowableEmitter<DashScopeResult> emitter = this.responseEmitter;
    if (emitter == null || emitter.isCancelled()) {
      return;
    }
    Throwable parked = pendingResponseError.getAndSet(null);
    if (parked != null) {
      emitter.onError(parked);
    }
  }

  /** Core streaming request logic. Extracted to allow subclasses to use different executors. */
  protected void executeStreamRequest(FullDuplexRequest req) {
    try {
      isClosed.set(false); // Reset for reuse across sessions
      isFirstMessage.set(false);
      pendingResponseError.set(null);

      JsonObject startMessage = req.getStartTaskMessage();
      onBeforeSendStartMessage(startMessage);
      String taskId = startMessage.get("header").getAsJsonObject().get("task_id").getAsString();
      // send start message out.
      sendTextWithRetry(
          req.getApiKey(),
          req.isSecurityCheck(),
          JsonUtils.toJson(startMessage),
          req.getWorkspace(),
          req.getHeaders(),
          req.getBaseWebSocketUrl());

      Flowable<Object> streamingData = req.getStreamingData();
      Disposable d =
          streamingData.subscribe(
              data -> {
                try {
                  if (data instanceof String) {
                    JsonObject continueData = req.getContinueMessage((String) data, taskId);
                    sendTextWithRetry(
                        req.getApiKey(),
                        req.isSecurityCheck(),
                        JsonUtils.toJson(continueData),
                        req.getWorkspace(),
                        req.getHeaders(),
                        req.getBaseWebSocketUrl());
                  } else if (data instanceof byte[]) {
                    sendBinaryWithRetry(
                        req.getApiKey(),
                        req.isSecurityCheck(),
                        ByteString.of((byte[]) data),
                        req.getWorkspace(),
                        req.getHeaders(),
                        req.getBaseWebSocketUrl());
                  } else if (data instanceof ByteBuffer) {
                    sendBinaryWithRetry(
                        req.getApiKey(),
                        req.isSecurityCheck(),
                        ByteString.of((ByteBuffer) data),
                        req.getWorkspace(),
                        req.getHeaders(),
                        req.getBaseWebSocketUrl());
                  } else {
                    JsonObject continueData = req.getContinueMessage(data, taskId);
                    sendTextWithRetry(
                        req.getApiKey(),
                        req.isSecurityCheck(),
                        JsonUtils.toJson(continueData),
                        req.getWorkspace(),
                        req.getHeaders(),
                        req.getBaseWebSocketUrl());
                  }
                } catch (Throwable ex) {
                  log.error(StringUtils.format("sendStreamData exception: %s", ex.getMessage()));
                  emitResponseError(ex);
                }
              },
              err -> {
                log.error(StringUtils.format("Get stream data error!"));
                emitResponseError(err);
              },
              new Action() {
                @Override
                public void run() throws Exception {
                  log.debug(StringUtils.format("Stream data send completed!"));
                  sendTextWithRetry(
                      req.getApiKey(),
                      req.isSecurityCheck(),
                      JsonUtils.toJson(req.getFinishedTaskMessage(taskId)),
                      req.getWorkspace(),
                      req.getHeaders(),
                      req.getBaseWebSocketUrl());
                }
              });
      // Publish the disposable, then check if cancel() raced ahead.
      // If isClosed is already true, cancel() has already run and missed
      // this disposable, so we must dispose it ourselves.
      streamingDataDisposable = d;
      if (isClosed.get()) {
        d.dispose();
      }
    } catch (Throwable ex) {
      log.error(StringUtils.format("sendStreamData exception: %s", ex.getMessage()));
      emitResponseError(ex);
    }
  }

  protected CompletableFuture<Void> sendStreamRequest(FullDuplexRequest req) {
    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> executeStreamRequest(req));
    return future;
  }

  private void joinSendFuture(CompletableFuture<Void> future) {
    try {
      if (future.isDone()) {
        future.join();
      } else {
        future.cancel(true);
        future.join();
      }
    } catch (CancellationException ex) {
      log.error("Sending streaming data cancelled", ex.getMessage());
    } catch (CompletionException ex) {
      log.error("Sending streaming data exception", ex.getMessage());
      emitResponseError(ex.getCause() != null ? ex.getCause() : ex);
    }
  }

  @Override
  public DashScopeResult streamIn(FullDuplexRequest req) {
    Flowable<DashScopeResult> flowable =
        Flowable.<DashScopeResult>create(
            emitter -> {
              this.responseEmitter = emitter;
              this.isFlattenResult = req.getIsFlatten();
              drainPendingResponseError();
            },
            BackpressureStrategy.BUFFER);
    flowable.subscribe().dispose();
    CompletableFuture<Void> future = sendStreamRequest(req);
    DashScopeResult result =
        flowable
            .doOnError(
                err -> {
                  joinSendFuture(future);
                })
            .doOnComplete(
                new Action() {
                  @Override
                  public void run() throws Exception {
                    joinSendFuture(future);
                  }
                })
            .blockingFirst();
    return result;
  }

  @Override
  public void streamIn(FullDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException {
    DashScopeResult res = streamIn(req);
    callback.onEvent(res);
    callback.onComplete();
  }

  @Override
  public Flowable<DashScopeResult> duplex(FullDuplexRequest req)
      throws NoApiKeyException, ApiException {
    Flowable<DashScopeResult> flowable =
        Flowable.<DashScopeResult>create(
            emitter -> {
              this.responseEmitter = emitter;
              this.isFlattenResult = req.getIsFlatten();
              drainPendingResponseError();
            },
            BackpressureStrategy.BUFFER);
    // No need to subscribe here: sendStreamRequest() handles the actual WebSocket connection
    // and the returned flowable will be subscribed by the caller
    CompletableFuture<Void> future = sendStreamRequest(req);

    return flowable
        .doOnError(
            err -> {
              joinSendFuture(future);
            })
        .doOnComplete(
            new Action() {
              @Override
              public void run() throws Exception {
                joinSendFuture(future);
              }
            });
  }

  @Override
  public void duplex(FullDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException {
    Flowable<DashScopeResult> flowable = duplex(req);
    flowable.subscribe(
        msg -> {
          callback.onEvent(msg);
        },
        err -> {
          callback.onError(new ApiException(err));
        },
        new Action() {
          @Override
          public void run() throws Exception {
            callback.onComplete();
          }
        });
  }
}
