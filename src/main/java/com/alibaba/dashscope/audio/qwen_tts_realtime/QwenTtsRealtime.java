// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.qwen_tts_realtime;

import static com.alibaba.dashscope.utils.JsonUtils.gson;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.DashScopeHeaders;
import com.alibaba.dashscope.protocol.okhttp.OkHttpClientFactory;
import com.alibaba.dashscope.utils.ApiKey;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;

/** @author lengjiayi */
@Slf4j
public class QwenTtsRealtime extends WebSocketListener {
  private QwenTtsRealtimeParam parameters;
  private QwenTtsRealtimeCallback callback;

  private OkHttpClient client;
  private WebSocket websocktetClient;
  private AtomicBoolean isOpen = new AtomicBoolean(false);
  private AtomicReference<CountDownLatch> connectLatch = new AtomicReference<>(null);
  private String sessionId = null;
  private String lastResponseId = null;
  private long lastFirstTextTime = -1;
  private long lastFirstAudioDelay = -1;
  private AtomicBoolean isClosed = new AtomicBoolean(false);

  /**
   * Constructor
   *
   * @param param apikey, model, url, etc.
   * @param callback callback
   */
  public QwenTtsRealtime(QwenTtsRealtimeParam param, QwenTtsRealtimeCallback callback) {
    this.parameters = param;
    this.callback = callback;
  }

  public void checkStatus() {
    if (this.isClosed.get()) {
      throw new RuntimeException("tts is already closed!");
    }
  }

  /** Qwen Tts Realtime APIs */

  /** Connect to server, create session and return default session configuration */
  public void connect() throws NoApiKeyException, InterruptedException {
    checkStatus();
    Request request =
        buildConnectionRequest(
            ApiKey.getApiKey(parameters.getApikey()),
            false,
            parameters.getWorkspace(),
            parameters.getHeaders(),
            parameters.getUrl());
    client = OkHttpClientFactory.getOkHttpClient();
    websocktetClient = client.newWebSocket(request, this);
    connectLatch.set(new CountDownLatch(1));
    connectLatch.get().await();
  }

  /**
   * Update session configuration, should be used before append text
   *
   * @param config session configuration
   */
  public void updateSession(QwenTtsRealtimeConfig config) {
    checkStatus();
    JsonObject configJson = config.getConfig();
    Map<String, Object> update_request = new HashMap<>();
    update_request.put(QwenTtsRealtimeConstants.PROTOCOL_EVENT_ID, generateSessionId());
    update_request.put(
        QwenTtsRealtimeConstants.PROTOCOL_TYPE,
        QwenTtsRealtimeConstants.PROTOCOL_EVENT_TYPE_UPDATE_SESSION);
    update_request.put(QwenTtsRealtimeConstants.PROTOCOL_SESSION, configJson);
    sendMessage(gson.toJson(update_request), true);
  }

  /**
   * Send text
   *
   * @param text text to send
   */
  public void appendText(String text) {
    checkStatus();
    Map<String, Object> append_request = new HashMap<>();
    append_request.put(QwenTtsRealtimeConstants.PROTOCOL_EVENT_ID, generateSessionId());
    append_request.put(
        QwenTtsRealtimeConstants.PROTOCOL_TYPE,
        QwenTtsRealtimeConstants.PROTOCOL_EVENT_TYPE_APPEND_TEXT);
    append_request.put(QwenTtsRealtimeConstants.PROTOCOL_TEXT, text);
    sendMessage(gson.toJson(append_request), true);
    if (lastFirstTextTime < 0) {
      lastFirstTextTime = System.currentTimeMillis();
    }
  }

  /** Commit the text sent before, create response and start synthesis audio. */
  public void commit() {
    checkStatus();
    Map<String, Object> commit_request = new HashMap<>();
    commit_request.put(QwenTtsRealtimeConstants.PROTOCOL_EVENT_ID, generateSessionId());
    commit_request.put(
        QwenTtsRealtimeConstants.PROTOCOL_TYPE,
        QwenTtsRealtimeConstants.PROTOCOL_EVENT_TYPE_COMMIT);
    sendMessage(gson.toJson(commit_request), true);
  }

  /** Clear the text sent to server before. */
  public void clearAppendedText() {
    checkStatus();
    Map<String, Object> clear_request = new HashMap<>();
    clear_request.put(QwenTtsRealtimeConstants.PROTOCOL_EVENT_ID, generateSessionId());
    clear_request.put(
        QwenTtsRealtimeConstants.PROTOCOL_TYPE,
        QwenTtsRealtimeConstants.PROTOCOL_EVENT_TYPE_CLEAR_TEXT);
    sendMessage(gson.toJson(clear_request), true);
  }

  /** cancel the current response */
  public void cancelResponse() {
    checkStatus();
    Map<String, Object> cancel_request = new HashMap<>();
    cancel_request.put(QwenTtsRealtimeConstants.PROTOCOL_EVENT_ID, generateSessionId());
    cancel_request.put(
        QwenTtsRealtimeConstants.PROTOCOL_TYPE,
        QwenTtsRealtimeConstants.PROTOCOL_EVENT_TYPE_CANCEL_RESPONSE);
    sendMessage(gson.toJson(cancel_request), true);
  }

  /** finish input text stream, server will synthesis all text in buffer and close the connection */
  public void finish() {
    checkStatus();
    Map<String, Object> cancel_request = new HashMap<>();
    cancel_request.put(QwenTtsRealtimeConstants.PROTOCOL_EVENT_ID, generateSessionId());
    cancel_request.put(
        QwenTtsRealtimeConstants.PROTOCOL_TYPE,
        QwenTtsRealtimeConstants.PROTOCOL_EVENT_SESSION_FINISH);
    sendMessage(gson.toJson(cancel_request), true);
  }

  /** close the connection to server */
  public void close() {

    checkStatus();
    websocktetClient.close(1000, "bye");
    isClosed.set(true);
  }

  /**
   * close the connection to server
   *
   * @param code websocket close code
   * @param reason websocket close reason
   */
  public void close(int code, String reason) {
    checkStatus();
    websocktetClient.close(code, reason);
    isClosed.set(true);
  }

  /**
   * send raw data to server
   *
   * @param rawData raw data
   */
  public void sendRaw(String rawData) {
    checkStatus();
    sendMessage(rawData, true);
  }

  public String getSessionId() {
    return sessionId;
  }
  public String getResponseId() {
    return lastResponseId;
  }

  public long getFirstAudioDelay() {
    return lastFirstAudioDelay;
  }

  /** WebSocket utils */

  /**
   * build connection request
   *
   * @param apiKey api key
   * @param isSecurityCheck is security check
   * @param workspace workspace
   * @param customHeaders custom headers
   * @param baseWebSocketUrl base web socket url
   * @return request
   * @throws NoApiKeyException no api key
   */
  private Request buildConnectionRequest(
      String apiKey,
      boolean isSecurityCheck,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl)
      throws NoApiKeyException {
    // build the request builder.
    Request.Builder bd = new Request.Builder();
    bd.headers(
        Headers.of(
            DashScopeHeaders.buildWebSocketHeaders(
                apiKey, isSecurityCheck, workspace, customHeaders)));
    String url = Constants.baseWebsocketApiUrl;
    if (baseWebSocketUrl != null) {
      url = baseWebSocketUrl;
    }
    Request request = bd.url(url).build();
    return request;
  }

  private String generateSessionId() {
    return "event_" + java.util.UUID.randomUUID().toString().replace("-", "");
  }

  private void sendMessage(String message, boolean enableLog) {
    if (enableLog == true) {
      log.debug("send message: " + message);
    }
    Boolean isOk = websocktetClient.send(message);
  }

  private void sendMessage(ByteString message) {
    websocktetClient.send(message);
  }

  /** WebSocket callbacks */
  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    isOpen.set(true);
    connectLatch.get().countDown();
    log.debug("WebSocket opened");
    callback.onOpen();
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    if (text.length() > 1024) {
      log.debug("Received message: " + text.substring(0, 1024));
    } else {
      log.debug("Received message: " + text);
    }
    JsonObject response = JsonUtils.parse(text);
    callback.onEvent(response);
    if (response.has("type")) {
      String type = response.get("type").getAsString();
      switch (type) {
        case QwenTtsRealtimeConstants.PROTOCOL_RESPONSE_TYPE_SESSION_CREATED:
          sessionId = response.get("session").getAsJsonObject().get("id").getAsString();
          break;
        case QwenTtsRealtimeConstants.PROTOCOL_RESPONSE_TYPE_RESPONSE_CREATED:
          lastResponseId = response.get("response").getAsJsonObject().get("id").getAsString();
          break;
        case QwenTtsRealtimeConstants.PROTOCOL_RESPONSE_TYPE_AUDIO_DELTA:
          if (lastFirstTextTime > 0 && lastFirstAudioDelay < 0) {
            lastFirstAudioDelay = System.currentTimeMillis() - lastFirstTextTime;
          }
          break;
        case QwenTtsRealtimeConstants.PROTOCOL_RESPONSE_TYPE_RESPONSE_DONE:
          log.debug(
              "[Metric] response: "
                  + lastResponseId
                  + ", first audio delay: "
                  + lastFirstAudioDelay
                  + " ms");
          break;
      }
    }
  }

  @Override
  public void onClosed(WebSocket webSocket, int code, String reason) {
    isOpen.set(false);
    connectLatch.get().countDown();
    log.debug("WebSocket closed");
    callback.onClose(code, reason);
  }

  @Override
  public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
    isClosed.set(true);
    websocktetClient.close(code, reason);
    log.debug("WebSocket closing: " + code + ", " + reason);
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    log.error("WebSocket failed: " + t.getMessage());
  }
}
