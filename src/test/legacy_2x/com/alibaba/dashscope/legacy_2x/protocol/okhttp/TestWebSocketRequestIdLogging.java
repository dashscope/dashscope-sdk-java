// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.protocol.okhttp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.legacy_2x.audio.protocol.AudioWebsocketCallback;
import com.alibaba.dashscope.legacy_2x.audio.protocol.AudioWebsocketRequest;
import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.LoggerFactory;
import org.slf4j.simple.SimpleLogger;

@Execution(ExecutionMode.SAME_THREAD)
public class TestWebSocketRequestIdLogging {
  private static class SdkLogCapture implements AutoCloseable {
    private final PrintStream originalErr = System.err;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final SimpleLogger logger;
    private final Field levelField;
    private final int originalLevel;

    SdkLogCapture(Class<?> loggerClass) throws Exception {
      logger = (SimpleLogger) LoggerFactory.getLogger(loggerClass);
      levelField = SimpleLogger.class.getDeclaredField("currentLogLevel");
      levelField.setAccessible(true);
      originalLevel = levelField.getInt(logger);
      levelField.setInt(logger, 10);
      System.setErr(new PrintStream(output, true, StandardCharsets.UTF_8.name()));
    }

    String output() {
      return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws Exception {
      System.setErr(originalErr);
      levelField.setInt(logger, originalLevel);
    }
  }

  private static OkHttpWebSocketClient newClient() {
    OkHttpWebSocketClient client = new OkHttpWebSocketClient(new OkHttpClient(), false);
    Flowable.<DashScopeResult>create(
            emitter -> client.responseEmitter = emitter, BackpressureStrategy.BUFFER)
        .subscribe();
    return client;
  }

  private static Response failureResponse(String requestId) {
    return new Response.Builder()
        .request(new Request.Builder().url("http://localhost/").build())
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(500)
        .message("failed")
        .header("x-request-id", requestId)
        .build();
  }

  private static AudioWebsocketRequest newAudioRequest() throws Exception {
    AudioWebsocketRequest request = new AudioWebsocketRequest();
    Field callbackField = AudioWebsocketRequest.class.getDeclaredField("callback");
    callbackField.setAccessible(true);
    callbackField.set(
        request,
        new AudioWebsocketCallback() {
          @Override
          public void onOpen() {}

          @Override
          public void onMessage(WebSocket webSocket, String text) {}

          @Override
          public void onMessage(WebSocket webSocket, ByteBuffer buffer) {}

          @Override
          public void onError(WebSocket webSocket, Throwable t) {}

          @Override
          public void onClose(int code, String reason) {}
        });
    return request;
  }

  @Test
  public void testOkHttpLogsUnknownBeforeRequestIdIsAvailable() throws Exception {
    OkHttpWebSocketClient client = new OkHttpWebSocketClient(new OkHttpClient(), false);
    try (SdkLogCapture capture = new SdkLogCapture(OkHttpWebSocketClient.class)) {
      client.onFailure(null, new IOException("failed"), null);
      assertTrue(capture.output().contains("requestId=unknown"));
    }
  }

  @Test
  public void testOkHttpLogsTaskIdFromMessage() throws Exception {
    try (SdkLogCapture capture = new SdkLogCapture(OkHttpWebSocketClient.class)) {
      newClient()
          .onMessage(
              null,
              "{\"header\":{\"task_id\":\"websocket-task\",\"event\":\"task-started\"},"
                  + "\"payload\":{}}");
      String output = capture.output();
      assertTrue(output.contains("requestId=websocket-task"));
      assertTrue(output.contains("Receive first package. requestId=websocket-task"));
    }
  }

  @Test
  public void testOkHttpHandshakeFailureLogsResponseHeaderRequestId() throws Exception {
    OkHttpWebSocketClient client = new OkHttpWebSocketClient(new OkHttpClient(), false);
    try (SdkLogCapture capture = new SdkLogCapture(OkHttpWebSocketClient.class)) {
      client.onFailure(null, new IOException("handshake failed"), failureResponse("handshake-id"));
      assertTrue(capture.output().contains("requestId=handshake-id"));
    }
  }

  @Test
  public void testAudioLogsUnknownBeforeRequestIdIsAvailable() throws Exception {
    AudioWebsocketRequest request = newAudioRequest();
    try (SdkLogCapture capture = new SdkLogCapture(AudioWebsocketRequest.class)) {
      request.onClosed(null, 1000, "bye");
      assertTrue(capture.output().contains("requestId=unknown"));
    }
  }

  @Test
  public void testAudioTaskIdIsNotOverwrittenByFailureResponseHeader() throws Exception {
    AudioWebsocketRequest request = newAudioRequest();
    try (SdkLogCapture capture = new SdkLogCapture(AudioWebsocketRequest.class)) {
      request.onMessage(null, "{\"header\":{\"task_id\":\"audio-task\"}}");
      request.onFailure(null, new IOException("failed"), failureResponse("handshake-id"));
      String output = capture.output();
      assertTrue(output.contains("requestId=audio-task"));
      assertFalse(output.contains("requestId=handshake-id"));
    }
  }

  @Test
  public void testCancelledSendTextLogsRequestId() throws Exception {
    OkHttpWebSocketClient client = newClient();
    try (SdkLogCapture capture = new SdkLogCapture(OkHttpWebSocketClient.class)) {
      client.cancel();
      client.sendTextWithRetry("key", false, "{}", null, null, "ws://localhost", "audio");
      assertTrue(
          capture
              .output()
              .contains("sendTextWithRetry skipped: connection already closed. requestId=unknown"));
    }
  }

  @Test
  public void testNullClientSendWarnsLogRequestId() throws Exception {
    OkHttpWebSocketClient client = newClient();
    Field isOpen = OkHttpWebSocketClient.class.getDeclaredField("isOpen");
    isOpen.setAccessible(true);
    ((AtomicBoolean) isOpen.get(client)).set(true);
    try (SdkLogCapture capture = new SdkLogCapture(OkHttpWebSocketClient.class)) {
      client.sendTextWithRetry("key", false, "{}", null, null, "ws://localhost", "audio");
      client.sendBinaryWithRetry(
          "key", false, ByteString.EMPTY, null, null, "ws://localhost", "audio");
      String output = capture.output();
      assertTrue(
          output.contains("webSocketClient is null, cannot send message. requestId=unknown"));
      assertTrue(
          output.contains(
              "webSocketClient is null, cannot send binary message. requestId=unknown"));
    }
  }

  @Test
  public void testCancelledEstablishLogsRequestId() throws Exception {
    OkHttpWebSocketClient client = newClient();
    try (SdkLogCapture capture = new SdkLogCapture(OkHttpWebSocketClient.class)) {
      client.cancel();
      Method establish =
          OkHttpWebSocketClient.class.getDeclaredMethod(
              "establishWebSocketClient",
              String.class,
              boolean.class,
              String.class,
              Map.class,
              String.class,
              String.class);
      establish.setAccessible(true);
      establish.invoke(client, "key", false, null, null, "ws://localhost", "audio");
      assertTrue(
          capture.output().contains("Connection cancelled, stop reconnecting. requestId=unknown"));
    }
  }
}
