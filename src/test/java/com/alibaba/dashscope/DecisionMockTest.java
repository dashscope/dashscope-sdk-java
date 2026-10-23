// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeAsyncClient;
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.v3.errors.BadRequestException;
import com.alibaba.dashscope.v3.types.decision.DecisionAnswer;
import com.alibaba.dashscope.v3.types.decision.DecisionCreateParams;
import com.alibaba.dashscope.v3.types.decision.DecisionQuestion;
import com.alibaba.dashscope.v3.types.decision.DecisionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer tests for {@code decision()} (2.x decision/ + Python v2 decision.py parity). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class DecisionMockTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MockWebServer server;
  private DashScopeClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    client =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.shutdown();
  }

  private static JsonNode bodyOf(RecordedRequest request) throws IOException {
    return MAPPER.readTree(request.getBody().readUtf8());
  }

  private static MockResponse okJson(String json) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(json);
  }

  private static DecisionCreateParams sampleParams() {
    Map<String, DecisionQuestion> questions = new LinkedHashMap<>();
    questions.put(
        "department",
        DecisionQuestion.choice(
            "Which team should handle this?",
            new LinkedHashMap<String, String>() {
              {
                put("billing", " invoices and refunds");
                put("technical", "product defects");
              }
            }));
    questions.put("escalate", DecisionQuestion.noul("Page the on-call engineer?"));
    questions.put(
        "severity", DecisionQuestion.score("How severe?", Arrays.asList("low", "mid", "high")));
    return DecisionCreateParams.builder()
        .model("decision-model-preview")
        .state(Collections.singletonMap("ticket_id", "T-1001"))
        .questions(questions)
        .build();
  }

  @Test
  void createHappyPathMixedQuestions() throws Exception {
    server.enqueue(
        okJson(
            "{\"model\":\"decision-model-preview\",\"answers\":{"
                + "\"department\":{\"type\":\"choice\",\"choice\":\"technical\","
                + "\"confidence\":0.87,\"probabilities\":{\"billing\":0.13,\"technical\":0.87}},"
                + "\"escalate\":{\"type\":\"noul\",\"noul\":0.05},"
                + "\"severity\":{\"type\":\"score\",\"score\":1.43,\"confidence\":0.62,"
                + "\"probabilities\":{\"0\":0.1,\"1\":0.6,\"2\":0.3},"
                + "\"legend\":{\"0\":\"low\",\"1\":\"mid\",\"2\":\"high\"}}},"
                + "\"usage\":{\"input_tokens\":128},\"latency_ms\":35.2,"
                + "\"request_id\":\"req-dec-1\"}"));

    DecisionResult result = client.decision().create(sampleParams());

    assertFalse(result.isError());
    assertEquals("decision-model-preview", result.getModel());
    assertEquals(3, result.getAnswers().size());
    DecisionAnswer department = result.getAnswers().get("department");
    assertEquals("choice", department.getType());
    assertEquals("technical", department.getChoice());
    assertEquals(0.87, department.getConfidence());
    assertEquals(0.87, department.getProbabilities().get("technical"));
    assertEquals(0.05, result.getAnswers().get("escalate").getNoul());
    DecisionAnswer severity = result.getAnswers().get("severity");
    assertEquals(1.43, severity.getScore());
    assertEquals("mid", severity.getLegend().get("1"));
    assertEquals(128, result.getUsage().getInputTokens());
    assertEquals(35.2, result.getLatencyMs());
    assertEquals("req-dec-1", result.getRequestId());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    // A custom base URL is the service root itself; the path is appended as-is.
    assertEquals("/systemone", request.getPath());
    assertEquals("POST", request.getMethod());
    assertEquals("Bearer test-key", request.getHeader("Authorization"));
    JsonNode body = bodyOf(request);
    assertEquals("decision-model-preview", body.get("model").asText());
    assertEquals("T-1001", body.get("state").get("ticket_id").asText());
    JsonNode questions = body.get("questions");
    assertEquals("choice", questions.get("department").get("type").asText());
    assertEquals(
        "product defects",
        questions.get("department").get("criteria").get("technical").asText().trim());
    assertEquals("noul", questions.get("escalate").get("type").asText());
    // Unset optional criteria stay off the wire (exclude_none parity).
    assertFalse(questions.get("escalate").has("criteria"));
    assertEquals(3, questions.get("severity").get("criteria").size());
  }

  @Test
  void nativeBaseUrlDerivesCompatibleModeRoot() throws Exception {
    server.enqueue(okJson("{\"model\":\"m\",\"answers\":{},\"request_id\":\"r\"}"));
    try (DashScopeClient nativeClient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/api/v1").toString())
            .maxRetries(0)
            .build()) {
      nativeClient.decision().create(sampleParams());
    }
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/compatible-mode/v1/systemone", request.getPath());
  }

  @Test
  void bareStringStateSerializesVerbatim() throws Exception {
    server.enqueue(okJson("{\"model\":\"m\",\"answers\":{},\"request_id\":\"r\"}"));
    client
        .decision()
        .create(
            DecisionCreateParams.builder()
                .model("decision-model-preview")
                .state("这是一段自由文本")
                .questions(
                    Collections.singletonMap("ok", DecisionQuestion.noul("Is this acceptable?")))
                .build());
    JsonNode body = bodyOf(server.takeRequest(5, TimeUnit.SECONDS));
    assertTrue(body.get("state").isTextual());
    assertEquals("这是一段自由文本", body.get("state").asText());
  }

  @Test
  void rawQuestionPassesThroughUnvalidated() throws Exception {
    server.enqueue(okJson("{\"model\":\"m\",\"answers\":{},\"request_id\":\"r\"}"));
    Map<String, Object> wire = new LinkedHashMap<>();
    wire.put("type", "future-type");
    wire.put("future_field", 42);
    client
        .decision()
        .create(
            DecisionCreateParams.builder()
                .model("decision-model-preview")
                .state("s")
                .questions(Collections.singletonMap("q", DecisionQuestion.raw(wire)))
                .build());
    JsonNode question = bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("questions").get("q");
    assertEquals("future-type", question.get("type").asText());
    assertEquals(42, question.get("future_field").asInt());
  }

  @Test
  void validationRejectsBadParams() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DecisionCreateParams.builder().state("s").questions(questionsOk()).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> DecisionCreateParams.builder().model("m").questions(questionsOk()).build());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DecisionCreateParams.builder()
                .model("m")
                .state("s")
                .questions(Collections.emptyMap())
                .build());
    // choice with zero options
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DecisionCreateParams.builder()
                .model("m")
                .state("s")
                .questions(
                    Collections.singletonMap(
                        "q", DecisionQuestion.choice("i", Collections.emptyMap())))
                .build());
    // score with one level
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DecisionCreateParams.builder()
                .model("m")
                .state("s")
                .questions(
                    Collections.singletonMap(
                        "q", DecisionQuestion.score("i", Collections.singletonList("only"))))
                .build());
    // empty instructions
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DecisionCreateParams.builder()
                .model("m")
                .state("s")
                .questions(Collections.singletonMap("q", DecisionQuestion.noul("")))
                .build());
  }

  private static Map<String, DecisionQuestion> questionsOk() {
    return Collections.singletonMap("q", DecisionQuestion.noul("ok?"));
  }

  @Test
  void nestedErrorRaisesByDefault() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"error\":{\"code\":\"InvalidParameter\",\"message\":\"bad question\"}}"));

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> client.decision().create(sampleParams()));
    assertEquals("InvalidParameter", e.getCode());
    assertEquals("bad question", e.getMessage());
  }

  @Test
  void nestedErrorSurfacesWhenNotRaising() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"error\":{\"code\":\"InvalidParameter\",\"message\":\"bad question\"},"
                    + "\"request_id\":\"req-err\"}"));

    try (DashScopeClient lax =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      DecisionResult result = lax.decision().create(sampleParams());
      assertTrue(result.isError());
      assertEquals("InvalidParameter", result.getCode());
      assertEquals("bad question", result.getMessage());
      assertEquals("req-err", result.getRequestId());
      assertEquals(400, result.getStatusCode());
      assertTrue(result.getAnswers().isEmpty());
      assertNull(result.getUsage());
    }
  }

  @Test
  void asyncCreateCompletes() throws Exception {
    server.enqueue(
        okJson(
            "{\"model\":\"decision-model-preview\",\"answers\":{\"ok\":{\"type\":\"noul\","
                + "\"noul\":0.9}},\"request_id\":\"req-async\"}"));
    try (DashScopeAsyncClient async =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build()) {
      DecisionResult result =
          async
              .decision()
              .create(
                  DecisionCreateParams.builder()
                      .model("decision-model-preview")
                      .state("s")
                      .questions(questionsOk())
                      .build())
              .get(10, TimeUnit.SECONDS);
      assertEquals(0.9, result.getAnswers().get("ok").getNoul());
      assertEquals("req-async", result.getRequestId());
    }
  }
}
