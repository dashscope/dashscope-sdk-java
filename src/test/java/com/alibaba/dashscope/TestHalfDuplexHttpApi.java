// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestHalfDuplexHttpApi {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  private static final MediaType MEDIA_TYPE_EVENT_STREAM = MediaType.parse("text/event-stream");
  private SynchronizeHalfDuplexApi<HalfDuplexTestParam> syncApi;
  private ApiServiceOption serviceOption;

  @BeforeEach
  public void before() {
    serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.OUT)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup("group")
            .task("task")
            .function("function")
            .build();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  @Test
  public void testHttpSendEmptyResponse() throws ApiException, NoApiKeyException, IOException {
    MockWebServer server = new MockWebServer();
    TestResponse rsp = TestResponse.builder().build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().model("qwen-turbo").parameter("k1", "v1").build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    DashScopeResult result = syncApi.call(param);
    assertEquals(JsonUtils.toJson(result), "{}");
    server.close();
  }

  @Test
  public void testHttpSendNoUsageResponse() throws ApiException, NoApiKeyException, IOException {
    MockWebServer server = new MockWebServer();
    JsonObject output = new JsonObject();
    output.addProperty("text", "hello");
    TestResponse rsp = TestResponse.builder().output(output).build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().model("qwen-turbo").parameter("k1", "v1").build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    DashScopeResult result = syncApi.call(param);
    assertEquals(output, result.getOutput());
    server.close();
  }

  @Test
  public void testHttpWithResources()
      throws ApiException, NoApiKeyException, IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    JsonObject output = new JsonObject();
    output.addProperty("text", "hello");
    TestResponse rsp = TestResponse.builder().output(output).build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    JsonObject resources = new JsonObject();
    resources.addProperty("str", "String");
    resources.addProperty("num", 100);
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder()
            .model("qwen-turbo")
            .parameter("k1", "v1")
            .resources(resources)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    DashScopeResult result = syncApi.call(param);
    assertEquals(output, result.getOutput());
    RecordedRequest request = server.takeRequest();
    String body = request.getBody().readUtf8();
    JsonObject req = JsonUtils.parse(body);
    assertEquals(resources, req.get(ApiKeywords.RESOURCES));
    server.close();
  }

  @Test
  public void testHttpWithCustomHeaders()
      throws ApiException, NoApiKeyException, IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    JsonObject output = new JsonObject();
    output.addProperty("text", "hello");
    TestResponse rsp = TestResponse.builder().output(output).build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    Map<String, Object> customHeaders =
        new HashMap<String, Object>() {
          {
            put("h1", "1");
            put("h2", 1000);
          }
        };
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder()
            .model("qwen-turbo")
            .parameter("k1", "v1")
            .headers(customHeaders)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    DashScopeResult result = syncApi.call(param);
    assertEquals(output, result.getOutput());
    RecordedRequest request = server.takeRequest();
    Map<String, List<String>> headers = request.getHeaders().toMultimap();
    for (Entry<String, Object> entry : customHeaders.entrySet()) {
      assertTrue(headers.containsKey(entry.getKey()));
      assertTrue(headers.get(entry.getKey()).get(0).equals(entry.getValue().toString()));
    }
    server.close();
  }

  @Test
  public void testHttpSendNoOutputResponse() throws ApiException, NoApiKeyException, IOException {
    MockWebServer server = new MockWebServer();
    JsonObject usage = new JsonObject();
    usage.addProperty("n", 1);
    TestResponse rsp = TestResponse.builder().usage(usage).build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().model("qwen-turbo").parameter("k1", "v1").build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    DashScopeResult result = syncApi.call(param);
    assertNull(result.getOutput());
    assertEquals(usage, result.getUsage());
    server.close();
  }

  @Test
  public void testHttpSendResponseError() throws ApiException, NoApiKeyException, IOException {
    MockWebServer server = new MockWebServer();
    String errorCode = "InvalidParameter";
    String errorMessage = "No model parameter!";
    TestResponse rsp = TestResponse.builder().code(errorCode).message(errorMessage).build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setResponseCode(400)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().model("qwen-turbo").parameter("k1", "v1").build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> {
              syncApi.call(param);
            });
    // TODO response with exception is uggly.
    assertTrue(exception.getStatus().getMessage().contains(errorMessage));
    assertEquals(exception.getStatus().getCode(), errorCode);
    server.close();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  public void testHttpSSE() throws ApiException, NoApiKeyException, IOException {
    MockWebServer server = new MockWebServer();
    String firstText = "Hello";
    // String secondText = "Hello world";
    JsonObject output = new JsonObject();
    output.addProperty("text", firstText);
    TestResponse rsp = TestResponse.builder().output(output).build();
    server.enqueue(
        new MockResponse()
            .setBody("data: " + JsonUtils.toJson(rsp) + "\n\n")
            .setHeader("content-type", MEDIA_TYPE_EVENT_STREAM));
    // output = new JsonObject();
    // output.addProperty("text", secondText);
    // rsp = TestResponse.builder().output(output).build();
    // server.enqueue(new MockResponse()
    //    .setBody("data: " + JsonUtils.toJson(rsp) + "\n\n").setHeader("content-type",
    // MEDIA_TYPE_EVENT_STREAM));
    int port = server.getPort();
    System.out.println(port);
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().model("qwen-turbo").parameter("k1", "v1").build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Flowable<DashScopeResult> result = syncApi.streamCall(param);
    List<String> outputTexts = new ArrayList<>();
    result.blockingForEach(
        msg -> {
          String text = ((JsonObject) (msg.getOutput())).get("text").getAsString();
          outputTexts.add(text);
        });
    assertEquals(firstText, outputTexts.get(0));
    server.close();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  public void testHttpSSEWithCallBack()
      throws ApiException, NoApiKeyException, IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    String firstText = "Hello";
    // String secondText = "Hello world";
    JsonObject output = new JsonObject();
    output.addProperty("text", firstText);
    TestResponse rsp = TestResponse.builder().output(output).build();
    server.enqueue(
        new MockResponse()
            .setBody("data: " + JsonUtils.toJson(rsp) + "\n\n")
            .setHeader("content-type", MEDIA_TYPE_EVENT_STREAM));
    // output = new JsonObject();
    // output.addProperty("text", secondText);
    // rsp = TestResponse.builder().output(output).build();
    // server.enqueue(new MockResponse()
    //    .setBody("data: " + JsonUtils.toJson(rsp) + "\n\n").setHeader("content-type",
    // MEDIA_TYPE_EVENT_STREAM));
    int port = server.getPort();
    System.out.println(port);
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().model("qwen-turbo").parameter("k1", "v1").build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    List<String> outputTexts = new ArrayList<>();
    Semaphore semaphore = new Semaphore(0);
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            String text = ((JsonObject) (msg.getOutput())).get("text").getAsString();
            outputTexts.add(text);
          }

          @Override
          public void onComplete() {
            semaphore.release();
          }

          @Override
          public void onError(Exception e) {
            semaphore.release();
          }
        });
    semaphore.acquire();
    assertEquals(firstText, outputTexts.get(0));
    server.close();
  }
}
