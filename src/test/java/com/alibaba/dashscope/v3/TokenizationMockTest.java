// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeAsyncClient;
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.v3.types.messages.Message;
import com.alibaba.dashscope.v3.types.tokenization.TokenCount;
import com.alibaba.dashscope.v3.types.tokenization.TokenizationCountParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class TokenizationMockTest {
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

  private static MockResponse okJson(String json) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(json);
  }

  @Test
  void countPromptHappyPath() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"token_ids\":[2610,525,264,10950,17847,13],"
                + "\"tokens\":[\"You\",\" are\",\" a\",\" helpful\",\" assistant\",\".\"]},"
                + "\"usage\":{\"input_tokens\":6},\"request_id\":\"tk1\"}"));

    TokenCount count =
        client
            .tokenization()
            .count(
                TokenizationCountParams.builder()
                    .model("qwen-turbo")
                    .prompt("You are a helpful assistant.")
                    .build());

    assertEquals(Arrays.asList(2610, 525, 264, 10950, 17847, 13), count.getTokenIds());
    assertEquals(6, count.getTokens().size());
    assertEquals(6, count.getUsage().getInputTokens());
    assertEquals(6, count.getInputTokens());
    assertEquals("tk1", count.getRequestId());
    assertFalse(count.isError());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/tokenizer", request.getPath());
    assertEquals(
        "java-sdk/3.0.0-SNAPSHOT/tokenizers", request.getHeader("x-dashscope-sdk-client"));
    JsonNode body = MAPPER.readTree(request.getBody().readUtf8());
    assertEquals("qwen-turbo", body.get("model").asText());
    assertEquals("You are a helpful assistant.", body.get("input").get("prompt").asText());
    assertFalse(body.get("input").has("messages"));
    // parameters is always present, empty object by default (Python v2 parity).
    assertTrue(body.has("parameters"));
    assertEquals(0, body.get("parameters").size());
  }

  @Test
  void countMessagesAppendsPromptAsUserMessage() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"token_ids\":[1,2,3]},\"usage\":{\"input_tokens\":3},"
                + "\"request_id\":\"tk2\"}"));

    TokenCount count =
        client
            .tokenization()
            .count(
                TokenizationCountParams.builder()
                    .model("qwen-turbo")
                    .messages(
                        Arrays.asList(
                            Message.system("You are a helpful assistant."),
                            Message.user("hello")))
                    .prompt("extra question")
                    .parameters(Collections.singletonMap("truncation", true))
                    .build());

    assertEquals(Arrays.asList(1, 2, 3), count.getTokenIds());
    assertEquals(3, count.getInputTokens());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    JsonNode body = MAPPER.readTree(request.getBody().readUtf8());
    JsonNode input = body.get("input");
    assertFalse(input.has("prompt"));
    JsonNode messages = input.get("messages");
    assertEquals(3, messages.size());
    assertEquals("system", messages.get(0).get("role").asText());
    assertEquals("user", messages.get(2).get("role").asText());
    assertEquals("extra question", messages.get(2).get("content").asText());
    assertTrue(body.get("parameters").get("truncation").asBoolean());
  }

  @Test
  void countValidationRejectsBadInput() {
    assertThrows(
        IllegalArgumentException.class,
        () -> TokenizationCountParams.builder().prompt("hi").build());
    assertThrows(
        IllegalArgumentException.class,
        () -> TokenizationCountParams.builder().model("qwen-turbo").build());
  }

  @Test
  void asyncCountCompletes() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"token_ids\":[97]},\"usage\":{\"input_tokens\":1},"
                + "\"request_id\":\"tk3\"}"));

    DashScopeAsyncClient asyncClient =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build();
    try {
      TokenCount count =
          asyncClient
              .tokenization()
              .count(
                  TokenizationCountParams.builder().model("qwen-turbo").prompt("a").build())
              .get(10, TimeUnit.SECONDS);
      assertEquals(Arrays.asList(97), count.getTokenIds());
      assertEquals("tk3", count.getRequestId());
    } finally {
      asyncClient.close();
    }
  }
}
