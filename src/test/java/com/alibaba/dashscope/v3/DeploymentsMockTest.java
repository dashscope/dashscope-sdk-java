// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.v3.errors.BadRequestException;
import com.alibaba.dashscope.v3.resources.deployments.DeploymentEventStream;
import com.alibaba.dashscope.v3.types.common.JobStatus;
import com.alibaba.dashscope.v3.types.deployments.Deployment;
import com.alibaba.dashscope.v3.types.deployments.DeploymentCreateParams;
import com.alibaba.dashscope.v3.types.deployments.DeploymentEvent;
import com.alibaba.dashscope.v3.types.deployments.DeploymentList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer tests for {@code deployments()} (Python v2 deployments.py parity). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class DeploymentsMockTest {
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

  @Test
  void createPostsWireSpellings() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"deployed_model\":\"dep-1\",\"model_name\":\"my-ft\","
                + "\"status\":\"PENDING\",\"capacity\":2},\"request_id\":\"r1\"}"));

    Deployment deployment =
        client
            .deployments()
            .create(
                DeploymentCreateParams.builder()
                    .model("my-ft")
                    .capacity(2)
                    .modelVersion("v1")
                    .suffix("sfx")
                    .plan("plan-a")
                    .templateId("tpl-1")
                    .deploySpec(Collections.singletonMap("instance_type", "ecs.g7"))
                    .billingMethod("mu")
                    .build());

    assertEquals("dep-1", deployment.getDeployedModel());
    // The model_name wire alias lands on model.
    assertEquals("my-ft", deployment.getModel());
    assertEquals(2, deployment.getCapacity());
    assertEquals("r1", deployment.getRequestId());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", request.getMethod());
    assertEquals("/deployments", request.getPath());
    JsonNode body = bodyOf(request);
    // Public names model/modelVersion ride as model_name/model_version.
    assertEquals("my-ft", body.get("model_name").asText());
    assertFalse(body.has("model"));
    assertEquals(2, body.get("capacity").asInt());
    assertEquals("v1", body.get("model_version").asText());
    assertEquals("sfx", body.get("suffix").asText());
    assertEquals("plan-a", body.get("plan").asText());
    assertEquals("tpl-1", body.get("template_id").asText());
    assertEquals("ecs.g7", body.get("deploy_spec").get("instance_type").asText());
    assertEquals("mu", body.get("billing_method").asText());
  }

  @Test
  void getParsesFlattenedDeployment() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"deployed_model\":\"dep-2\",\"status\":\"RUNNING\","
                + "\"ready_capacity\":2,\"gmt_create\":\"2026-10-01\",\"gmt_modified\":\"2026-10-20\"},"
                + "\"request_id\":\"r2\"}"));
    Deployment deployment = client.deployments().get("dep-2");
    assertEquals("dep-2", deployment.getDeployedModel());
    assertEquals("RUNNING", deployment.getStatus());
    assertEquals(2, deployment.getReadyCapacity());
    // gmt_* aliases land on createdAt/updatedAt.
    assertEquals("2026-10-01", deployment.getCreatedAt());
    assertEquals("2026-10-20", deployment.getUpdatedAt());
    assertEquals("r2", deployment.getRequestId());
    assertEquals("/deployments/dep-2", server.takeRequest(5, TimeUnit.SECONDS).getPath());
  }

  @Test
  void listParsesPage() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"deployments\":[{\"deployed_model\":\"d1\"},"
                + "{\"deployed_model\":\"d2\"}],\"page_no\":1,\"page_size\":10,\"total\":2},"
                + "\"request_id\":\"r3\"}"));
    DeploymentList page = client.deployments().list(1, 10);
    assertEquals(2, page.getTotalCount());
    assertEquals(2, page.getDeployments().size());
    assertEquals("d2", page.getDeployments().get(1).getDeployedModel());
    assertEquals("r3", page.getRequestId());
    assertEquals("/deployments?page_no=1&page_size=10",
        server.takeRequest(5, TimeUnit.SECONDS).getPath());
  }

  @Test
  void scalePutsCapacity() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"deployed_model\":\"dep-3\",\"capacity\":4,\"status\":\"SCALING\"},"
                + "\"request_id\":\"r4\"}"));
    Deployment deployment = client.deployments().scale("dep-3", 4);
    assertEquals(4, deployment.getCapacity());
    assertEquals("SCALING", deployment.getStatus());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("PUT", request.getMethod());
    assertEquals("/deployments/dep-3/scale", request.getPath());
    JsonNode body = bodyOf(request);
    assertEquals("dep-3", body.get("deployed_model").asText());
    assertEquals(4, body.get("capacity").asInt());
  }

  @Test
  void deleteReturnsJobStatus() throws Exception {
    server.enqueue(okJson("{\"output\":{\"status\":\"DELETED\"},\"request_id\":\"r5\"}"));
    JobStatus status = client.deployments().delete("dep-4");
    assertEquals("DELETED", status.getStatus());
    assertEquals("r5", status.getRequestId());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("DELETE", request.getMethod());
    assertEquals("/deployments/dep-4", request.getPath());
  }

  @Test
  void streamEventsParsesFramesAndErrorRaises() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody("data: {\"phase\": \"pulling\"}\n\ndata: {\"phase\": \"ready\"}\n\n"));
    List<DeploymentEvent> events = new ArrayList<>();
    try (DeploymentEventStream stream = client.deployments().streamEvents("dep-5")) {
      for (DeploymentEvent event : stream) {
        events.add(event);
      }
    }
    assertEquals(2, events.size());
    assertEquals("ready", events.get(1).getOutput().get("phase").asText());
    assertEquals("/deployments/dep-5/stream",
        server.takeRequest(5, TimeUnit.SECONDS).getPath());

    // Error frame without a status line: the body's own code picks the class.
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "event:error\ndata: {\"code\":\"InvalidParameter\",\"message\":\"bad scale\"}\n\n"));
    try (DeploymentEventStream stream = client.deployments().streamEvents("dep-5")) {
      BadRequestException e =
          assertThrows(
              BadRequestException.class,
              () -> {
                for (DeploymentEvent ignored : stream) {
                  // drain
                }
              });
      assertEquals("bad scale", e.getMessage());
    }
  }

  @Test
  void validationRejectsBadInput() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DeploymentCreateParams.builder().capacity(1).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> DeploymentCreateParams.builder().model("m").build());
    assertThrows(IllegalArgumentException.class, () -> client.deployments().get(""));
    assertThrows(IllegalArgumentException.class, () -> client.deployments().scale(null, 1));
  }
}
