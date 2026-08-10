package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.agentstudio.message.ClientEvents;
import com.alibaba.dashscope.agentstudio.model.Deployment;
import com.alibaba.dashscope.agentstudio.model.DeploymentRun;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.DeploymentAgentParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentCreateParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentListParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentResourceParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentRunListParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentScheduleParam;
import com.alibaba.dashscope.agentstudio.param.DeploymentUpdateParam;
import com.alibaba.dashscope.agentstudio.resource.DeploymentRuns;
import com.alibaba.dashscope.agentstudio.resource.Deployments;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class TestAgentStudioDeployments {
  private static MockWebServer mockServer;
  private static JsonObject fixtures;
  private static String baseUrl;

  @BeforeAll
  public static void before() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start();
    baseUrl = mockServer.url("/api/v1/agentstudio/").toString();
    byte[] content = Files.readAllBytes(Paths.get("./src/test/resources/agentstudio.json"));
    fixtures = JsonUtils.parse(new String(content, StandardCharsets.UTF_8));
  }

  @AfterAll
  public static void after() throws IOException {
    mockServer.close();
  }

  private void enqueue(String fixtureKey) {
    JsonObject response = fixtures.get(fixtureKey).getAsJsonObject();
    mockServer.enqueue(TestUtils.createMockResponse(JsonUtils.toJson(response), 200));
  }

  @Test
  public void testCreateAndNestedResponseModels() throws Exception {
    enqueue("deployment_response");
    Deployment deployment =
        deployments()
            .create(
                DeploymentCreateParam.builder()
                    .name("daily-summary")
                    .description("Summarize orders")
                    .agent(DeploymentAgentParam.builder().id("agent_01").version(12).build())
                    .environmentId("env_01")
                    .schedule(schedule())
                    .initialEvents(
                        Collections.singletonList(
                            ClientEvents.userMessage("Summarize yesterday's orders")))
                    .resources(
                        Collections.singletonList(
                            DeploymentResourceParam.builder()
                                .type(DeploymentResourceParam.TYPE_FILE)
                                .fileId("file_01")
                                .mountPath("/mnt/data")
                                .build()))
                    .vaultIds(Collections.singletonList("vault_01"))
                    .metadata(Collections.singletonMap("biz", "summary"))
                    .build());

    RecordedRequest request = mockServer.takeRequest();
    JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().endsWith("/deployments"));
    assertEquals("agent_01", body.getAsJsonObject("agent").get("id").getAsString());
    assertEquals("Asia/Shanghai", body.getAsJsonObject("schedule").get("timezone").getAsString());
    assertEquals(
        "message",
        body.getAsJsonArray("initial_events").get(0).getAsJsonObject().get("type").getAsString());
    assertEquals("summary", body.getAsJsonObject("metadata").get("biz").getAsString());

    assertEquals("depl_01", deployment.getId());
    assertEquals(Integer.valueOf(12), deployment.getAgent().getVersion());
    assertEquals("qwen3-max", deployment.getAgent().getModel().getId());
    assertEquals("Asia/Shanghai", deployment.getSchedule().getTimezone());
    assertEquals("file_01", deployment.getResources().get(0).getFileId());
    assertEquals("RUN_FAILED", deployment.getPausedReason().getError().getCode());
    assertEquals("summary", deployment.getMetadata().get("biz"));
    assertEquals("req-depl-01", deployment.getRequestId());
  }

  @Test
  public void testUpdateCanExplicitlyClearNullableFields() throws Exception {
    enqueue("deployment_response");
    DeploymentUpdateParam updateParam =
        DeploymentUpdateParam.builder()
            .clearEnvironment(true)
            .clearSchedule(true)
            .resources(Collections.emptyList())
            .metadata(Collections.emptyMap())
            .build();
    assertTrue(updateParam.getClearEnvironment());
    assertTrue(updateParam.getClearSchedule());
    assertTrue(updateParam.getHttpBody().has("environment_id"));
    assertTrue(updateParam.getHttpBody().has("schedule"));
    deployments().update("depl_01", updateParam);

    RecordedRequest request = mockServer.takeRequest();
    JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().endsWith("/deployments/depl_01"));
    assertTrue(body.has("environment_id"), body.toString());
    assertTrue(body.get("environment_id").isJsonNull());
    assertTrue(body.has("schedule"), body.toString());
    assertTrue(body.get("schedule").isJsonNull());
    assertEquals(0, body.getAsJsonArray("resources").size());
    assertEquals(0, body.getAsJsonObject("metadata").size());
  }

  @Test
  public void testListFiltersAndPagination() throws Exception {
    enqueue("deployment_list_response");
    CursorPage<Deployment> page =
        deployments()
            .list(
                DeploymentListParam.builder()
                    .agentId("agent_01")
                    .keyword("daily")
                    .status("active")
                    .includeArchived(true)
                    .limit(10)
                    .createdAtGte("2026-07-01T00:00:00Z")
                    .createdAtLte("2026-07-31T23:59:59Z")
                    .build());

    RecordedRequest request = mockServer.takeRequest();
    assertEquals("agent_01", request.getRequestUrl().queryParameter("agent_id"));
    assertEquals("true", request.getRequestUrl().queryParameter("include_archived"));
    assertEquals("2026-07-01T00:00:00Z", request.getRequestUrl().queryParameter("created_at[gte]"));
    assertEquals(1, page.getData().size());
    assertEquals("next-deployment", page.getNextPage());
  }

  @Test
  public void testLifecycleAndDeploymentRunRoutes() throws Exception {
    enqueue("deployment_response");
    deployments().pause("depl_01");
    assertTrue(mockServer.takeRequest().getPath().endsWith("/deployments/depl_01/pause"));

    enqueue("deployment_response");
    deployments().unpause("depl_01");
    assertTrue(mockServer.takeRequest().getPath().endsWith("/deployments/depl_01/unpause"));

    enqueue("deployment_response");
    deployments().archive("depl_01");
    assertTrue(mockServer.takeRequest().getPath().endsWith("/deployments/depl_01/archive"));

    enqueue("deployment_run_response");
    DeploymentRun run = deployments().run("depl_01");
    assertTrue(mockServer.takeRequest().getPath().endsWith("/deployments/depl_01/run"));
    assertEquals("drun_01", run.getId());
    assertEquals(Integer.valueOf(12), run.getAgent().getVersion());
    assertEquals("RUN_FAILED", run.getError().getCode());
    assertEquals("req-run-01", run.getRequestId());

    enqueue("deployment_run_list_response");
    CursorPage<DeploymentRun> history =
        deployments().listRuns("depl_01", DeploymentRunListParam.builder().limit(20).build());
    assertTrue(mockServer.takeRequest().getPath().contains("/deployments/depl_01/runs"));
    assertEquals("drun_01", history.getData().get(0).getId());

    enqueue("deployment_run_list_response");
    CursorPage<DeploymentRun> allRuns =
        deploymentRuns().list(DeploymentRunListParam.builder().limit(20).build());
    assertTrue(mockServer.takeRequest().getPath().contains("/deployment_runs?"));
    assertEquals("depl_01", allRuns.getData().get(0).getDeploymentId());
    assertEquals("req-run-list", allRuns.getRequestId());

    enqueue("deployment_run_response");
    DeploymentRun retrieved = deploymentRuns().retrieve("drun_01");
    assertTrue(mockServer.takeRequest().getPath().endsWith("/deployment_runs/drun_01"));
    assertEquals("session_01", retrieved.getSessionId());
  }

  @Test
  public void testAsyncAndClientAccessors() throws Exception {
    try (AgentStudioClient client =
        new AgentStudioClient("test-key", baseUrl, "ws_01", null, null)) {
      assertNotNull(client.deployments());
      assertNotNull(client.deploymentRuns());
    }

    enqueue("deployment_response");
    Deployment deployment = deployments().retrieveAsync("depl_01").get();
    mockServer.takeRequest();
    assertEquals("depl_01", deployment.getId());
  }

  private static Deployments deployments() {
    return new Deployments(baseUrl, null, "test-key");
  }

  private static DeploymentRuns deploymentRuns() {
    return new DeploymentRuns(baseUrl, null, "test-key");
  }

  private static DeploymentScheduleParam schedule() {
    return DeploymentScheduleParam.builder()
        .type(DeploymentScheduleParam.TYPE_CRON)
        .expression("0 9 * * 1-5")
        .timezone("Asia/Shanghai")
        .build();
  }
}
