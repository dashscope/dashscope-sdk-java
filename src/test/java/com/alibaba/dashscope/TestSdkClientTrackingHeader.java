// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.AsyncTaskOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.task.AsyncTaskParam;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.WebSocket;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Verifies the tracking headers that actually reach the wire, which is what the gateway logs.
 * Expected shape: {@code x-dashscope-sdk-client: java-sdk/<version>/<module>} plus a session id;
 * nothing else.
 */
@Execution(ExecutionMode.SAME_THREAD)
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestSdkClientTrackingHeader {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  private static final String SDK_CLIENT = "x-dashscope-sdk-client";
  private static final String SDK_SESSION_ID = "x-dashscope-sdk-session-id";
  private static final String SDK_HEADER_PREFIX = "x-dashscope-sdk-";
  private static final String CLIENT_PREFIX = "java-sdk/" + Version.version;

  private MockWebServer server;
  private String originalHttpUrl;
  private String originalWebSocketUrl;

  @BeforeEach
  public void before() {
    server = new MockWebServer();
    originalHttpUrl = Constants.baseHttpApiUrl;
    originalWebSocketUrl = Constants.baseWebsocketApiUrl;
  }

  @AfterEach
  public void after() throws IOException {
    Constants.baseHttpApiUrl = originalHttpUrl;
    Constants.baseWebsocketApiUrl = originalWebSocketUrl;
    server.close();
  }

  private RecordedRequest takeRequest() throws InterruptedException {
    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
    assertNotNull(request, "no request reached the mock server");
    return request;
  }

  /** Asserts the full tracking header set every request must carry, then the module segment. */
  private void assertTrackingHeaders(RecordedRequest request, String expectedModule) {
    assertEquals(CLIENT_PREFIX + "/" + expectedModule, request.getHeader(SDK_CLIENT));
    assertNotNull(request.getHeader(SDK_SESSION_ID));
    for (String name : request.getHeaders().names()) {
      if (name.startsWith(SDK_HEADER_PREFIX)) {
        assertTrue(SDK_CLIENT.equals(name) || SDK_SESSION_ID.equals(name), "unexpected: " + name);
      }
    }
  }

  private void enqueueJsonResponse() {
    JsonObject output = new JsonObject();
    output.addProperty("text", "hi");
    output.addProperty("finish_reason", "stop");
    TestResponse rsp = TestResponse.builder().output(output).requestId("probe-request-id").build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
  }

  private QwenParam qwenParam() {
    return QwenParam.builder()
        .model(Generation.Models.QWEN_TURBO)
        .resultFormat(QwenParam.ResultFormat.TEXT)
        .prompt("hi")
        .build();
  }

  @Test
  public void testGenerationSendsTrackingHeadersOnTheWire() throws Exception {
    enqueueJsonResponse();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", server.getPort());

    new Generation().call(qwenParam());

    RecordedRequest request = takeRequest();
    assertEquals("/services/aigc/text-generation/generation", request.getPath());
    assertTrackingHeaders(request, "aigc");
  }

  @Test
  public void testAsyncTaskPollSendsTasksModuleOnTheWire() throws Exception {
    enqueueJsonResponse();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", server.getPort());
    AsyncTaskOption option =
        AsyncTaskOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.GET)
            .url("/tasks/task-id")
            .build();
    AsyncTaskParam param = AsyncTaskParam.builder().taskId("task-id").build();

    new SynchronizeHalfDuplexApi<AsyncTaskParam>(option).call(param);

    RecordedRequest request = takeRequest();
    assertEquals("/tasks/task-id", request.getPath());
    assertTrackingHeaders(request, "tasks");
  }

  @Test
  public void testWebSocketHandshakeSendsTrackingHeadersOnTheWire() throws Exception {
    WebSocketRecorder serverListener = new WebSocketRecorder("server");
    server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
    Constants.baseWebsocketApiUrl =
        String.format("ws://127.0.0.1:%s/api-ws/v1/inference/", server.getPort());
    ApiServiceOption option =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.NONE)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup("audio")
            .task("tts")
            .function("SpeechSynthesizer")
            .build();
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().model("testModel").prompt("prompt").build();

    new SynchronizeHalfDuplexApi<>(option)
        .call(
            param,
            new ResultCallback<DashScopeResult>() {
              @Override
              public void onEvent(DashScopeResult message) {}

              @Override
              public void onComplete() {}

              @Override
              public void onError(Exception e) {}
            });

    WebSocket wsServer = serverListener.assertOpen();
    RecordedRequest request = takeRequest();
    assertTrackingHeaders(request, "audio");
    wsServer.close(1000, "bye");
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_DISABLE_SDK_HEADERS", value = "1")
  public void testDisableEnvVarSuppressesHeadersOnTheWire() throws Exception {
    enqueueJsonResponse();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", server.getPort());

    new Generation().call(qwenParam());

    RecordedRequest request = takeRequest();
    assertNull(request.getHeader(SDK_CLIENT));
    assertNull(request.getHeader(SDK_SESSION_ID));
    assertNotNull(request.getHeader("Authorization"));
  }
}
