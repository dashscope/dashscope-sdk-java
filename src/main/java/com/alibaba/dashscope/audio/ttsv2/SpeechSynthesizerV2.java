// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.ttsv2;

import com.alibaba.dashscope.audio.protocol.AudioWebsocketCallback;
import com.alibaba.dashscope.audio.protocol.AudioWebsocketRequest;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisUsage;
import com.alibaba.dashscope.audio.tts.timestamp.Sentence;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;

/** @author songsong.sss */
@Slf4j
public final class SpeechSynthesizerV2 implements AudioWebsocketCallback {
  private SpeechSynthesisState state = SpeechSynthesisState.IDLE;
  private ResultCallback<SpeechSynthesisResult> callback;

  private AtomicReference<CountDownLatch> stopLatch = new AtomicReference<>(null);

  private SpeechSynthesisParam parameters;

  @Getter private ByteBuffer audioData;
  private String preRequestId = null;
  private boolean isFirst = true;
  private AtomicBoolean canceled = new AtomicBoolean(false);
  private boolean asyncCall = false;
  private long startStreamTimeStamp = -1;
  private long firstPackageTimeStamp = -1;
  private double recvAudioLength = 0;
  @Getter @Setter private long startedTimeout = 5000;
  @Getter @Setter private long firstAudioTimeout = -1;
  private AtomicReference<CountDownLatch> startLatch = new AtomicReference<>(null);
  private AtomicReference<CountDownLatch> firstAudioLatch = new AtomicReference<>(null);
  private AudioWebsocketRequest websocketRequest;
  private String websocketUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
  private JsonObject bailianHeader = new JsonObject();
  private static final String HEADER_ACTION = "action";
  private static final String TASK_ID = "task_id";
  private String taskId;
  private boolean enableSsml = false;

  /**
   * CosyVoice Speech Synthesis SDK
   *
   * @param param Configuration for speech synthesis, including voice type, volume, etc.
   * @param callback In non-streaming output scenarios, this can be set to null
   * @param baseUrl Base URL
   * @param connectionOptions Connection options
   */
  public SpeechSynthesizerV2(
      SpeechSynthesisParam param,
      ResultCallback<SpeechSynthesisResult> callback,
      String baseUrl,
      ConnectionOptions connectionOptions) {
    if (baseUrl != null) {
      this.websocketUrl = baseUrl;
    }
    this.parameters = param;
    this.callback = callback;
    this.asyncCall = this.callback != null;
    this.taskId = UUID.randomUUID().toString();
  }

  /**
   * CosyVoice Speech Synthesis SDK
   *
   * @param baseUrl Base URL
   * @param connectionOptions Connection options
   */
  public SpeechSynthesizerV2(String baseUrl, ConnectionOptions connectionOptions) {
    this(null, null, baseUrl, connectionOptions);
  }

  /** CosyVoice Speech Synthesis SDK */
  public SpeechSynthesizerV2() {
    this(null, null, null, null);
  }

  public void updateParamAndCallback(
      SpeechSynthesisParam param, ResultCallback<SpeechSynthesisResult> callback) {
    this.parameters = param;
    this.callback = callback;
    this.canceled.set(false);

    // reset inner params
    this.stopLatch = new AtomicReference<>(null);
    this.startLatch = new AtomicReference<>(null);
    this.firstAudioLatch = new AtomicReference<>(null);
    this.firstAudioTimeout = -1;
    this.isFirst = true;

    this.asyncCall = this.callback != null;
    this.taskId = UUID.randomUUID().toString();
  }

  /**
   * CosyVoice Speech Synthesis SDK
   *
   * @param param Configuration for speech synthesis, including voice type, volume, etc.
   * @param callback In non-streaming output scenarios, this can be set to null
   * @param baseUrl Base URL
   */
  public SpeechSynthesizerV2(
      SpeechSynthesisParam param, ResultCallback<SpeechSynthesisResult> callback, String baseUrl) {
    this(param, callback, baseUrl, null);
  }

  /**
   * CosyVoice Speech Synthesis SDK
   *
   * @param param Configuration for speech synthesis, including voice type, volume, etc.
   * @param callback In non-streaming output scenarios, this can be set to null
   */
  public SpeechSynthesizerV2(
      SpeechSynthesisParam param, ResultCallback<SpeechSynthesisResult> callback) {
    this(param, callback, null, null);
  }

  public String getLastRequestId() {
    return preRequestId;
  }

  private void checkConnectStatus() {
    websocketRequest.checkStatus();
  }

  public void connect() throws NoApiKeyException, InterruptedException {
    startStreamTimeStamp = System.currentTimeMillis();
    this.canceled.set(false);
    if (websocketRequest != null && websocketRequest.isOpen()) {
      websocketRequest.close();
    }

    websocketRequest = new AudioWebsocketRequest();
    websocketRequest.connect(
        parameters.getApiKey(),
        parameters.getWorkspace(),
        parameters.getHeaders(),
        websocketUrl,
        this);
  }

  public void close() {
    if (websocketRequest != null && websocketRequest.isOpen()) {
      try {
        websocketRequest.close();
      } catch (Exception e) {
        log.warn("Failed to close websocket connection: " + e.getMessage());
      }
    }
  }

  private void sendTaskMessage(String action, JsonObject input) {
    JsonObject wsMessage = new JsonObject();

    bailianHeader.addProperty(HEADER_ACTION, action);
    bailianHeader.addProperty(TASK_ID, taskId);

    JsonObject payload = new JsonObject();
    if ("run-task".equals(action)) {
      payload.addProperty("task_group", "audio");
      payload.addProperty("task", "tts");
      payload.addProperty("function", "SpeechSynthesizer");
      payload.addProperty("model", this.parameters.getModel());
      JsonObject parameters = JsonUtils.toJsonObject(this.parameters.getParameters());
      if (enableSsml) {
        parameters.addProperty("enable_ssml", true);
      }
      payload.add("parameters", parameters);

      payload.add("input", input != null ? input : new JsonObject());
    } else {
      payload.add("input", input != null ? input : new JsonObject());
    }

    wsMessage.add("header", JsonUtils.toJsonObject(bailianHeader));
    wsMessage.add("payload", JsonUtils.toJsonObject(payload));
    log.info("sendTaskMessage: {}", wsMessage.toString());
    websocketRequest.sendTextMessage(wsMessage.toString());
  }

  public void startSynthesizer(boolean enableSsml) throws InterruptedException {
    bailianHeader.addProperty("streaming", "duplex");
    this.enableSsml = enableSsml;
    sendTaskMessage("run-task", new JsonObject());
  }

  public void sendText(String text) {
    JsonObject input = new JsonObject();
    input.addProperty("text", text);
    sendTaskMessage("continue-task", input);
  }

  public void stopSynthesizer() {
    sendTaskMessage("finish-task", new JsonObject());
  }

  @Override
  public void onOpen() {
    log.info("WebSocket connection opened");
    if (callback != null) {
      callback.onOpen(null);
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    log.debug("Received text message: " + text);
    try {
      JsonObject messageObj = JsonParser.parseString(text).getAsJsonObject();
      if (messageObj.has("header")) {
        JsonObject header = messageObj.getAsJsonObject("header");
        if (header.has("event")) {
          String event = header.get("event").getAsString();

          switch (event) {
            case "task-started":
              handleTaskStarted(messageObj);
              break;
            case "task-finished":
              handleTaskFinished(messageObj);
              break;
            case "task-failed":
              handleTaskFailed(messageObj);
              break;
            case "result-generated":
              handleResultGenerated(messageObj);
              break;
            default:
              log.warn("Unknown event: " + event);
              break;
          }
        }
      }
    } catch (Exception e) {
      log.error("Error processing text message: " + e.getMessage(), e);
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, ByteBuffer bytes) {
    log.debug("Received binary message, size: " + bytes.remaining());
    try {
      // 创建新的 buffer 并正确准备
      ByteBuffer audioFrame = ByteBuffer.allocate(bytes.remaining());
      audioFrame.put(bytes);
      audioFrame.flip();

      if (callback != null) {
        SpeechSynthesisResult result = new SpeechSynthesisResult();
        result.setAudioFrame(audioFrame);
        callback.onEvent(result);
      } else {
        if (audioData != null) {
          // 创建新 buffer
          ByteBuffer newBuffer =
              ByteBuffer.allocate(audioData.remaining() + audioFrame.remaining());
          newBuffer.put(audioData);
          newBuffer.put(audioFrame);
          newBuffer.flip();
          audioData = newBuffer;
        } else {
          audioData = audioFrame;
        }
      }

      // Update received audio length
      recvAudioLength += bytes.remaining();

      // Record timestamp of first audio package
      if (firstPackageTimeStamp == -1) {
        firstPackageTimeStamp = System.currentTimeMillis();
        if (firstAudioLatch.get() != null) {
          firstAudioLatch.get().countDown();
        }
      }
    } catch (Exception e) {
      log.error("Error processing binary message: " + e.getMessage(), e);
      if (callback != null) {
        callback.onError(e);
      }
    }
  }

  @Override
  public void onError(WebSocket webSocket, Throwable t) {
    if (callback != null) {
      // callback error first and then count down stopLatch
      Exception exception = new ApiException(t);
      callback.onError(exception);

      if (stopLatch.get() != null) {
        stopLatch.get().countDown();
      }
    }
  }

  @Override
  public void onClose(int code, String reason) {
    log.warn("WebSocket connection closed: " + reason + " (" + code + ")");
  }

  private void handleTaskStarted(JsonObject message) {
    log.info("Task started");
    state = SpeechSynthesisState.TTS_STARTED;
    firstPackageTimeStamp = -1;
    if (startLatch.get() != null) {
      startLatch.get().countDown();
    }
    if (callback == null) {
      audioData = null;
    }
  }

  private void handleTaskFinished(JsonObject message) {
    log.info("Task finished");
    if (stopLatch.get() != null) {
      stopLatch.get().countDown();
    }
    if (callback != null) {
      callback.onComplete();
    }
    // Reset for reuse
    isFirst = true;
  }

  private void handleTaskFailed(JsonObject message) {
    log.error("Task failed: " + message.toString());
    if (callback != null) {
      String errorMessage = "Unknown error";
      if (message.has("header") && message.getAsJsonObject("header").has("error_message")) {
        errorMessage = message.getAsJsonObject("header").get("error_message").getAsString();
      }

      // Create a Status object for the ApiException
      com.alibaba.dashscope.common.Status status =
          com.alibaba.dashscope.common.Status.builder()
              .statusCode(-1)
              .code("TASK_FAILED")
              .message(errorMessage)
              .build();
      callback.onError(new ApiException(status));

      if (stopLatch.get() != null) {
        stopLatch.get().countDown();
      }
    }
  }

  private void handleResultGenerated(JsonObject message) {
    log.debug("Result generated: " + message.toString());
    if (callback == null) {
      return;
    }
    SpeechSynthesisResult result = new SpeechSynthesisResult();
    if (message.has("header")) {
      JsonObject header = message.getAsJsonObject("header");
      if (header.has("task_id")) {
        preRequestId = header.get("task_id").getAsString();
        result.setRequestId(preRequestId);
      }
    }
    if (message.has("payload")) {
      JsonObject payload = message.getAsJsonObject("payload");
      if (payload != null && payload.has("output")) {
        JsonObject output = payload.getAsJsonObject("output");
        result.setOutput(output);
        if (output != null && output.has("sentence")) {
          result.setTimestamp(
              JsonUtils.fromJsonObject(output.getAsJsonObject("sentence"), Sentence.class));
        }
      }
      if (payload != null && payload.has("usage")) {
        result.setUsage(
            JsonUtils.fromJsonObject(payload.getAsJsonObject("usage"), SpeechSynthesisUsage.class));
      }
    }
    callback.onEvent(result);
  }

  /** First Package Delay is the time between start sending text and receive first audio package */
  public long getFirstPackageDelay() {
    return this.firstPackageTimeStamp - this.startStreamTimeStamp;
  }

  private void startStream(boolean enableSsml) throws NoApiKeyException, InterruptedException {

    if (websocketRequest == null || !websocketRequest.isOpen()) {
      // if websocket is not open, then connect
      connect();
    } else {
      startStreamTimeStamp = System.currentTimeMillis();
    }

    checkConnectStatus(); // check websocket connection， if socket is closed.
    startLatch = new AtomicReference<>(new CountDownLatch(1));
    startSynthesizer(enableSsml);
    boolean startResult = startLatch.get().await(startedTimeout, TimeUnit.MILLISECONDS);
    if (!startResult) {
      throw new RuntimeException(
          "TimeoutError: waiting for task started more than" + startedTimeout + " ms.");
    }
  }

  private void submitText(String text) {
    if (text == null || text.isEmpty()) {
      throw new ApiException(
          new InputRequiredException("Parameter invalid: text is null or empty"));
    }
    synchronized (this) {
      if (state != SpeechSynthesisState.TTS_STARTED) {
        throw new ApiException(
            new InputRequiredException(
                "State invalid: expect stream input tts state is started but " + state.getValue()));
      }
      sendText(text);
    }
  }

  private void startStream() throws NoApiKeyException, InterruptedException {
    startStream(false);
  }

  public void streamingComplete(long completeTimeoutMillis) {
    log.debug("streamingComplete with timeout: " + completeTimeoutMillis);
    synchronized (this) {
      if (state != SpeechSynthesisState.TTS_STARTED) {
        throw new ApiException(
            new RuntimeException(
                "State invalid: expect stream input tts state is started but " + state.getValue()));
      }
    }
    stopLatch = new AtomicReference<>(new CountDownLatch(1));
    stopSynthesizer();

    if (stopLatch.get() != null) {
      try {
        if (completeTimeoutMillis > 0) {
          log.debug("start waiting for stopLatch");
          if (!stopLatch.get().await(completeTimeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("TimeoutError: waiting for streaming complete");
          }
        } else {
          log.debug("start waiting for stopLatch");
          stopLatch.get().await();
        }
        log.debug("stopLatch is done");
      } catch (InterruptedException ignored) {
        log.error("Interrupted while waiting for streaming complete");
      }
    }
  }

  public void streamingComplete() {
    streamingComplete(600000);
  }

  public void asyncStreamingComplete() {
    synchronized (this) {
      if (state != SpeechSynthesisState.TTS_STARTED) {
        throw new ApiException(
            new RuntimeException(
                "State invalid: expect stream input tts state is started but " + state.getValue()));
      }
    }
    stopSynthesizer();
  }

  public void streamingCancel() {
    canceled.set(true);
    synchronized (this) {
      if (state != SpeechSynthesisState.TTS_STARTED) {
        return;
      }
    }
    stopSynthesizer();
  }

  public void streamingCall(String text) {
    if (isFirst) {
      isFirst = false;
      try {
        this.startStream(false);
        this.submitText(text);
      } catch (InterruptedException e) {
        log.error("Interrupted while waiting for streaming complete", e);
      } catch (NoApiKeyException e) {
        throw new ApiException(e);
      }
    } else {
      this.submitText(text);
    }
  }

  public ByteBuffer call(String text, long timeoutMillis) throws RuntimeException {
    try {
      this.startStream(true);
      this.submitText(text);
    } catch (InterruptedException e) {
      log.error("Interrupted while waiting for streaming complete", e);
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
    if (this.asyncCall) {
      this.asyncStreamingComplete();
      return null;
    } else {
      this.streamingComplete(timeoutMillis);
      return audioData;
    }
  }

  public ByteBuffer call(String text) {
    return call(text, 0);
  }
}
