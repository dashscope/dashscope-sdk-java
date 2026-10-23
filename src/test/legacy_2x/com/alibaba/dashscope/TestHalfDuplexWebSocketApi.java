// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.Observable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestHalfDuplexWebSocketApi {
  private ApiServiceOption serviceOption =
      ApiServiceOption.builder()
          .protocol(Protocol.WEBSOCKET)
          .streamingMode(StreamingMode.NONE)
          .outputMode(OutputMode.ACCUMULATE)
          .taskGroup("group")
          .task("task")
          .function("function")
          .build();
  MockWebServer server;
  private JsonObject textOutput;
  private JsonObject usage;
  WebSocketRecorder serverListener;
  ByteBuffer binaryOutput;
  ByteBuffer binaryInput;

  @BeforeEach
  public void before() {
    textOutput = new JsonObject();
    textOutput.addProperty("text", "材料：\n猪脚一只，葱，姜，蒜头适量，冰糖两颗，料酒三大勺，生抽四大勺，盐适量");
    textOutput.addProperty("finish_reason", "stop");
    usage = new JsonObject();
    usage.addProperty("input_tokens", 16);
    usage.addProperty("output_tokens", 148);
    byte[] bytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
    binaryOutput = ByteBuffer.wrap(bytes);
    byte[] inBytes = {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19};
    binaryInput = ByteBuffer.wrap(inBytes);
    this.server = new MockWebServer();
    serverListener = new WebSocketRecorder("server");
    this.server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
    int port = this.server.getPort();
    Constants.baseWebsocketApiUrl = String.format("ws://127.0.0.1:%s/api-ws/v1/inference/", port);
  }

  @AfterEach
  public void after() throws IOException {
    System.out.println("Close Server!!!!!!!!!!!!!!!!!!!!!!!!");
    server.close();
    server.shutdown();
    server = null;
  }

  private void sendTextWithRetry(WebSocket wSocket, String message) {
    // simple retry with fixed delay, no strategy
    int maxRetries = 3;
    int retryCount = 0;
    while (retryCount < maxRetries) {
      Boolean isOk = wSocket.send(message);
      if (isOk) {
        break;
      } else {
        log.error("Send message: %s", message);
      }
      Observable.timer(5000, TimeUnit.MILLISECONDS).blockingSingle();
      ++retryCount;
    }
  }

  private void sendBinaryWithRetry(WebSocket wSocket, ByteString message) {
    int maxRetries = 3;
    int retryCount = 0;
    while (retryCount < maxRetries) {
      Boolean isOk = wSocket.send(message);
      if (isOk) {
        break;
      }
      Observable.timer(5000, TimeUnit.MILLISECONDS).blockingSingle();
      ++retryCount;
    }
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testStreamingNoneText2Text()
      throws ApiException, NoApiKeyException, IOException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeHalfDuplexApi<HalfDuplexTestParam> syncApi =
        new SynchronizeHalfDuplexApi<>(serviceOption);
    ;
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder()
            .parameter("hello", "world")
            .model("testModel")
            .prompt("prompt")
            .build();

    Semaphore semaphore = new Semaphore(0);
    List<DashScopeResult> results = new ArrayList<>();
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            results.add(msg);
            semaphore.release();
          }

          @Override
          public void onComplete() {}

          @Override
          public void onError(Exception e) {
            semaphore.release();
          }
        });
    WebSocket wsServer = serverListener.assertOpen();
    sendTextWithRetry(wsServer, JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    sendTextWithRetry(
        wsServer,
        JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(textOutput, usage)));
    wsServer.close(1000, "bye");
    semaphore.acquire();
    serverListener.assertHalfDuplexRequest(param, StreamingMode.NONE.getValue());
    assertEquals(results.get(0).getOutput(), textOutput);
    assertEquals(results.get(0).getUsage(), usage);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testStreamingNoneText2TextWithResources()
      throws ApiException, NoApiKeyException, IOException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeHalfDuplexApi<HalfDuplexTestParam> syncApi =
        new SynchronizeHalfDuplexApi<>(serviceOption);
    JsonObject resources = new JsonObject();
    resources.addProperty("str", "String");
    resources.addProperty("num", 100);
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder()
            .parameter("hello", "world")
            .model("testModel")
            .prompt("prompt")
            .resources(resources)
            .build();

    Semaphore semaphore = new Semaphore(0);
    List<DashScopeResult> results = new ArrayList<>();
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            results.add(msg);
            semaphore.release();
          }

          @Override
          public void onComplete() {}

          @Override
          public void onError(Exception e) {
            semaphore.release();
          }
        });
    WebSocket wsServer = serverListener.assertOpen();
    sendTextWithRetry(wsServer, JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    sendTextWithRetry(
        wsServer,
        JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(textOutput, usage)));
    wsServer.close(1000, "bye");
    semaphore.acquire();
    serverListener.assertResources(resources);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testStreamingNoneBinary2Text()
      throws ApiException, NoApiKeyException, IOException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeHalfDuplexApi<HalfDuplexBinaryParam> syncApi =
        new SynchronizeHalfDuplexApi<>(serviceOption);
    ;
    HalfDuplexBinaryParam param =
        HalfDuplexBinaryParam.builder()
            .parameter("hello", "world")
            .model("testModel")
            .data(binaryInput)
            .build();

    Semaphore semaphore = new Semaphore(0);
    List<DashScopeResult> results = new ArrayList<>();
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            results.add(msg);
            semaphore.release();
          }

          @Override
          public void onComplete() {}

          @Override
          public void onError(Exception e) {}
        });
    WebSocket wsServer = serverListener.assertOpen();
    sendTextWithRetry(wsServer, JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    sendTextWithRetry(
        wsServer,
        JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(textOutput, usage)));
    wsServer.close(1000, "bye");
    semaphore.acquire();
    serverListener.assertHalfDuplexRequest(param, StreamingMode.NONE.getValue());
    assertEquals(results.get(0).getOutput(), textOutput);
    assertEquals(results.get(0).getUsage(), usage);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testStreamingNoneText2Binary()
      throws ApiException, NoApiKeyException, IOException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeHalfDuplexApi<HalfDuplexTestParam> syncApi =
        new SynchronizeHalfDuplexApi<>(serviceOption);
    ;
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder()
            .parameter("hello", "world")
            .model("testModel")
            .prompt("prompt")
            .build();

    Semaphore semaphore = new Semaphore(0);
    List<DashScopeResult> results = new ArrayList<>();
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            results.add(msg);
            semaphore.release();
          }

          @Override
          public void onComplete() {}

          @Override
          public void onError(Exception e) {}
        });
    WebSocket wsServer = serverListener.assertOpen();
    sendTextWithRetry(wsServer, JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    sendBinaryWithRetry(wsServer, ByteString.of(binaryOutput));
    sendTextWithRetry(
        wsServer, JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(null, usage)));
    wsServer.close(1000, "bye");
    semaphore.acquire(2);
    serverListener.assertHalfDuplexRequest(param, StreamingMode.NONE.getValue());
    assertEquals((ByteBuffer) results.get(0).getOutput(), binaryOutput.position(0));
    assertEquals(results.get(1).getUsage(), usage);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testStreamingNoneBinary2Binary()
      throws ApiException, NoApiKeyException, IOException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeHalfDuplexApi<HalfDuplexBinaryParam> syncApi =
        new SynchronizeHalfDuplexApi<>(serviceOption);
    ;
    HalfDuplexBinaryParam param =
        HalfDuplexBinaryParam.builder()
            .parameter("hello", "world")
            .model("testModel")
            .data(binaryInput)
            .build();

    Semaphore semaphore = new Semaphore(0);
    List<DashScopeResult> results = new ArrayList<>();
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            results.add(msg);
            semaphore.release();
          }

          @Override
          public void onComplete() {}

          @Override
          public void onError(Exception e) {}
        });
    WebSocket wsServer = serverListener.assertOpen();
    sendTextWithRetry(wsServer, JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    sendBinaryWithRetry(wsServer, ByteString.of(binaryOutput));
    sendTextWithRetry(
        wsServer, JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(null, usage)));
    wsServer.close(1000, "bye");
    semaphore.acquire(2);
    serverListener.assertHalfDuplexRequest(param, StreamingMode.NONE.getValue());
    assertEquals((ByteBuffer) results.get(0).getOutput(), binaryOutput.position(0));
    assertEquals(results.get(1).getUsage(), usage);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testStreamingOutBinary2Mix()
      throws ApiException, NoApiKeyException, IOException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeHalfDuplexApi<HalfDuplexBinaryParam> syncApi =
        new SynchronizeHalfDuplexApi<>(serviceOption);
    ;
    HalfDuplexBinaryParam param =
        HalfDuplexBinaryParam.builder()
            .parameter("hello", "world")
            .model("testModel")
            .data(binaryInput)
            .build();

    Semaphore semaphore = new Semaphore(0);
    List<DashScopeResult> results = new ArrayList<>();
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            results.add(msg);
            semaphore.release();
          }

          @Override
          public void onComplete() {}

          @Override
          public void onError(Exception e) {}
        });
    WebSocket wsServer = serverListener.assertOpen();
    sendTextWithRetry(wsServer, JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    sendBinaryWithRetry(wsServer, ByteString.of(binaryOutput));
    sendTextWithRetry(
        wsServer,
        JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(textOutput, null)));
    sendTextWithRetry(
        wsServer, JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(null, usage)));
    wsServer.close(1000, "bye");
    semaphore.acquire(3);
    serverListener.assertHalfDuplexRequest(param, StreamingMode.NONE.getValue());
    assertEquals((ByteBuffer) results.get(0).getOutput(), binaryOutput.position(0));
    assertEquals(results.get(1).getOutput(), textOutput);
    assertEquals(results.get(2).getUsage(), usage);
  }
}
