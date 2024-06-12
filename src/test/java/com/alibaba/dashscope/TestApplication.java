// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import com.alibaba.dashscope.app.*;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Title App Completion call test cases.<br>
 * Description App Completion call test cases.<br>
 * Created at 2024-02-26 09:50
 *
 * @since jdk8
 */
@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestApplication {
  private static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; charset=utf-8";
  private static final String MEDIA_TYPE_EVENT_STREAM = "text/event-stream";

  MockWebServer server;

  @BeforeEach
  public void setup() {
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  /**
   * @see Application#call(ApplicationParam)
   *     <p>Rag application call test case
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  @Test
  public void testRagCall()
      throws ApiException, NoApiKeyException, InputRequiredException, InterruptedException {
    ApplicationResult testResult = buildRagResult();

    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(testResult))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();

    String appId = "1234";
    RagApplicationParam param =
        RagApplicationParam.builder()
            .workspace("ws_1234")
            .appId(appId)
            .prompt("API接口说明中, TopP参数改如何传递?")
            .topP(0.2)
            .temperature(1.0f)
            .docTagCodes(Arrays.asList("t1234", "t5678"))
            .docReferenceType(RagApplicationParam.DocReferenceType.SIMPLE)
            .hasThoughts(true)
            .build();

    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Application application = new Application();
    ApplicationResult result = application.call(param);

    checkResult(server.takeRequest(), appId, result, testResult);
  }

  /**
   * @see Application#call(ApplicationParam)
   *     <p>Plugin and flow applicaiton call test case
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  @Test
  public void testFlowCall()
      throws ApiException, NoApiKeyException, InputRequiredException, InterruptedException {
    ApplicationResult testResult = new ApplicationResult();
    testResult.setRequestId(UUID.randomUUID().toString());

    ApplicationOutput output =
        ApplicationOutput.builder()
            .text("当月的居民用电量为102千瓦。")
            .finishReason("stop")
            .thoughts(
                Collections.singletonList(
                    ApplicationOutput.Thought.builder()
                        .thought("开启了插件增强")
                        .actionType("api")
                        .actionName("plugin")
                        .action("api")
                        .actionInputStream(
                            "{\"userId\": \"123\", \"date\": \"202402\", \"city\": \"hangzhou\"}")
                        .actionInput(
                            JsonUtils.parse(
                                "{\"userId\": \"123\", \"date\": \"202402\", \"city\": \"hangzhou\"}"))
                        .observation(
                            "{\"quantity\": 102, \"type\": \"resident\", \"date\": \"202402\", \"unit\": \"千瓦\"}")
                        .response("当月的居民用电量为102千瓦。")
                        .build()))
            .build();
    testResult.setOutput(output);

    ApplicationUsage usage =
        ApplicationUsage.builder()
            .models(
                Collections.singletonList(
                    ApplicationUsage.ModelUsage.builder()
                        .modelId("123")
                        .inputTokens(50)
                        .outputTokens(20)
                        .build()))
            .build();
    testResult.setUsage(usage);

    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(testResult))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();

    String appId = "1234";
    String flowParams = "{\"userId\": \"123\"}";
    ApplicationParam param =
        ApplicationParam.builder()
            .workspace("ws_1234")
            .appId(appId)
            .prompt("本月的用电量是多少?")
            .bizParams(JsonUtils.parse(flowParams))
            .hasThoughts(true)
            .topP(0.2)
            .build();

    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Application application = new Application();
    ApplicationResult result = application.call(param);

    checkResult(server.takeRequest(), appId, result, testResult);
  }

  /**
   * @see Application#streamCall(ApplicationParam)
   *     <p>Application call with stream response (Http SSE) test case
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  @Test
  public void testStreamCall()
      throws NoApiKeyException, InputRequiredException, InterruptedException {
    ApplicationResult testResult = buildRagResult();

    server.enqueue(
        new MockResponse()
            .setBody("data: " + JsonUtils.toJson(testResult) + "\n\n")
            .setHeader("content-type", MEDIA_TYPE_EVENT_STREAM));
    int port = server.getPort();

    String appId = "2345";
    RagApplicationParam param =
        RagApplicationParam.builder()
            .workspace("ws_1234")
            .appId(appId)
            .prompt("API接口说明中, TopP参数改如何传递?")
            .topP(0.2)
            .temperature(1.0f)
            .docTagCodes(Arrays.asList("t1234", "t5678"))
            .docReferenceType(RagApplicationParam.DocReferenceType.SIMPLE)
            .hasThoughts(true)
            .build();

    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Application application = new Application();
    Flowable<ApplicationResult> flowable = application.streamCall(param);
    ApplicationResult result = flowable.blockingSingle();

    checkResult(server.takeRequest(), appId, result, testResult);
  }

  /**
   * @see Application#call(ApplicationParam)
   *     <p>Application call with error test case
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  @Test
  public void testCallWithError()
      throws ApiException, NoApiKeyException, InputRequiredException, InterruptedException {
    JsonObject testResult = new JsonObject();

    int responseCode = 400;
    String requestId = UUID.randomUUID().toString();
    String code = "App.InvalidAppId";
    String message = "App id is invalid.";

    testResult.addProperty(ApiKeywords.REQUEST_ID, requestId);
    testResult.addProperty(ApiKeywords.CODE, code);
    testResult.addProperty(ApiKeywords.MESSAGE, message);

    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(testResult))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON)
            .setResponseCode(responseCode));
    int port = server.getPort();

    String appId = "1234";
    RagApplicationParam param =
        RagApplicationParam.builder()
            .workspace("ws_1234")
            .appId(appId)
            .prompt("API接口说明中, TopP参数改如何传递?")
            .topP(0.2)
            .temperature(1.0f)
            .docTagCodes(Arrays.asList("t1234", "t5678"))
            .docReferenceType(RagApplicationParam.DocReferenceType.SIMPLE)
            .hasThoughts(true)
            .build();

    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Application application = new Application();

    try {
      ApplicationResult result = application.call(param);
    } catch (ApiException e) {
      Status status = e.getStatus();
      Assertions.assertEquals(status.getStatusCode(), responseCode);
      Assertions.assertEquals(status.getRequestId(), requestId);
      Assertions.assertEquals(status.getCode(), code);
      Assertions.assertEquals(status.getMessage(), message);
    }

    RecordedRequest request = server.takeRequest();
    Assertions.assertEquals(request.getMethod(), "POST");
    Assertions.assertEquals(request.getPath(), "/apps/" + appId + "/completion");
  }

  private ApplicationResult buildRagResult() {
    ApplicationResult result = new ApplicationResult();

    result.setRequestId(UUID.randomUUID().toString());

    List<String> images = new ArrayList<>();
    images.add("http://127.0.0.1:8080/qqq.png");
    images.add("http://127.0.0.1:8080/www.png");
    images.add("http://127.0.0.1:8080/eee.png");

    ApplicationOutput output =
        ApplicationOutput.builder()
            .text("API接口说明中，通过parameters的topP属性设置，取值范围在(0,1.0)。")
            .finishReason("stop")
            .sessionId(UUID.randomUUID().toString())
            .docReferences(
                Collections.singletonList(
                    ApplicationOutput.DocReference.builder()
                        .indexId("1")
                        .docId("1234")
                        .docName("API接口说明.pdf")
                        .docUrl("https://127.0.0.1/dl/API接口说明.pdf")
                        .title("API接口说明")
                        .text("topP取值范围在(0,1.0),取值越大,生成的随机性越高")
                        .bizId("2345")
                        .images(images)
                        .build()))
            .thoughts(
                Collections.singletonList(
                    ApplicationOutput.Thought.builder()
                        .thought("开启了文档增强，优先检索文档内容")
                        .actionType("api")
                        .actionName("文档检索")
                        .action("searchDocument")
                        .actionInputStream("{\"query\":\"API接口说明中, TopP参数改如何传递?\"}")
                        .actionInput(JsonUtils.parse("{\"query\":\"API接口说明中, TopP参数改如何传递?\"}"))
                        .observation(
                            "{\n"
                                + "  \"data\": [\n"
                                + "    {\n"
                                + "      \"docId\": \"1234\",\n"
                                + "      \"docName\": \"API接口说明\",\n"
                                + "      \"docUrl\": \"https://127.0.0.1/dl/API接口说明.pdf\",\n"
                                + "      \"indexId\": \"1\",\n"
                                + "      \"score\": 0.11992252,\n"
                                + "      \"text\": \"填(0,1.0),取值越大,生成的随机性越高;启用文档检索后,文档引用类型,取值包括:simple|indexed。\",\n"
                                + "      \"title\": \"API接口说明\",\n"
                                + "      \"titlePath\": \"API接口说明>>>接口说明>>>是否必   说明>>>填\"\n"
                                + "    }\n"
                                + "  ],\n"
                                + "  \"status\": \"SUCCESS\"\n"
                                + "}")
                        .response(
                            "API接口说明中, TopP参数是一个float类型的参数,取值范围为0到1.0,默认为1.0。取值越大,生成的随机性越高。[5]")
                        .build()))
            .build();
    result.setOutput(output);

    ApplicationUsage usage =
        ApplicationUsage.builder()
            .models(
                Collections.singletonList(
                    ApplicationUsage.ModelUsage.builder()
                        .modelId("123")
                        .inputTokens(10)
                        .outputTokens(30)
                        .build()))
            .build();
    result.setUsage(usage);

    return result;
  }

  private void checkResult(
      RecordedRequest request,
      String appId,
      ApplicationResult result,
      ApplicationResult testResult) {
    Assertions.assertEquals(result.getRequestId(), testResult.getRequestId());

    // output
    Assertions.assertNotNull(result.getOutput());
    Assertions.assertEquals(result.getOutput().getText(), testResult.getOutput().getText());
    Assertions.assertEquals(
        result.getOutput().getFinishReason(), testResult.getOutput().getFinishReason());
    Assertions.assertEquals(
        result.getOutput().getSessionId(), testResult.getOutput().getSessionId());

    // usage
    Assertions.assertTrue(
        result.getUsage().getModels() != null && result.getUsage().getModels().size() > 0);
    ApplicationUsage.ModelUsage modelUsage = result.getUsage().getModels().get(0);
    ApplicationUsage.ModelUsage expectedModelUsage = testResult.getUsage().getModels().get(0);
    Assertions.assertEquals(modelUsage.getModelId(), expectedModelUsage.getModelId());
    Assertions.assertEquals(modelUsage.getInputTokens(), expectedModelUsage.getInputTokens());
    Assertions.assertEquals(modelUsage.getOutputTokens(), expectedModelUsage.getOutputTokens());

    // doc reference
    if (testResult.getOutput().getDocReferences() != null
        && testResult.getOutput().getDocReferences().size() > 0) {
      Assertions.assertNotNull(result.getOutput().getDocReferences());
      Assertions.assertEquals(
          result.getOutput().getDocReferences().size(),
          testResult.getOutput().getDocReferences().size());

      for (int i = 0; i < result.getOutput().getDocReferences().size(); i++) {
        ApplicationOutput.DocReference docReference = result.getOutput().getDocReferences().get(i);
        ApplicationOutput.DocReference expectedDocReference =
            testResult.getOutput().getDocReferences().get(i);

        Assertions.assertEquals(docReference.getIndexId(), expectedDocReference.getIndexId());
        Assertions.assertEquals(docReference.getDocId(), expectedDocReference.getDocId());
        Assertions.assertEquals(docReference.getDocName(), expectedDocReference.getDocName());
        Assertions.assertEquals(docReference.getDocUrl(), expectedDocReference.getDocUrl());
        Assertions.assertEquals(docReference.getTitle(), expectedDocReference.getTitle());
        Assertions.assertEquals(docReference.getText(), expectedDocReference.getText());
        Assertions.assertEquals(docReference.getBizId(), expectedDocReference.getBizId());
        Assertions.assertEquals(
            JsonUtils.toJson(docReference.getImages()),
            JsonUtils.toJson(expectedDocReference.getImages()));
      }
    }

    // thoughts
    if (testResult.getOutput().getThoughts() != null
        && testResult.getOutput().getThoughts().size() > 0) {
      Assertions.assertNotNull(result.getOutput().getThoughts());
      Assertions.assertEquals(
          result.getOutput().getThoughts().size(), testResult.getOutput().getThoughts().size());

      for (int i = 0; i < result.getOutput().getThoughts().size(); i++) {
        ApplicationOutput.Thought thought = result.getOutput().getThoughts().get(0);
        ApplicationOutput.Thought expectedThought = testResult.getOutput().getThoughts().get(0);

        Assertions.assertEquals(thought.getThought(), expectedThought.getThought());
        Assertions.assertEquals(thought.getAction(), expectedThought.getAction());
        Assertions.assertEquals(thought.getActionType(), expectedThought.getActionType());
        Assertions.assertEquals(thought.getActionName(), expectedThought.getActionName());
        Assertions.assertEquals(
            JsonUtils.toJson(thought.getActionInput()),
            JsonUtils.toJson(expectedThought.getActionInput()));
        Assertions.assertEquals(
            thought.getActionInputStream(), expectedThought.getActionInputStream());
        Assertions.assertEquals(thought.getObservation(), expectedThought.getObservation());
        Assertions.assertEquals(thought.getResponse(), expectedThought.getResponse());
      }
    }

    // method and url
    Assertions.assertEquals(request.getMethod(), "POST");
    Assertions.assertEquals(request.getPath(), "/apps/" + appId + "/completion");
  }
}
