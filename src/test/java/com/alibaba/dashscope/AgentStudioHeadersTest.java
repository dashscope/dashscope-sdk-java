// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.alibaba.dashscope.applications.agentstudio.param.AgentCreateParam;
import com.alibaba.dashscope.applications.agentstudio.resource.AgentStudioEventStream;
import com.alibaba.dashscope.applications.agentstudio.resource.Agents;
import com.alibaba.dashscope.applications.agentstudio.resource.FileContent;
import com.alibaba.dashscope.applications.agentstudio.resource.Files;
import com.alibaba.dashscope.applications.agentstudio.resource.SessionEvents;
import com.alibaba.dashscope.shared.SdkInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Asserts the AgentStudio SDK tracking header policy: every request path (ordinary HTTP, file
 * upload, file download, session SSE) carries {@code x-dashscope-sdk-client:
 * java-sdk/<version>/agentstudio3}, caller-supplied values win, and DASHSCOPE_DISABLE_SDK_HEADERS
 * opts out without dropping caller-set headers.
 */
@ClearEnvironmentVariable(key = "DASHSCOPE_DISABLE_SDK_HEADERS")
class AgentStudioHeadersTest {
  private static final String EXPECTED_SDK_CLIENT = "java-sdk/" + SdkInfo.VERSION + "/agentstudio3";
  private static final String API_KEY = "test-key";

  private MockWebServer server;
  private String baseUrl;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    baseUrl = String.format("http://127.0.0.1:%s/api/v1/", server.getPort());
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  private void enqueueJson(String body) {
    server.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setBody(body));
  }

  private void createAgent(AgentCreateParam param) {
    enqueueJson("{\"id\":\"agent_1\"}");
    new Agents(baseUrl, null, API_KEY).create(param);
  }

  @Test
  void ordinaryHttpCarriesAgentStudio3Module() throws Exception {
    createAgent(AgentCreateParam.builder().name("a").model("qwen-max").build());
    RecordedRequest request = server.takeRequest();
    assertEquals(EXPECTED_SDK_CLIENT, request.getHeader("x-dashscope-sdk-client"));
    assertNotNull(request.getHeader("x-dashscope-sdk-session-id"));
    assertEquals("Bearer " + API_KEY, request.getHeader("Authorization"));
  }

  @Test
  void callerSdkClientHeaderWinsOnOrdinaryHttp() throws Exception {
    createAgent(
        AgentCreateParam.builder()
            .name("a")
            .model("qwen-max")
            .header("x-dashscope-sdk-client", "my-client/9.9")
            .build());
    RecordedRequest request = server.takeRequest();
    assertEquals("my-client/9.9", request.getHeader("x-dashscope-sdk-client"));
  }

  @Test
  void fileUploadCarriesAgentStudio3Module() throws Exception {
    enqueueJson("{\"id\":\"file_1\"}");
    byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
    new Files(baseUrl, null, API_KEY)
        .upload("a.txt", new ByteArrayInputStream(bytes), "text/plain");
    RecordedRequest request = server.takeRequest();
    assertEquals(EXPECTED_SDK_CLIENT, request.getHeader("x-dashscope-sdk-client"));
    assertNotNull(request.getHeader("x-dashscope-sdk-session-id"));
    assertEquals("Bearer " + API_KEY, request.getHeader("Authorization"));
  }

  @Test
  void fileDownloadCarriesAgentStudio3Module() throws Exception {
    byte[] bytes = "binary-content".getBytes(StandardCharsets.UTF_8);
    server.enqueue(new MockResponse().setBody(new okio.Buffer().write(bytes)));
    FileContent content = new Files(baseUrl, null, API_KEY).download("file_1");
    assertArrayEquals(bytes, content.getBytes());
    RecordedRequest request = server.takeRequest();
    assertEquals(EXPECTED_SDK_CLIENT, request.getHeader("x-dashscope-sdk-client"));
    assertNotNull(request.getHeader("x-dashscope-sdk-session-id"));
    assertEquals("Bearer " + API_KEY, request.getHeader("Authorization"));
  }

  @Test
  void sessionSseCarriesAgentStudio3Module() throws Exception {
    server.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "text/event-stream")
            .setBody("data: {\"type\":\"session_status\"}\n\n"));
    try (AgentStudioEventStream stream =
        new SessionEvents(baseUrl, null, API_KEY).stream("sess_1")) {
      for (Object ignored : stream) {
        // drain the single frame so the request is fully recorded
      }
    }
    RecordedRequest request = server.takeRequest();
    assertEquals(EXPECTED_SDK_CLIENT, request.getHeader("x-dashscope-sdk-client"));
    assertNotNull(request.getHeader("x-dashscope-sdk-session-id"));
    assertEquals("Bearer " + API_KEY, request.getHeader("Authorization"));
    assertEquals("text/event-stream", request.getHeader("Accept"));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_DISABLE_SDK_HEADERS", value = "1")
  void optOutRemovesTrackingHeadersOnOrdinaryHttp() throws Exception {
    createAgent(AgentCreateParam.builder().name("a").model("qwen-max").build());
    RecordedRequest request = server.takeRequest();
    assertNull(request.getHeader("x-dashscope-sdk-client"));
    assertNull(request.getHeader("x-dashscope-sdk-session-id"));
    assertEquals("Bearer " + API_KEY, request.getHeader("Authorization"));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_DISABLE_SDK_HEADERS", value = "TRUE")
  void optOutIsCaseInsensitiveAndAppliesToUpload() throws Exception {
    enqueueJson("{\"id\":\"file_1\"}");
    new Files(baseUrl, null, API_KEY)
        .upload(
            "a.txt",
            new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
            "text/plain");
    RecordedRequest request = server.takeRequest();
    assertNull(request.getHeader("x-dashscope-sdk-client"));
    assertNull(request.getHeader("x-dashscope-sdk-session-id"));
    assertEquals("Bearer " + API_KEY, request.getHeader("Authorization"));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_DISABLE_SDK_HEADERS", value = "1")
  void optOutKeepsCallerSetSdkClientHeader() throws Exception {
    createAgent(
        AgentCreateParam.builder()
            .name("a")
            .model("qwen-max")
            .header("x-dashscope-sdk-client", "my-client/9.9")
            .build());
    RecordedRequest request = server.takeRequest();
    assertEquals("my-client/9.9", request.getHeader("x-dashscope-sdk-client"));
    assertNull(request.getHeader("x-dashscope-sdk-session-id"));
  }
}
