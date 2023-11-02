// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import io.reactivex.functions.Action;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
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

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestGeneration {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  private static final MediaType MEDIA_TYPE_EVENT_STREAM = MediaType.parse("text/event-stream");
  MockWebServer server;
  private TestResponse rsp;
  private JsonObject output;
  private JsonObject usage;
  private String requestId;
  private String expectMessageBody =
      "{\"model\":\"qwen-turbo\",\"input\":{\"messages\":[{\"role\":\"user\",\"content\":\"如何做土豆炖猪脚?\"}]},\"parameters\":{\"top_p\":0.8,\"result_format\":\"message\"}}";
  private String expectTextBody =
      "{\"model\":\"qwen-turbo\",\"input\":{\"prompt\":\"如何做土豆炖猪脚?\"},\"parameters\":{\"top_p\":0.8}}";
  private String expectTextBodyOutputMessage =
      "{\"model\":\"qwen-turbo\",\"input\":{\"prompt\":\"如何做土豆炖猪脚?\"},\"parameters\":{\"top_p\":0.8,\"result_format\":\"message\",\"enable_search\":true}}";

  @BeforeEach
  public void before() {
    output = new JsonObject();
    output.addProperty("text", "材料：\n猪脚一只，葱，姜，蒜头适量，冰糖两颗，料酒三大勺，生抽四大勺，盐适量");
    output.addProperty("finish_reason", "stop");
    usage = new JsonObject();
    usage.addProperty("input_tokens", 16);
    usage.addProperty("output_tokens", 148);
    requestId = "682d7353-5100-9054-b44b-76f0cb045b37";
    rsp = TestResponse.builder().output(output).usage(usage).requestId(requestId).build();
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  private void checkResult(GenerationResult result, RecordedRequest request, String expectBody) {
    assertEquals(result.getRequestId(), requestId);
    assertEquals(result.getOutput().getText(), output.get("text").getAsString());
    assertEquals(result.getOutput().getFinishReason(), output.get("finish_reason").getAsString());
    assertEquals(
        result.getUsage().getInputTokens(), new Integer(usage.get("input_tokens").getAsInt()));
    assertEquals(
        result.getUsage().getOutputTokens(), new Integer(usage.get("output_tokens").getAsInt()));
    String body = request.getBody().readUtf8();
    assertEquals(request.getMethod(), "POST");
    assertEquals(request.getPath(), "/services/aigc/text-generation/generation");
    assertEquals(expectBody, body);
  }

  @Test
  public void testExtensionParameter() {
    QwenParam param =
        QwenParam.builder()
            .model("test")
            .resultFormat(QwenParam.ResultFormat.TEXT)
            .parameter("str", "world")
            .parameter("int", 100)
            .parameter("boolean", true)
            .parameter("float", 0.01)
            .build();
    Map<String, Object> p = param.getParameters();
    assertEquals(p.get("str"), "world");
    assertEquals(p.get("int"), 100);
    assertEquals(p.get("boolean"), true);
    assertEquals(p.get("float"), 0.01);
  }

  @Test
  public void testHttpTextCall()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    QwenParam param =
        QwenParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .resultFormat(QwenParam.ResultFormat.TEXT)
            .prompt("如何做土豆炖猪脚?")
            .topP(0.8)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Generation generation = new Generation();
    GenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, expectTextBody);
  }

  @Test
  public void testHttpCall()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    QwenParam param =
        QwenParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .resultFormat(QwenParam.ResultFormat.MESSAGE)
            .prompt("如何做土豆炖猪脚?")
            .topP(0.8)
            .enableSearch(true)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Generation generation = new Generation();
    GenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, expectTextBodyOutputMessage);
  }

  @Test
  public void testHttpCallWithCallBack()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    QwenParam param =
        QwenParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .prompt("如何做土豆炖猪脚?")
            .resultFormat(QwenParam.ResultFormat.MESSAGE)
            .topP(0.8)
            .enableSearch(true)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Generation generation = new Generation();
    Semaphore semaphore = new Semaphore(0);
    List<GenerationResult> results = new ArrayList<>();
    generation.call(
        param,
        new ResultCallback<GenerationResult>() {
          @Override
          public void onEvent(GenerationResult msg) {
            results.add(msg);
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
    RecordedRequest request = server.takeRequest();
    semaphore.acquire();
    checkResult(results.get(0), request, expectTextBodyOutputMessage);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  public void testHttpStream()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    server.enqueue(
        new MockResponse()
            .setBody("data: " + JsonUtils.toJson(rsp) + "\n\n")
            .setHeader("content-type", MEDIA_TYPE_EVENT_STREAM));
    int port = server.getPort();
    QwenParam param =
        QwenParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .prompt("如何做土豆炖猪脚?")
            .resultFormat(QwenParam.ResultFormat.MESSAGE)
            .topP(0.8)
            .enableSearch(true)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Generation generation = new Generation();
    Flowable<GenerationResult> flowable = generation.streamCall(param);
    GenerationResult result = flowable.blockingSingle();
    checkResult(result, server.takeRequest(), expectTextBodyOutputMessage);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  public void testHttpStreamWithCallBack()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    server.enqueue(
        new MockResponse()
            .setBody("data: " + JsonUtils.toJson(rsp) + "\n\n")
            .setHeader("content-type", MEDIA_TYPE_EVENT_STREAM));
    int port = server.getPort();
    QwenParam param =
        QwenParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .resultFormat(QwenParam.ResultFormat.MESSAGE)
            .prompt("如何做土豆炖猪脚?")
            .topP(0.8)
            .enableSearch(true)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Generation generation = new Generation();
    Semaphore semaphore = new Semaphore(0);
    List<GenerationResult> results = new ArrayList<>();
    generation.streamCall(
        param,
        new ResultCallback<GenerationResult>() {
          @Override
          public void onEvent(GenerationResult msg) {
            results.add(msg);
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
    checkResult(results.get(0), server.takeRequest(), expectTextBodyOutputMessage);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testWebSocketStream()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    WebSocketRecorder serverListener = new WebSocketRecorder("server");
    server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
    int port = server.getPort();
    QwenParam param =
        QwenParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .resultFormat(QwenParam.ResultFormat.MESSAGE)
            .prompt("如何做土豆炖猪脚?")
            .topP(0.8)
            .build();
    Constants.baseWebsocketApiUrl = String.format("ws://127.0.0.1:%s/api-ws/v1/inference/", port);
    Generation generation = new Generation(Protocol.WEBSOCKET.getValue());
    Flowable<GenerationResult> flowable = generation.streamCall(param);
    List<GenerationResult> results = new ArrayList<>();
    Semaphore semaphore = new Semaphore(0);
    flowable
        .doOnComplete(
            new Action() {
              @Override
              public void run() throws Exception {
                semaphore.release();
              }
            })
        .forEach(
            msg -> {
              results.add(msg);
            });
    WebSocket wsServer = serverListener.assertOpen();
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(output, usage)));
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(output, usage)));
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(output, usage)));
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskFinishedMessage(output, usage)));
    wsServer.close(1000, "bye");
    semaphore.acquire();
    serverListener.assertHalfDuplexRequest(param, StreamingMode.OUT.getValue());
    System.out.println(JsonUtils.toJson(results.get(0)));
    assertTrue(results.size() == 4);
    assertEquals(results.get(0).getOutput().getText(), output.get("text").getAsString());
    assertEquals(
        results.get(0).getUsage().getInputTokens(), (Integer) usage.get("input_tokens").getAsInt());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testWebSocketStreamWithCallBack()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    WebSocketRecorder serverListener = new WebSocketRecorder("server");
    server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
    int port = server.getPort();
    QwenParam param =
        QwenParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .resultFormat(QwenParam.ResultFormat.MESSAGE)
            .prompt("如何做土豆炖猪脚?")
            .topP(0.8)
            .build();
    Constants.baseWebsocketApiUrl = String.format("ws://127.0.0.1:%s/api-ws/v1/inference/", port);
    Generation generation = new Generation(Protocol.WEBSOCKET.getValue());
    List<GenerationResult> results = new ArrayList<>();
    Semaphore semaphore = new Semaphore(0);
    generation.streamCall(
        param,
        new ResultCallback<GenerationResult>() {
          @Override
          public void onEvent(GenerationResult msg) {
            results.add(msg);
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
    WebSocket wsServer = serverListener.assertOpen();
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskStartMessage()));
    wsServer.send(JsonUtils.toJson(WebSocketServerMessage.getTaskGeneratedMessage(output, usage)));
    wsServer.close(1000, "bye");
    semaphore.acquire();
    serverListener.assertHalfDuplexRequest(param, StreamingMode.OUT.getValue());
    assertEquals(results.get(0).getOutput().getText(), output.get("text").getAsString());
    assertEquals(
        results.get(0).getUsage().getInputTokens(), (Integer) usage.get("input_tokens").getAsInt());
  }
}
