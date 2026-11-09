// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import static com.alibaba.dashscope.audio.live.QwenLiveConstants.*;

import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.DashScopeHeaders;
import com.alibaba.dashscope.protocol.okhttp.OkHttpClientFactory;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

@Slf4j
public class QwenLiveConversation {
  private static final Gson EVENT_GSON = JsonUtils.gson.newBuilder().serializeNulls().create();
  private final QwenLiveConnectionParam param;
  private final QwenLiveCallback callback;
  private final QwenLiveCallbackDispatcher callbackDispatcher;
  private final WebSocket.Factory factory;
  private final int connectTimeoutSeconds;
  private final Object lifecycleLock = new Object();
  private final AtomicBoolean connectAttempted = new AtomicBoolean(false);
  private final AtomicBoolean isOpen = new AtomicBoolean(false);
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private final AtomicBoolean sessionClosedReceived = new AtomicBoolean(false);
  private final AtomicReference<CountDownLatch> connectLatch = new AtomicReference<>();
  private final AtomicReference<CountDownLatch> sessionClosedLatch = new AtomicReference<>();
  private volatile WebSocket webSocketClient;
  private volatile Throwable connectFailure;
  private volatile String sessionId;
  private volatile String closeReason;
  private boolean closeRequested;
  private boolean socketCancelled;

  public QwenLiveConversation(QwenLiveConnectionParam param, QwenLiveCallback callback) {
    this(param, callback, OkHttpClientFactory.getOkHttpClient(), 60);
  }

  QwenLiveConversation(
      QwenLiveConnectionParam param,
      QwenLiveCallback callback,
      WebSocket.Factory factory,
      int connectTimeoutSeconds) {
    this.param = Objects.requireNonNull(param, "param");
    this.callback = Objects.requireNonNull(callback, "callback");
    this.factory = Objects.requireNonNull(factory, "factory");
    if (connectTimeoutSeconds <= 0) {
      throw new IllegalArgumentException("connectTimeoutSeconds must be positive.");
    }
    this.connectTimeoutSeconds = connectTimeoutSeconds;
    this.callbackDispatcher =
        new QwenLiveCallbackDispatcher(
            param.getCallbackExecutor(),
            param.getCallbackQueueCapacity(),
            this::failCallbackDispatch,
            this::notifyError);
  }

  public void connect() throws NoApiKeyException, InterruptedException {
    synchronized (lifecycleLock) {
      if (isClosed.get() || !connectAttempted.compareAndSet(false, true)) {
        throw new IllegalStateException("Qwen Live conversation cannot connect again.");
      }
      connectLatch.set(new CountDownLatch(1));
      sessionClosedLatch.set(new CountDownLatch(1));
    }
    try {
      Request request = buildConnectionRequest();
      WebSocket created =
          Objects.requireNonNull(factory.newWebSocket(request, new Listener()), "socket");
      boolean terminal;
      synchronized (lifecycleLock) {
        terminal = isClosed.get() || connectFailure != null;
        if (!terminal) {
          webSocketClient = created;
        }
      }
      if (terminal) cancelSocket(created);
      if (!connectLatch.get().await(connectTimeoutSeconds, TimeUnit.SECONDS)) {
        cancel();
        throw new IllegalStateException("Timed out connecting Qwen Live websocket.");
      }
      synchronized (lifecycleLock) {
        if (connectFailure != null) {
          throw new IllegalStateException("Failed connecting Qwen Live websocket.", connectFailure);
        }
        checkStatus();
      }
    } catch (InterruptedException e) {
      cancel();
      Thread.currentThread().interrupt();
      throw e;
    } catch (NoApiKeyException | RuntimeException e) {
      synchronized (lifecycleLock) {
        if (connectFailure == null) connectFailure = e;
        markClosed();
      }
      cancel();
      throw e;
    }
  }

  private Request buildConnectionRequest() throws NoApiKeyException {
    Request.Builder builder = new Request.Builder().url(param.getUrl());
    Map<String, String> headers =
        DashScopeHeaders.buildWebSocketHeaders(
            param.getApikey(), false, param.getWorkspace(), param.getHeaders(), null, "audio");
    for (Map.Entry<String, String> header : headers.entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }
    return builder.build();
  }

  public void checkStatus() {
    if (!isOpen.get() || isClosed.get() || webSocketClient == null) {
      throw new IllegalStateException("Qwen Live websocket is not open.");
    }
  }

  public String updateSession(QwenLiveSessionConfig config) {
    requireValue(config, "config");
    JsonObject payload = new JsonObject();
    payload.add(FIELD_SESSION, config.getConfig());
    return sendEvent(EVENT_SESSION_UPDATE, payload, true);
  }

  public String appendAudio(String audioBase64) {
    requireValue(audioBase64, "audioBase64");
    JsonObject payload = new JsonObject();
    payload.addProperty(FIELD_AUDIO, audioBase64);
    return sendEvent(EVENT_SESSION_INPUT_AUDIO_APPEND, payload, false);
  }

  public String appendAudioBytes(byte[] audio) {
    requireValue(audio, "audio");
    return appendAudio(Base64.getEncoder().encodeToString(audio));
  }

  public String appendAudioBytes(byte[] audio, int offset, int length) {
    requireValue(audio, "audio");
    if (offset < 0 || length < 0 || offset > audio.length - length) {
      throw new IllegalArgumentException("Audio slice is out of bounds.");
    }
    return appendAudio(
        Base64.getEncoder().encodeToString(Arrays.copyOfRange(audio, offset, offset + length)));
  }

  public String muteInputAudio() {
    return sendEvent(EVENT_SESSION_INPUT_AUDIO_MUTE, new JsonObject(), true);
  }

  public String unmuteInputAudio() {
    return sendEvent(EVENT_SESSION_INPUT_AUDIO_UNMUTE, new JsonObject(), true);
  }

  public String appendInstruction(String delegationId, String content) {
    return sendDelegationContent(EVENT_SESSION_INSTRUCTION_APPEND, delegationId, content);
  }

  public String appendThinking(String delegationId, String content) {
    return sendDelegationContent(EVENT_SESSION_THINKING_APPEND, delegationId, content);
  }

  public String appendCommentary(String delegationId, String content) {
    return sendDelegationContent(EVENT_SESSION_COMMENTARY_APPEND, delegationId, content);
  }

  private String sendDelegationContent(String type, String delegationId, String content) {
    QwenLiveSessionConfig.requireText(delegationId, FIELD_DELEGATION_ID);
    requireValue(content, FIELD_CONTENT);
    JsonObject payload = new JsonObject();
    payload.addProperty(FIELD_DELEGATION_ID, delegationId);
    payload.addProperty(FIELD_CONTENT, content);
    return sendEvent(type, payload, true);
  }

  public String createResponseItem(JsonObject item) {
    requireValue(item, FIELD_ITEM);
    JsonObject payload = new JsonObject();
    payload.add(FIELD_ITEM, item.deepCopy());
    return sendEvent(EVENT_RESPONSE_ITEM_CREATE, payload, true);
  }

  public String createResponse() {
    return sendEvent(EVENT_RESPONSE_CREATE, new JsonObject(), true);
  }

  public String sendRaw(String rawJson) {
    JsonObject payload = parseObject(rawJson);
    String type = stringField(payload, FIELD_TYPE);
    QwenLiveSessionConfig.requireText(type, FIELD_TYPE);
    return sendEvent(type, payload, !EVENT_SESSION_INPUT_AUDIO_APPEND.equals(type));
  }

  private String sendEvent(String type, JsonObject payload, boolean enableLog) {
    JsonObject event = payload.deepCopy();
    String eventId = "event_" + UUID.randomUUID().toString().replace("-", "");
    event.addProperty(FIELD_TYPE, type);
    event.addProperty(FIELD_EVENT_ID, eventId);
    String message = EVENT_GSON.toJson(event);
    synchronized (lifecycleLock) {
      checkStatus();
      WebSocket socket = webSocketClient;
      if (socket == null || !socket.send(message)) {
        throw new IllegalStateException("Failed to enqueue Qwen Live event: " + eventId);
      }
    }
    if (enableLog) {
      log.debug("Qwen Live sent: {}", message);
    } else {
      log.debug("Qwen Live audio event: {}, message length: {}", eventId, message.length());
    }
    return eventId;
  }

  private static void requireValue(Object value, String field) {
    if (value == null) throw new IllegalArgumentException(field + " must not be null.");
  }

  private static JsonObject parseObject(String json) {
    requireValue(json, "json");
    try (JsonReader reader = new JsonReader(new StringReader(json))) {
      reader.setLenient(false);
      JsonElement parsed = Streams.parse(reader);
      if (!parsed.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) {
        throw new IllegalArgumentException("Expected a single JSON object.");
      }
      return parsed.getAsJsonObject();
    } catch (IOException | com.google.gson.JsonParseException e) {
      throw new IllegalArgumentException("Invalid JSON object.", e);
    }
  }

  private static String stringField(JsonObject object, String field) {
    JsonElement value = object.get(field);
    return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
        ? value.getAsString()
        : null;
  }

  public void close() {
    close(1000, "bye");
  }

  public void close(int code, String reason) {
    validateClose(code, reason);
    WebSocket socket;
    boolean wasOpen;
    synchronized (lifecycleLock) {
      if (closeRequested || socketCancelled) {
        return;
      }
      closeRequested = true;
      wasOpen = isOpen.get();
      socket = webSocketClient;
      markClosing();
    }
    if (socket == null || !wasOpen || !socket.close(code, reason)) {
      cancel();
    }
  }

  public void cancel() {
    WebSocket socket;
    synchronized (lifecycleLock) {
      markClosed();
      socket = webSocketClient;
    }
    cancelSocket(socket);
  }

  private void cancelSocket(WebSocket socket) {
    if (socket == null) return;
    synchronized (lifecycleLock) {
      if (socketCancelled) return;
      socketCancelled = true;
    }
    socket.cancel();
  }

  private static void validateClose(int code, String reason) {
    if (code < 1000
        || code >= 5000
        || (code >= 1004 && code <= 1006)
        || (code >= 1015 && code < 3000)) {
      throw new IllegalArgumentException("Invalid websocket close code: " + code);
    }
    if (reason != null && reason.getBytes(StandardCharsets.UTF_8).length > 123) {
      throw new IllegalArgumentException("Websocket close reason exceeds 123 UTF-8 bytes.");
    }
  }

  public boolean awaitSessionClosed(int timeoutSeconds) throws InterruptedException {
    if (timeoutSeconds < 0)
      throw new IllegalArgumentException("timeoutSeconds must not be negative.");
    if (sessionClosedReceived.get()) return true;
    CountDownLatch latch = sessionClosedLatch.get();
    if (latch == null) return false;
    try {
      latch.await(timeoutSeconds, TimeUnit.SECONDS);
      return sessionClosedReceived.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw e;
    }
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getCloseReason() {
    return closeReason;
  }

  private void markClosing() {
    isClosed.set(true);
    isOpen.set(false);
    CountDownLatch connecting = connectLatch.get();
    if (connecting != null) connecting.countDown();
  }

  private void markClosed() {
    markClosing();
    CountDownLatch closing = sessionClosedLatch.get();
    if (closing != null) closing.countDown();
  }

  private void dispatchCallback(Runnable action) {
    callbackDispatcher.dispatch(
        () -> {
          try {
            action.run();
          } catch (Throwable failure) {
            notifyError(failure);
          }
        });
  }

  private void reportError(Throwable failure) {
    callbackDispatcher.dispatch(() -> notifyError(failure));
  }

  private void notifyError(Throwable failure) {
    try {
      callback.onError(failure);
    } catch (Throwable callbackFailure) {
      log.warn("Qwen Live onError callback failed.", callbackFailure);
    }
  }

  private void failCallbackDispatch(Throwable failure) {
    log.warn("Qwen Live callback dispatch failed; cancelling session.", failure);
    synchronized (lifecycleLock) {
      if (connectFailure == null) connectFailure = failure;
    }
    cancel();
  }

  private static ApiException serverError(JsonObject event) {
    JsonElement detail = event.get(FIELD_ERROR);
    JsonObject error =
        detail != null && detail.isJsonObject() ? detail.getAsJsonObject() : new JsonObject();
    String code = stringField(error, "code");
    if (code == null) code = stringField(error, FIELD_TYPE);
    String message = stringField(error, "message");
    return new ApiException(
        Status.builder()
            .statusCode(-1)
            .code(code)
            .message(message != null ? message : "Qwen Live server error.")
            .requestId(stringField(event, "request_id"))
            .build());
  }

  private class Listener extends WebSocketListener {
    @Override
    public void onOpen(WebSocket socket, Response response) {
      boolean terminal;
      synchronized (lifecycleLock) {
        terminal = isClosed.get();
        if (!terminal) {
          webSocketClient = socket;
          isOpen.set(true);
          CountDownLatch latch = connectLatch.get();
          if (latch != null) latch.countDown();
        }
      }
      if (terminal) {
        cancelSocket(socket);
        return;
      }
      dispatchCallback(callback::onOpen);
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
      JsonObject event;
      try {
        event = parseObject(text);
      } catch (RuntimeException failure) {
        reportError(failure);
        return;
      }
      String type = stringField(event, FIELD_TYPE);
      synchronized (lifecycleLock) {
        if (EVENT_SESSION_CREATED.equals(type)) {
          JsonElement session = event.get(FIELD_SESSION);
          if (session != null && session.isJsonObject()) {
            String id = stringField(session.getAsJsonObject(), FIELD_ID);
            if (id != null) sessionId = id;
          }
        } else if (EVENT_SESSION_CLOSED.equals(type)) {
          closeReason = stringField(event, FIELD_REASON);
          sessionClosedReceived.set(true);
          markClosed();
        }
      }
      ApiException failure = EVENT_ERROR.equals(type) ? serverError(event) : null;
      dispatchCallback(
          () -> {
            if (failure != null) notifyError(failure);
            callback.onEvent(event);
          });
    }

    @Override
    public void onClosing(WebSocket socket, int code, String reason) {
      synchronized (lifecycleLock) {
        markClosed();
      }
      if (!socket.close(code, reason)) cancelSocket(socket);
    }

    @Override
    public void onClosed(WebSocket socket, int code, String reason) {
      synchronized (lifecycleLock) {
        markClosed();
      }
      dispatchCallback(() -> callback.onClose(code, reason));
    }

    @Override
    public void onFailure(WebSocket socket, Throwable failure, Response response) {
      synchronized (lifecycleLock) {
        if (webSocketClient == null) webSocketClient = socket;
        if (connectFailure == null) connectFailure = failure;
        markClosed();
      }
      cancelSocket(socket);
      reportError(failure);
    }
  }
}
