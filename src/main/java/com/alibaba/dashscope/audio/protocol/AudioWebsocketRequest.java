package com.alibaba.dashscope.audio.protocol;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.DashScopeHeaders;
import com.alibaba.dashscope.protocol.okhttp.OkHttpClientFactory;
import com.alibaba.dashscope.utils.ApiKey;
import com.alibaba.dashscope.utils.Constants;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;

/** @author songsong.shao */
@Slf4j
public class AudioWebsocketRequest extends WebSocketListener {

  private OkHttpClient client;
  private WebSocket websocketClient;
  private AtomicBoolean isOpen = new AtomicBoolean(false);
  private AtomicReference<CountDownLatch> connectLatch = new AtomicReference<>(null);
  private AtomicBoolean isClosed = new AtomicBoolean(false);
  private AudioWebsocketCallback callback;
  private Integer connectTimeout = 5000;

  public boolean isOpen() {
    return isOpen.get();
  }

  public boolean isClosed() {
    return isClosed.get();
  }

  public void checkStatus() {
    if (this.isClosed.get()) {
      throw new RuntimeException("Websocket is already closed!");
    }
  }

  public void connect(
      String apiKey,
      String workspace,
      Map<String, String> customHeaders,
      String baseWebSocketUrl,
      AudioWebsocketCallback callback)
      throws NoApiKeyException, InterruptedException, RuntimeException {
    Request request =
        buildConnectionRequest(
            ApiKey.getApiKey(apiKey), false, workspace, customHeaders, baseWebSocketUrl);
    this.callback = callback;
    client = OkHttpClientFactory.getOkHttpClient();
    websocketClient = client.newWebSocket(request, this);
    connectLatch.set(new CountDownLatch(1));
    boolean result = connectLatch.get().await(connectTimeout, TimeUnit.MILLISECONDS);
    if (!result) {
      throw new RuntimeException(
          "TimeoutError: waiting for websocket connect more than" + connectTimeout + " ms.");
    }
  }

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

  private void sendMessage(String message, boolean enableLog) {
    checkStatus();
    if (enableLog) {
      log.debug("send message: " + message);
    }
    if (!websocketClient.send(message)) {
      log.warn("Failed to enqueue websocket text message for sending.");
    }
  }

  public void close() {
    this.close(1000, "bye");
  }

  public void close(int code, String reason) {
    checkStatus();
    websocketClient.close(code, reason);
    isClosed.set(true);
  }

  public void sendTextMessage(String message) {
    checkStatus();
    this.sendMessage(message, true);
  }

  public void sendBinaryMessage(ByteString rawData) {
    checkStatus();
    if (!websocketClient.send(rawData)) {
      log.warn("Failed to enqueue websocket binary message for sending.");
    }
  }

  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    isOpen.set(true);
    if (connectLatch.get() != null) {
      connectLatch.get().countDown();
    }

    log.debug("WebSocket opened");
    callback.onOpen();
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    callback.onMessage(webSocket, text);
  }

  @Override
  public void onMessage(WebSocket webSocket, ByteString bytes) {
    log.debug("Received binary message");
    callback.onMessage(webSocket, bytes.asByteBuffer());
  }

  @Override
  public void onClosed(WebSocket webSocket, int code, String reason) {
    isOpen.set(false);
    isClosed.set(true);
    if (connectLatch.get() != null) {
      connectLatch.get().countDown();
    }
    log.debug("WebSocket closed");
    callback.onClose(code, reason);
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    log.error("WebSocket failed: " + t);
    if (connectLatch.get() != null) {
      connectLatch.get().countDown();
    }
    if (callback != null) {
      callback.onError(webSocket, t);
    } else {
      throw new RuntimeException(t);
    }
  }
}
