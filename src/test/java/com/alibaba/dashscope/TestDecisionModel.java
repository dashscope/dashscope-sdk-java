// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.decision.DecisionAnswer;
import com.alibaba.dashscope.decision.DecisionModel;
import com.alibaba.dashscope.decision.DecisionModelParam;
import com.alibaba.dashscope.decision.DecisionModelResult;
import com.alibaba.dashscope.decision.DecisionQuestion;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

public class TestDecisionModel {

  /** The success response from the decision model api document. */
  private static final String DOC_RESPONSE_BODY =
      "{\n"
          + "  \"model\": \"decision-model-preview\",\n"
          + "  \"request_id\": \"7b986c65-b223-9341-b5f0-b988e27ecaac\",\n"
          + "  \"answers\": {\n"
          + "    \"department\": {\n"
          + "      \"type\": \"choice\",\n"
          + "      \"choice\": \"billing\",\n"
          + "      \"confidence\": 0.88,\n"
          + "      \"probabilities\": {\"billing\": 0.94, \"technical\": 0.06}\n"
          + "    },\n"
          + "    \"escalate\": {\"type\": \"noul\", \"noul\": 0.99},\n"
          + "    \"severity\": {\n"
          + "      \"type\": \"score\",\n"
          + "      \"score\": 2.25,\n"
          + "      \"confidence\": 0.91,\n"
          + "      \"legend\": {\"0\": \"轻微问题，不影响功能\", \"1\": \"部分功能受影响，但存在替代方案\",\n"
          + "                 \"2\": \"核心功能不可用，没有替代方案\", \"3\": \"造成严重业务或安全影响\"},\n"
          + "      \"probabilities\": {\"0\": 0, \"1\": 0.01, \"2\": 0.73, \"3\": 0.26}\n"
          + "    }\n"
          + "  },\n"
          + "  \"usage\": {\"input_tokens\": 125},\n"
          + "  \"latency_ms\": 52.9\n"
          + "}";

  private static DecisionModelParam buildDocParam() {
    Map<String, Object> state = new HashMap<>();
    state.put("ticket_id", "T-1001");
    state.put("content", "订单支付后超过 24 小时仍未到账，用户无法继续使用核心服务，要求立即处理。");

    Map<String, String> options = new LinkedHashMap<>();
    options.put("billing", "支付、退款和账单问题");
    options.put("technical", "产品故障和集成问题");

    List<String> levels =
        Arrays.asList("轻微问题，不影响功能", "部分功能受影响，但存在替代方案", "核心功能不可用，没有替代方案", "造成严重业务或安全影响");

    Map<String, DecisionQuestion> questions = new LinkedHashMap<>();
    questions.put("department", DecisionQuestion.choice("应该由哪个团队处理？", options));
    questions.put("escalate", DecisionQuestion.noul("是否需要立即通知值班人员？"));
    questions.put("severity", DecisionQuestion.score("这个问题有多严重？", levels));

    return DecisionModelParam.builder()
        .model(DecisionModelParam.Models.DECISION_MODEL_PREVIEW)
        .state(state)
        .questions(questions)
        .build();
  }

  @Test
  public void testHttpBody() {
    JsonObject body = buildDocParam().getHttpBody();
    assertEquals("decision-model-preview", body.get("model").getAsString());
    assertEquals("T-1001", body.getAsJsonObject("state").get("ticket_id").getAsString());

    JsonObject questions = body.getAsJsonObject("questions");
    assertEquals(3, questions.size());

    JsonObject department = questions.getAsJsonObject("department");
    assertEquals("choice", department.get("type").getAsString());
    assertEquals("应该由哪个团队处理？", department.get("instructions").getAsString());
    assertEquals("支付、退款和账单问题", department.getAsJsonObject("criteria").get("billing").getAsString());

    JsonObject escalate = questions.getAsJsonObject("escalate");
    assertEquals("noul", escalate.get("type").getAsString());
    assertTrue(!escalate.has("criteria"), "noul question must not carry criteria");

    JsonObject severity = questions.getAsJsonObject("severity");
    assertEquals("score", severity.get("type").getAsString());
    assertEquals(4, severity.getAsJsonArray("criteria").size());
    assertEquals("轻微问题，不影响功能", severity.getAsJsonArray("criteria").get(0).getAsString());
  }

  @Test
  public void testValidate() throws InputRequiredException {
    Map<String, DecisionQuestion> oneQuestion = new HashMap<>();
    oneQuestion.put("q1", DecisionQuestion.noul("是否升级？"));

    // model/state/questions are enforced at build time by lombok @NonNull.
    assertThrows(
        NullPointerException.class,
        () -> DecisionModelParam.builder().state(new HashMap<>()).questions(oneQuestion).build());
    assertThrows(
        NullPointerException.class,
        () ->
            DecisionModelParam.builder()
                .model(DecisionModelParam.Models.DECISION_MODEL_PREVIEW)
                .questions(oneQuestion)
                .build());
    assertThrows(
        NullPointerException.class,
        () ->
            DecisionModelParam.builder()
                .model(DecisionModelParam.Models.DECISION_MODEL_PREVIEW)
                .state(new HashMap<>())
                .build());

    // empty questions passes the builder but fails validate().
    DecisionModelParam noQuestions =
        DecisionModelParam.builder()
            .model(DecisionModelParam.Models.DECISION_MODEL_PREVIEW)
            .state(new HashMap<>())
            .questions(new HashMap<>())
            .build();
    assertThrows(InputRequiredException.class, noQuestions::validate);

    Map<String, DecisionQuestion> badType = new HashMap<>();
    badType.put("q1", DecisionQuestion.builder().type("bad").instructions("test").build());
    DecisionModelParam badTypeParam =
        DecisionModelParam.builder()
            .model(DecisionModelParam.Models.DECISION_MODEL_PREVIEW)
            .state(new HashMap<>())
            .questions(badType)
            .build();
    assertThrows(InputRequiredException.class, badTypeParam::validate);

    Map<String, DecisionQuestion> noCriteria = new HashMap<>();
    noCriteria.put("q1", DecisionQuestion.builder().type("choice").instructions("test").build());
    DecisionModelParam noCriteriaParam =
        DecisionModelParam.builder()
            .model(DecisionModelParam.Models.DECISION_MODEL_PREVIEW)
            .state(new HashMap<>())
            .questions(noCriteria)
            .build();
    assertThrows(InputRequiredException.class, noCriteriaParam::validate);

    buildDocParam().validate();
  }

  @Test
  public void testFromDashScopeResult() throws Exception {
    NetworkResponse response =
        NetworkResponse.builder()
            .headers(Collections.emptyMap())
            .message(DOC_RESPONSE_BODY)
            .httpStatusCode(200)
            .build();
    DashScopeResult dashScopeResult =
        new DashScopeResult().fromResponse(Protocol.HTTP, response, true);

    DecisionModelResult result = DecisionModelResult.fromDashScopeResult(dashScopeResult);
    assertEquals("decision-model-preview", result.getModel());
    assertEquals("7b986c65-b223-9341-b5f0-b988e27ecaac", result.getRequestId());
    assertEquals(125, result.getUsage().getInputTokens());
    assertEquals(52.9, result.getLatencyMs(), 1e-6);
    assertEquals(3, result.getAnswers().size());

    DecisionAnswer department = result.getAnswers().get("department");
    assertEquals("choice", department.getType());
    assertEquals("billing", department.getChoice());
    assertEquals(0.88, department.getConfidence(), 1e-6);
    assertEquals(0.94, department.getProbabilities().get("billing"), 1e-6);

    DecisionAnswer escalate = result.getAnswers().get("escalate");
    assertEquals("noul", escalate.getType());
    assertEquals(0.99, escalate.getNoul(), 1e-6);

    DecisionAnswer severity = result.getAnswers().get("severity");
    assertEquals("score", severity.getType());
    assertEquals(2.25, severity.getScore(), 1e-6);
    assertEquals("造成严重业务或安全影响", severity.getLegend().get("3"));
    assertEquals(0.73, severity.getProbabilities().get("2"), 1e-6);
  }

  @Test
  public void testRealCall() throws Exception {
    String apiKey = System.getenv(Constants.DASHSCOPE_API_KEY_ENV);
    org.junit.jupiter.api.Assumptions.assumeTrue(apiKey != null && !apiKey.isEmpty());
    DecisionModel decisionModel = new DecisionModel();
    DecisionModelResult result = decisionModel.call(buildDocParam());
    assertNotNull(result.getRequestId());
    assertNotNull(result.getAnswers());
    System.out.println(JsonUtils.toJson(result));
  }

  /** End-to-end through the real okhttp stack against a local mock server. */
  @Test
  public void testEndToEndWithMockServer() throws Exception {
    MockWebServer server = new MockWebServer();
    server.start();
    try {
      server.enqueue(
          new MockResponse()
              .setBody(DOC_RESPONSE_BODY)
              .setHeader("content-type", "application/json; charset=utf-8"));

      DecisionModelParam param = buildDocParam();
      param.setApiKey("test-api-key");
      DecisionModel decisionModel = new DecisionModel(server.url("/compatible-mode/v1").toString());
      DecisionModelResult result = decisionModel.call(param);

      // response parsing
      assertEquals("decision-model-preview", result.getModel());
      assertEquals("7b986c65-b223-9341-b5f0-b988e27ecaac", result.getRequestId());
      assertEquals(125, result.getUsage().getInputTokens());
      assertEquals(52.9, result.getLatencyMs(), 1e-6);
      assertEquals("billing", result.getAnswers().get("department").getChoice());
      assertEquals(0.99, result.getAnswers().get("escalate").getNoul(), 1e-6);
      assertEquals(2.25, result.getAnswers().get("severity").getScore(), 1e-6);

      // request on the wire
      RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
      assertNotNull(request, "no request reached the mock server");
      assertEquals("POST", request.getMethod());
      assertEquals("/compatible-mode/v1/systemone", request.getPath());
      assertEquals("Bearer test-api-key", request.getHeader("Authorization"));
      assertEquals(
          "java-sdk/" + Version.version + "/decision",
          request.getHeader("x-dashscope-sdk-client"));
      JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
      assertEquals("decision-model-preview", body.get("model").getAsString());
      assertEquals("T-1001", body.getAsJsonObject("state").get("ticket_id").getAsString());
      assertEquals(3, body.getAsJsonObject("questions").size());
      assertEquals(
          "noul",
          body.getAsJsonObject("questions").getAsJsonObject("escalate").get("type").getAsString());
    } finally {
      server.close();
    }
  }

  /** Non-2xx responses must surface the server error code and message. */
  @Test
  public void testErrorResponseWithMockServer() throws Exception {
    MockWebServer server = new MockWebServer();
    server.start();
    try {
      server.enqueue(
          new MockResponse()
              .setResponseCode(400)
              .setBody(
                  "{\"request_id\":\"req-err-1\",\"code\":\"InvalidParameter\",\"message\":\"questions is required\"}")
              .setHeader("content-type", "application/json; charset=utf-8"));

      DecisionModelParam param = buildDocParam();
      param.setApiKey("test-api-key");
      DecisionModel decisionModel = new DecisionModel(server.url("/compatible-mode/v1").toString());

      ApiException e = assertThrows(ApiException.class, () -> decisionModel.call(param));
      assertEquals(400, e.getStatus().getStatusCode());
      assertEquals("InvalidParameter", e.getStatus().getCode());
      assertEquals("questions is required", e.getStatus().getMessage());
      assertEquals("req-err-1", e.getStatus().getRequestId());
    } finally {
      server.close();
    }
  }
}
