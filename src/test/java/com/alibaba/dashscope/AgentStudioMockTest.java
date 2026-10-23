// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeAsyncClient;
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.agentstudio.model.Agent;
import com.alibaba.dashscope.agentstudio.model.Memory;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.AgentCreateParam;
import com.alibaba.dashscope.agentstudio.param.MemoryListParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Tests for the agentstudio single shared copy (design §7: the 2.x {@code agentstudio} package is
 * the canonical implementation for both generations) and the 3.0 client mount.
 */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@ClearEnvironmentVariable(key = "DASHSCOPE_API_REGION")
@ClearEnvironmentVariable(key = "DASHSCOPE_AGENTSTUDIO_URL")
@ClearEnvironmentVariable(key = "AGENTSTUDIO_URL")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
@Execution(ExecutionMode.SAME_THREAD)
public class AgentStudioMockTest {
  private static MockWebServer server;
  private static JsonObject fixtures;

  @BeforeAll
  public static void before() throws Exception {
    server = new MockWebServer();
    server.start();
    byte[] content = Files.readAllBytes(Paths.get("./src/test/resources/agentstudio.json"));
    fixtures = JsonUtils.parse(new String(content, StandardCharsets.UTF_8));
  }

  @AfterAll
  public static void after() throws Exception {
    server.close();
  }

  private static void enqueue(String fixtureKey) {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(JsonUtils.toJson(fixtures.get(fixtureKey).getAsJsonObject())));
  }

  private AgentStudioClient newClient() {
    return new AgentStudioClient(
        "test-key", server.url("/api/v1/agentstudio/").toString(), "ws_01", null, null);
  }

  @Test
  public void agentCreateHappyPath() throws Exception {
    enqueue("agent_response");
    AgentStudioClient client = newClient();
    Agent agent =
        client
            .agents()
            .create(AgentCreateParam.builder().name("test-agent").model("qwen-max").build());
    RecordedRequest req = server.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().endsWith("/api/v1/agentstudio/agents"));
    assertEquals("Bearer test-key", req.getHeader("Authorization"));
    assertEquals("agent_xyz", agent.getId());
    assertEquals("test-agent", agent.getName());
    assertEquals(Integer.valueOf(2), agent.getVersion());
    assertEquals("qwen-max", agent.getModel().getId());
    client.close();
  }

  @Test
  public void cursorPageTwoPageIteration() throws Exception {
    enqueue("memory_list_response");
    enqueue("memory_list_page2_response");
    AgentStudioClient client = newClient();
    CursorPage<Memory> page =
        client
            .memoryStores()
            .memories()
            .list(
                "memstore_01Hx",
                MemoryListParam.builder().pathPrefix("/").depth(1).view("basic").build());
    RecordedRequest first = server.takeRequest();
    assertEquals("GET", first.getMethod());
    assertTrue(first.getPath().contains("path_prefix=%2F"), "actual path: " + first.getPath());

    List<String> ids = new ArrayList<>();
    for (Memory item : page) {
      ids.add(item.getId());
    }
    assertEquals(Arrays.asList("mem_01Hy", "mem_02"), ids);
    RecordedRequest second = server.takeRequest();
    assertTrue(second.getPath().contains("page=tok_2"));
    client.close();
  }

  @Test
  public void notFoundRaisesApiException() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(404)
            .addHeader("Content-Type", "application/json")
            .setBody(
                "{\"code\":\"AgentNotFound\",\"message\":\"agent not found\","
                    + "\"request_id\":\"req-404\"}"));
    AgentStudioClient client = newClient();
    ApiException ex = assertThrows(ApiException.class, () -> client.agents().retrieve("nope"));
    assertEquals(404, ex.getStatus().getStatusCode());
    assertEquals("AgentNotFound", ex.getStatus().getCode());
    assertEquals("agent not found", ex.getStatus().getMessage());
    assertEquals("req-404", ex.getStatus().getRequestId());
    server.takeRequest(); // drain so later tests see only their own requests
    client.close();
  }

  @Test
  public void syncMountResolvesTemplateBaseUrl() {
    try (DashScopeClient client =
        DashScopeClient.builder().apiKey("test-key").workspace("ws_01").build()) {
      AgentStudioClient as = client.agentStudio();
      assertEquals(
          "https://ws_01.cn-beijing.maas.aliyuncs.com/api/v1/agentstudio", as.getBaseUrl());
      assertNotNull(as.agents());
    }
  }

  @Test
  public void mountWithoutWorkspaceThrows() {
    try (DashScopeClient client = DashScopeClient.builder().apiKey("test-key").build()) {
      assertThrows(IllegalArgumentException.class, client::agentStudio);
    }
  }

  @Test
  public void asyncMountReturnsSameClientType() {
    try (DashScopeAsyncClient client =
        DashScopeAsyncClient.builder().apiKey("test-key").workspace("ws_01").build()) {
      AgentStudioClient as = client.agentStudio();
      assertNotNull(as);
      assertEquals(
          "https://ws_01.cn-beijing.maas.aliyuncs.com/api/v1/agentstudio", as.getBaseUrl());
    }
  }
}
