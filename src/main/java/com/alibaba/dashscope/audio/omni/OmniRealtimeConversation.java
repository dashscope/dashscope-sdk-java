// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.DashScopeHeaders;
import com.alibaba.dashscope.protocol.okhttp.OkHttpClientFactory;
import com.alibaba.dashscope.utils.ApiKey;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** @author lengjiayi */
@Slf4j
public class OmniRealtimeConversation extends WebSocketListener {
  private OmniRealtimeParam parameters;
  private OmniRealtimeCallback callback;

  private OkHttpClient client;
  private WebSocket websocktetClient;
  private AtomicBoolean isOpen = new AtomicBoolean(false);
  private AtomicReference<CountDownLatch> connectLatch = new AtomicReference<>(null);
  private String sessionId = null;
  private String lastResponseId = null;
  private long lastResponseCreateTime = -1;
  private long lastFirstAudioDelay = -1;
  private long lastFirstTextDelay = -1;
  private AtomicBoolean isClosed = new AtomicBoolean(false);

  /**
   * Constructor
   *
   * @param param apikey, model, url, etc.
   * @param callback callback
   */
  public OmniRealtimeConversation(OmniRealtimeParam param, OmniRealtimeCallback callback) {
    this.parameters = param;
    this.callback = callback;
  }

  /** Omni APIs */
  public void checkStatus() {
    if (this.isClosed.get()) {
      throw new RuntimeException("conversation is already closed!");
    }
  }

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
   * Update session configuration, should be used before create response
   *
   * @param config session configuration
   */
  public void updateSession(OmniRealtimeConfig config) {
    checkStatus();
    JsonObject configJson = config.getConfig();
    Map<String, Object> update_request = new HashMap<>();
    update_request.put(OmniRealtimeConstants.PROTOCOL_EVENT_ID, generateEventId());
    update_request.put(
        OmniRealtimeConstants.PROTOCOL_TYPE,
        OmniRealtimeConstants.PROTOCOL_EVENT_TYPE_UPDATE_SESSION);
    update_request.put(OmniRealtimeConstants.PROTOCOL_SESSION, configJson);
    GsonBuilder builder = new GsonBuilder();
    builder.serializeNulls();
    Gson gson = builder.create();
    sendMessage(gson.toJson(update_request), true);
  }

  /**
   * send audio in base64 format
   *
   * @param audioBase64 base64 audio string
   */
  public void appendAudio(String audioBase64) {
    checkStatus();
    Map<String, Object> append_request = new HashMap<>();
    String event_id = generateEventId();
    append_request.put(OmniRealtimeConstants.PROTOCOL_EVENT_ID, event_id);
    append_request.put(
        OmniRealtimeConstants.PROTOCOL_TYPE,
        OmniRealtimeConstants.PROTOCOL_EVENT_TYPE_APPEND_AUDIO);
    append_request.put(OmniRealtimeConstants.PROTOCOL_AUDIO, audioBase64);
    log.debug("append audio with eid: " + event_id + ", length: " + audioBase64.length());
    GsonBuilder builder = new GsonBuilder();
    builder.serializeNulls();
    Gson gson = builder.create();
    sendMessage(gson.toJson(append_request), false);
  }

  /**
   * send one image frame in video in base64 format
   *
   * @param videoBase64 base64 image string
   */
  public void appendVideo(String videoBase64) {
    checkStatus();
    Map<String, Object> append_request = new HashMap<>();
    String event_id = generateEventId();
    append_request.put(OmniRealtimeConstants.PROTOCOL_EVENT_ID, event_id);
    append_request.put(
        OmniRealtimeConstants.PROTOCOL_TYPE,
        OmniRealtimeConstants.PROTOCOL_EVENT_TYPE_APPEND_VIDEO);
    append_request.put(OmniRealtimeConstants.PROTOCOL_VIDEO, videoBase64);
    log.debug("append video with eid: " + event_id + ", length: " + videoBase64.length());
    GsonBuilder builder = new GsonBuilder();
    builder.serializeNulls();
    Gson gson = builder.create();
    sendMessage(gson.toJson(append_request), false);
  }

  /**
   * Commit the audio and video sent before. When in Server VAD mode, the client does not need to
   * use this method, the server will commit the audio automatically after detecting vad end.
   */
  public void commit() {
    checkStatus();
    Map<String, Object> commit_request = new HashMap<>();
    commit_request.put(OmniRealtimeConstants.PROTOCOL_EVENT_ID, generateEventId());
    commit_request.put(
        OmniRealtimeConstants.PROTOCOL_TYPE, OmniRealtimeConstants.PROTOCOL_EVENT_TYPE_COMMIT);
    GsonBuilder builder = new GsonBuilder();
    builder.serializeNulls();
    Gson gson = builder.create();
    sendMessage(gson.toJson(commit_request), true);
  }

  /** clear the audio sent to server before. */
  public void clearAppendedAudio() {
    checkStatus();
    Map<String, Object> clear_request = new HashMap<>();
    clear_request.put(OmniRealtimeConstants.PROTOCOL_EVENT_ID, generateEventId());
    clear_request.put(
        OmniRealtimeConstants.PROTOCOL_TYPE, OmniRealtimeConstants.PROTOCOL_EVENT_TYPE_CLEAR_AUDIO);
    GsonBuilder builder = new GsonBuilder();
    builder.serializeNulls();
    Gson gson = builder.create();
    sendMessage(gson.toJson(clear_request), true);
  }

  /**
   * create response, use audio and video commited before to request llm. When in Server VAD mode,
   * the client does not need to use this method, the server will create response automatically
   * after detecting vad and sending commit.
   *
   * @param instructions instructions to llm
   * @param modalities omni output modalities to be used in session
   */
  public void createResponse(String instructions, List<OmniRealtimeModality> modalities) {
    checkStatus();
    Map<String, Object> create_request = new HashMap<>();
    create_request.put(OmniRealtimeConstants.PROTOCOL_EVENT_ID, generateEventId());
    create_request.put(
        OmniRealtimeConstants.PROTOCOL_TYPE,
        OmniRealtimeConstants.PROTOCOL_EVENT_TYPE_CREATE_RESPONSE);
    if (instructions != null || modalities != null) {
      Map<String, Object> response = new HashMap<>();
      response.put("instructions", instructions);
      if (modalities != null) {
        response.put("modalities", modalities);
      }
      create_request.put("response", response);
    }
    GsonBuilder builder = new GsonBuilder();
    builder.serializeNulls();
    Gson gson = builder.create();
    sendMessage(gson.toJson(create_request), true);
  }

  /** cancel the current response */
  public void cancelResponse() {
    checkStatus();
    Map<String, Object> cancel_request = new HashMap<>();
    cancel_request.put(OmniRealtimeConstants.PROTOCOL_EVENT_ID, generateEventId());
    cancel_request.put(
        OmniRealtimeConstants.PROTOCOL_TYPE,
        OmniRealtimeConstants.PROTOCOL_EVENT_TYPE_CANCEL_RESPONSE);
    GsonBuilder builder = new GsonBuilder();
    builder.serializeNulls();
    Gson gson = builder.create();
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

  public long getFirstTextDelay() {
    return lastFirstTextDelay;
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

  private String generateEventId() {
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
        case OmniRealtimeConstants.PROTOCOL_RESPONSE_TYPE_SESSION_CREATED:
          sessionId = response.get("session").getAsJsonObject().get("id").getAsString();
          break;
        case OmniRealtimeConstants.PROTOCOL_RESPONSE_TYPE_RESPONSE_CREATED:
          lastResponseId = response.get("response").getAsJsonObject().get("id").getAsString();
          lastResponseCreateTime = System.currentTimeMillis();
          lastFirstAudioDelay = -1;
          lastFirstTextDelay = -1;
          break;
        case OmniRealtimeConstants.PROTOCOL_RESPONSE_TYPE_AUDIO_TRANSCRIPT_DELTA:
          if (lastResponseCreateTime > 0 && lastFirstTextDelay < 0) {
            lastFirstTextDelay = System.currentTimeMillis() - lastResponseCreateTime;
          }
          break;
        case OmniRealtimeConstants.PROTOCOL_RESPONSE_TYPE_AUDIO_DELTA:
          if (lastResponseCreateTime > 0 && lastFirstAudioDelay < 0) {
            lastFirstAudioDelay = System.currentTimeMillis() - lastResponseCreateTime;
          }
          break;
        case OmniRealtimeConstants.PROTOCOL_RESPONSE_TYPE_RESPONSE_DONE:
          log.debug(
              "[Metric] response: "
                  + lastResponseId
                  + ", first text delay: "
                  + lastFirstTextDelay
                  + " ms, first audio delay: "
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
    log.debug("WebSocket closed: " + code + ", " + reason);
    callback.onClose(code, reason);
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    log.error("WebSocket failed: " + t.getMessage());
  }

  @Override
  public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
    isClosed.set(true);
    websocktetClient.close(code, reason);
    log.debug("WebSocket closing: " + code + ", " + reason);
  }
}
