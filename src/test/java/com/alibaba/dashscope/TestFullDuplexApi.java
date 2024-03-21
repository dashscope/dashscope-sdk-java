// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;

import com.alibaba.dashscope.api.SynchronizeFullDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.protocol.WebSocketResponse;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
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
public class TestFullDuplexApi {
  private ApiServiceOption serviceOption =
      ApiServiceOption.builder()
          .protocol(Protocol.WEBSOCKET)
          .streamingMode(StreamingMode.DUPLEX)
          .outputMode(OutputMode.ACCUMULATE)
          .taskGroup("group")
          .task("task")
          .function("function")
          .build();
  MockWebServer server;
  WebSocketRecorder serverListener;
  int msgIndex = 0;

  @BeforeEach
  public void before() {
    server = new MockWebServer();
    serverListener = new WebSocketRecorder("server");
    server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
    int port = this.server.getPort();
    Constants.baseWebsocketApiUrl = String.format("ws://127.0.0.1:%s/api-ws/v1/inference/", port);
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
    server.shutdown();
  }

  private FullDuplexTestParam getStreamTextParam() {
    String[] textData = new String[10];
    for (int i = 0; i < 10; ++i) {
      textData[i] = UUID.randomUUID().toString();
    }
    FullDuplexTestParam param =
        FullDuplexTestParam.builder()
            .model("duplexModel")
            .streamData(Flowable.<Object>fromArray(textData))
            .topP(1.0)
            .str(UUID.randomUUID().toString())
            .build();
    return param;
  }

  private FullDuplexTestParam getStreamTextCustomHeadersParam(Map<String, String> headers) {
    String[] textData = new String[10];
    for (int i = 0; i < 10; ++i) {
      textData[i] = UUID.randomUUID().toString();
    }
    FullDuplexTestParam param =
        FullDuplexTestParam.builder()
            .model("duplexModel")
            .streamData(Flowable.<Object>fromArray(textData))
            .topP(1.0)
            .str(UUID.randomUUID().toString())
            .headers(headers)
            .build();
    return param;
  }

  private FullDuplexTestParam getStreamTextParamWithResources() {
    String[] textData = new String[10];
    for (int i = 0; i < 10; ++i) {
      textData[i] = UUID.randomUUID().toString();
    }
    JsonObject resources = new JsonObject();
    resources.addProperty("str", "String");
    resources.addProperty("num", 100);
    FullDuplexTestParam param =
        FullDuplexTestParam.builder()
            .model("duplexModel")
            .streamData(Flowable.<Object>fromArray(textData))
            .resources(resources)
            .topP(1.0)
            .str(UUID.randomUUID().toString())
            .build();
    return param;
  }

  private FullDuplexTestParam getStreamBinaryParam() {
    ByteBuffer[] binaryData = new ByteBuffer[10];
    for (int i = 0; i < 10; ++i) {
      binaryData[i] = ByteBuffer.wrap(UUID.randomUUID().toString().getBytes());
    }
    FullDuplexTestParam param =
        FullDuplexTestParam.builder()
            .model("duplexModel")
            .streamData(Flowable.<Object>fromArray(binaryData))
            .topP(1.0)
            .str(UUID.randomUUID().toString())
            .build();
    return param;
  }

  private FullDuplexTestParam getStreamMixParam() {
    Flowable<Object> streamData = Flowable.<Object>fromIterable(generateMixData(10, 2048));
    FullDuplexTestParam param =
        FullDuplexTestParam.builder()
            .model("duplexModel")
            .streamData(streamData)
            .topP(1.0)
            .str(UUID.randomUUID().toString())
            .build();
    return param;
  }
  /**
   * Get server output data
   *
   * @param n number of binary data
   * @param maxLength the data maxlength.
   * @return
   */
  private List<byte[]> generateBinaryData(int n, int maxLength) {
    List<byte[]> outputs = new ArrayList<>(n);
    for (int i = 0; i < n; ++i) {
      int length = ThreadLocalRandom.current().nextInt(1, maxLength);
      byte[] b = new byte[length];
      new Random().nextBytes(b);
      outputs.add(b);
    }
    return outputs;
  }

  private List<Object> generateMixData(int n, int maxLength) {
    List<Object> outputs = new ArrayList<>(n);
    for (int i = 0; i < n; ++i) {
      int length = ThreadLocalRandom.current().nextInt(1, maxLength);
      int typeSelection = ThreadLocalRandom.current().nextInt(1, 3);
      if (typeSelection == 1) {
        byte[] b = new byte[length];
        new Random().nextBytes(b);
        outputs.add(b);
      } else {
        String randomStr = UUID.randomUUID().toString();
        while (randomStr.length() < length) {
          randomStr += UUID.randomUUID().toString();
        }
        randomStr = randomStr.substring(0, length);
        outputs.add(randomStr);
      }
    }
    return outputs;
  }

  private void verifyBinaryOutput(
      Flowable<DashScopeResult> results, List<Object> outputs, Semaphore receiveCompleted) {
    results
        .doOnError(
            err -> {
              log.error(err.getMessage());
              receiveCompleted.release();
            })
        .doOnComplete(
            new Action() {

              @Override
              public void run() throws Exception {
                receiveCompleted.release();
              }
            })
        .forEach(
            msg -> {
              log.info(String.format("Message index: %d", msgIndex));
              if (msg.getOutput() instanceof ByteBuffer) {
                assertEquals(msg.getOutput(), ByteBuffer.wrap((byte[]) outputs.get(msgIndex++)));
              } else {
                assertEquals(
                    ((JsonElement) msg.getOutput()).getAsString(),
                    (String) outputs.get(msgIndex++));
              }
            });
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testCustomHeaders() throws ApiException, NoApiKeyException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeFullDuplexApi<FullDuplexTestParam> api =
        new SynchronizeFullDuplexApi<>(serviceOption);
    Map<String, String> headers =
        new HashMap<String, String>() {
          {
            put("h1", "1");
            put("h2", "v");
          }
        };
    FullDuplexTestParam param = getStreamTextCustomHeadersParam(headers);
    Flowable<DashScopeResult> results = api.duplexCall(param);
    Semaphore receiveCompleted = new Semaphore(0);
    List<byte[]> outputs = generateBinaryData(3, 1024);
    verifyBinaryOutput(results, (List<Object>) (List<?>) outputs, receiveCompleted);
    WebSocket wsServer = serverListener.assertOpen();
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    for (byte[] b : outputs) {
      wsServer.send(ByteString.of(b));
    }
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskFinishedMessage(null, null)));

    receiveCompleted.acquire();
    serverListener.assertFullDuplexRequest(param, StreamingMode.DUPLEX.getValue());
    serverListener.assertHeaders(headers);
    wsServer.close(1000, "bye");
    Observable.interval(100, TimeUnit.MILLISECONDS).take(1).blockingSingle();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testText2Binary() throws ApiException, NoApiKeyException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeFullDuplexApi<FullDuplexTestParam> api =
        new SynchronizeFullDuplexApi<>(serviceOption);
    FullDuplexTestParam param = getStreamTextParam();
    Flowable<DashScopeResult> results = api.duplexCall(param);
    Semaphore receiveCompleted = new Semaphore(0);
    List<byte[]> outputs = generateBinaryData(3, 1024);
    verifyBinaryOutput(results, (List<Object>) (List<?>) outputs, receiveCompleted);
    WebSocket wsServer = serverListener.assertOpen();
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    for (byte[] b : outputs) {
      wsServer.send(ByteString.of(b));
    }
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskFinishedMessage(null, null)));

    receiveCompleted.acquire();
    serverListener.assertFullDuplexRequest(param, StreamingMode.DUPLEX.getValue());
    wsServer.close(1000, "bye");
    Observable.interval(100, TimeUnit.MILLISECONDS).take(1).blockingSingle();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testText2BinaryWithResources()
      throws ApiException, NoApiKeyException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeFullDuplexApi<FullDuplexTestParam> api =
        new SynchronizeFullDuplexApi<>(serviceOption);
    FullDuplexTestParam param = getStreamTextParamWithResources();
    Flowable<DashScopeResult> results = api.duplexCall(param);
    Semaphore receiveCompleted = new Semaphore(0);
    List<byte[]> outputs = generateBinaryData(3, 1024);
    verifyBinaryOutput(results, (List<Object>) (List<?>) outputs, receiveCompleted);
    WebSocket wsServer = serverListener.assertOpen();
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    for (byte[] b : outputs) {
      wsServer.send(ByteString.of(b));
    }
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskFinishedMessage(null, null)));

    receiveCompleted.acquire();
    serverListener.assertFullDuplexRequestWithResources(
        param, StreamingMode.DUPLEX.getValue(), (JsonObject) param.getResources());
    wsServer.close(1000, "bye");
    Observable.interval(100, TimeUnit.MILLISECONDS).take(1).blockingSingle();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testBinary2Binary() throws ApiException, NoApiKeyException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeFullDuplexApi<FullDuplexTestParam> api =
        new SynchronizeFullDuplexApi<>(serviceOption);
    FullDuplexTestParam param = getStreamBinaryParam();
    Flowable<DashScopeResult> results = api.duplexCall(param);
    Semaphore receiveCompleted = new Semaphore(0);
    List<byte[]> outputs = generateBinaryData(10, 1024);
    verifyBinaryOutput(results, (List<Object>) (List<?>) outputs, receiveCompleted);
    WebSocket wsServer = serverListener.assertOpen();
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    for (byte[] b : outputs) {
      wsServer.send(ByteString.of(b));
    }
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskFinishedMessage(null, null)));
    receiveCompleted.acquire();
    serverListener.assertFullDuplexRequest(param, StreamingMode.DUPLEX.getValue());
    wsServer.close(1000, "bye");
    Observable.interval(100, TimeUnit.MILLISECONDS).take(1).blockingSingle();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testBinary2BinaryCallback()
      throws ApiException, NoApiKeyException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeFullDuplexApi<FullDuplexTestParam> api =
        new SynchronizeFullDuplexApi<>(serviceOption);
    FullDuplexTestParam param = getStreamBinaryParam();
    List<DashScopeResult> results = new ArrayList<>();
    Semaphore receiveCompleted = new Semaphore(0);
    List<byte[]> outputs = generateBinaryData(5, 1024);
    api.duplexCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            results.add(message);
          }

          @Override
          public void onComplete() {
            receiveCompleted.release();
          }

          @Override
          public void onError(Exception ex) {
            receiveCompleted.release();
          }
        });
    WebSocket wsServer = serverListener.assertOpen();
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    for (byte[] b : outputs) {
      wsServer.send(ByteString.of(b));
    }
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskFinishedMessage(null, null)));
    receiveCompleted.acquire();
    Flowable.<DashScopeResult>fromIterable(results)
        .forEach(
            msg -> {
              if (msg.getOutput() instanceof ByteBuffer) {
                assertEquals(msg.getOutput(), ByteBuffer.wrap(outputs.get(msgIndex++)));
              } else {
                log.info(msg.toString());
              }
            });
    serverListener.assertFullDuplexRequest(param, StreamingMode.DUPLEX.getValue());
    wsServer.close(1000, "bye");
    Observable.interval(100, TimeUnit.MILLISECONDS).take(1).blockingSingle();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testMixType2MixType() throws ApiException, NoApiKeyException, InterruptedException {
    serviceOption.setProtocol(Protocol.WEBSOCKET);
    SynchronizeFullDuplexApi<FullDuplexTestParam> api =
        new SynchronizeFullDuplexApi<>(serviceOption);
    FullDuplexTestParam param = getStreamMixParam();
    Flowable<DashScopeResult> results = api.duplexCall(param);
    Semaphore receiveCompleted = new Semaphore(0);
    List<Object> outputs = generateMixData(100, 1024);
    verifyBinaryOutput(results, outputs, receiveCompleted);
    WebSocket wsServer = serverListener.assertOpen();
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    for (Object data : outputs) {
      if (data instanceof byte[]) {
        wsServer.send(ByteString.of((byte[]) data));
      } else {
        WebSocketResponse rsp =
            WebSocketServerMessage.getTaskGeneratedMessage(
                JsonUtils.toJsonElement((String) data), null);
        wsServer.send(JsonUtils.toJson(rsp));
      }
    }
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskFinishedMessage(null, null)));
    receiveCompleted.acquire();
    serverListener.assertFullDuplexRequest(param, StreamingMode.DUPLEX.getValue());
    wsServer.close(1000, "bye");
    Observable.interval(100, TimeUnit.MILLISECONDS).take(1).blockingSingle();
  }
}
