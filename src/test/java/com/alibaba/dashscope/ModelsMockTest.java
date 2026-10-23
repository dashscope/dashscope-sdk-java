// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.errors.NotFoundException;
import com.alibaba.dashscope.v3.types.models.Model;
import com.alibaba.dashscope.v3.types.models.ModelList;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer tests for {@code models()} (Python v2 models.py parity). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class ModelsMockTest {
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
  void listFiltersProviderClientSide() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"models\":["
                + "{\"model\":\"qwen-max\",\"provider\":\"Alibaba\",\"opensource\":false},"
                + "{\"model\":\"llama3\",\"source\":\"ModelScope\",\"opensource\":true}],"
                + "\"page_no\":1,\"page_size\":10,\"total\":2},\"request_id\":\"r1\"}"));

    ModelList page = client.models().list(1, 10, "modelscope", RequestOptions.NONE);
    // Items narrowed client-side; the total stays the server's pre-filter count.
    assertEquals(1, page.getModels().size());
    assertEquals("llama3", page.getModels().get(0).getModel());
    assertEquals(2, page.getTotalCount());
    assertEquals("r1", page.getRequestId());
    // The wire field is "opensource".
    assertEquals(Boolean.TRUE, page.getModels().get(0).getOpenSource());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/models?page_no=1&page_size=10", request.getPath());
  }

  @Test
  void getReturnsFirstMatch() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"models\":[{\"model\":\"qwen-max\",\"provider\":\"Alibaba\"}]},"
                + "\"request_id\":\"r2\"}"));
    Model model = client.models().get("qwen-max");
    assertEquals("qwen-max", model.getModel());
    assertEquals("Alibaba", model.getProvider());
    assertEquals("r2", model.getRequestId());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/models?model=qwen-max&page_no=1&page_size=1", request.getPath());
  }

  @Test
  void getMissingRaisesNotFound() throws Exception {
    server.enqueue(okJson("{\"output\":{\"models\":[]},\"request_id\":\"r3\"}"));
    NotFoundException e =
        assertThrows(NotFoundException.class, () -> client.models().get("no-such-model"));
    assertTrue(e.getMessage().contains("no-such-model"));
    // Business-code layer: status pinned to 200.
    assertEquals(200, e.getStatusCode().intValue());
    assertEquals("r3", e.getRequestId());
  }

  @Test
  void getMissingWithRaiseOnErrorFalseReturnsEmptyModel() throws Exception {
    server.enqueue(okJson("{\"output\":{\"models\":[]},\"request_id\":\"r4\"}"));
    try (DashScopeClient lenient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      Model model = lenient.models().get("no-such-model");
      assertNull(model.getModel());
      assertFalse(model.isError());
      assertEquals("r4", model.getRequestId());
    }
  }
}
