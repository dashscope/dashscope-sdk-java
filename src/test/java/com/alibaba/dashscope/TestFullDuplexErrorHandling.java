// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.api.SynchronizeFullDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestFullDuplexErrorHandling {
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
            .streamData(Flowable.<Object>fromArray(textData).observeOn(Schedulers.io()))
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

  private FullDuplexTestParam getStreamErrorData() {
    Flowable<Object> streamData =
        Flowable.<Object>create(
            emitter -> {
              emitter.onNext(UUID.randomUUID().toString());
              emitter.onError(new Exception("emitter on error"));
            },
            BackpressureStrategy.BUFFER);
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

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testAddressInvalid() throws ApiException, NoApiKeyException, InterruptedException {
    server = new MockWebServer();
    serverListener = new WebSocketRecorder("server");
    server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
    int port = this.server.getPort() + 1; // address is invalid
    Constants.baseWebsocketApiUrl = String.format("ws://127.0.0.1:%s/api-ws/v1/inference/", port);
    FullDuplexTestParam param = getStreamMixParam();
    ApiException thrown =
        assertThrows(
            ApiException.class,
            () -> {
              SynchronizeFullDuplexApi<FullDuplexTestParam> api =
                  new SynchronizeFullDuplexApi<>(serviceOption);
              Flowable<DashScopeResult> results = api.duplexCall(param);
              results.blockingForEach(
                  msg -> {
                    System.out.println(msg);
                  });
            });
    assertTrue(thrown.getMessage().contains("java.net.ConnectException"));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testErrorOnSendingStreamData()
      throws ApiException, NoApiKeyException, InterruptedException {
    server = new MockWebServer();
    serverListener = new WebSocketRecorder("server");
    server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
    int port = this.server.getPort();
    Constants.baseWebsocketApiUrl = String.format("ws://127.0.0.1:%s/api-ws/v1/inference/", port);
    FullDuplexTestParam param = getStreamErrorData();
    Exception thrown =
        assertThrows(
            Exception.class,
            () -> {
              SynchronizeFullDuplexApi<FullDuplexTestParam> api =
                  new SynchronizeFullDuplexApi<>(serviceOption);
              Flowable<DashScopeResult> results = api.duplexCall(param);
              results.blockingForEach(
                  msg -> {
                    System.out.println(msg);
                  });
            });
    log.error(thrown.getMessage());
    thrown.printStackTrace();
    assertTrue(thrown.getMessage().contains("emitter on error"));
  }

  private FullDuplexTestParam getStreamDataWithNullStreamData() {
    FullDuplexTestParam param =
        FullDuplexTestParam.builder()
            .model("duplexModel")
            .streamData(null)
            .topP(1.0)
            .str(UUID.randomUUID().toString())
            .build();
    return param;
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testSendingStreamDataNullFlowable()
      throws ApiException, NoApiKeyException, InterruptedException {
    server = new MockWebServer();
    serverListener = new WebSocketRecorder("server");
    server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
    int port = this.server.getPort();
    Constants.baseWebsocketApiUrl = String.format("ws://127.0.0.1:%s/api-ws/v1/inference/", port);
    FullDuplexTestParam param = getStreamDataWithNullStreamData();
    Exception thrown =
        assertThrows(
            Exception.class,
            () -> {
              SynchronizeFullDuplexApi<FullDuplexTestParam> api =
                  new SynchronizeFullDuplexApi<>(serviceOption);
              Flowable<DashScopeResult> results = api.duplexCall(param);
              results.blockingForEach(
                  msg -> {
                    System.out.println(msg);
                  });
            });
    log.error(thrown.getMessage());
    thrown.printStackTrace();
    assertTrue(thrown instanceof NullPointerException);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testErrorOnReceiveData()
      throws ApiException, NoApiKeyException, InterruptedException {
    server = new MockWebServer();
    serverListener = new WebSocketRecorder("server");
    server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
    int port = this.server.getPort();
    Constants.baseWebsocketApiUrl = String.format("ws://127.0.0.1:%s/api-ws/v1/inference/", port);
    FullDuplexTestParam param = getStreamTextParam();
    SynchronizeFullDuplexApi<FullDuplexTestParam> api =
        new SynchronizeFullDuplexApi<>(serviceOption);
    Flowable<DashScopeResult> results = api.duplexCall(param);
    Semaphore semaphore = new Semaphore(0);
    AtomicBoolean isError = new AtomicBoolean(false);
    results.subscribe(
        msg -> {
          System.out.println(msg);
        },
        err -> {
          System.out.println(err.getMessage());
          isError.set(true);
          semaphore.release();
        },
        new Action() {

          @Override
          public void run() throws Exception {
            semaphore.release();
          }
        });
    WebSocket wsServer = serverListener.assertOpen();
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    wsServer.send(UUID.randomUUID().toString());
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskFinishedMessage(null, null)));
    semaphore.acquire();
    assertTrue(isError.get());
  }
}
