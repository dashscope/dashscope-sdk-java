package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.agentstudio.message.ClientEvents;
import com.alibaba.dashscope.agentstudio.message.ContentBlock;
import com.alibaba.dashscope.agentstudio.message.Message;
import com.alibaba.dashscope.agentstudio.model.Agent;
import com.alibaba.dashscope.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.agentstudio.model.AgentStudioFile;
import com.alibaba.dashscope.agentstudio.model.AgentVersion;
import com.alibaba.dashscope.agentstudio.model.Credential;
import com.alibaba.dashscope.agentstudio.model.Environment;
import com.alibaba.dashscope.agentstudio.model.Session;
import com.alibaba.dashscope.agentstudio.model.Skill;
import com.alibaba.dashscope.agentstudio.model.SkillVersion;
import com.alibaba.dashscope.agentstudio.model.Vault;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.AgentCreateParam;
import com.alibaba.dashscope.agentstudio.param.AgentListParam;
import com.alibaba.dashscope.agentstudio.param.AgentUpdateParam;
import com.alibaba.dashscope.agentstudio.param.CredentialCreateParam;
import com.alibaba.dashscope.agentstudio.param.CredentialListParam;
import com.alibaba.dashscope.agentstudio.param.CredentialUpdateParam;
import com.alibaba.dashscope.agentstudio.param.EnvironmentCreateParam;
import com.alibaba.dashscope.agentstudio.param.EnvironmentListParam;
import com.alibaba.dashscope.agentstudio.param.FileListParam;
import com.alibaba.dashscope.agentstudio.param.SessionCreateParam;
import com.alibaba.dashscope.agentstudio.param.SessionEventListParam;
import com.alibaba.dashscope.agentstudio.param.SessionListParam;
import com.alibaba.dashscope.agentstudio.param.SessionUpdateParam;
import com.alibaba.dashscope.agentstudio.param.SkillCreateParam;
import com.alibaba.dashscope.agentstudio.param.SkillListParam;
import com.alibaba.dashscope.agentstudio.param.VaultCreateParam;
import com.alibaba.dashscope.agentstudio.param.VaultListParam;
import com.alibaba.dashscope.agentstudio.param.VaultUpdateParam;
import com.alibaba.dashscope.agentstudio.resource.Agents;
import com.alibaba.dashscope.agentstudio.resource.Environments;
import com.alibaba.dashscope.agentstudio.resource.Sessions;
import com.alibaba.dashscope.agentstudio.resource.Skills;
import com.alibaba.dashscope.agentstudio.resource.Vaults;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class TestAgentStudio {
  private static MockWebServer mockServer;
  private static JsonObject fixtures;

  @BeforeAll
  public static void before() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s/api/v1/", mockServer.getPort());
    Constants.apiKey = "test-key";
    byte[] content = Files.readAllBytes(Paths.get("./src/test/resources/agentstudio.json"));
    fixtures = JsonUtils.parse(new String(content, StandardCharsets.UTF_8));
  }

  @AfterAll
  public static void after() throws IOException {
    mockServer.close();
  }

  private void enqueue(String fixtureKey) {
    JsonObject rsp = fixtures.get(fixtureKey).getAsJsonObject();
    mockServer.enqueue(TestUtils.createMockResponse(JsonUtils.toJson(rsp), 200));
  }

  // ======================== Agents ========================

  @Test
  public void testAgentCreate() throws Exception {
    enqueue("agent_response");
    Agent agent =
        new Agents(null, null, null)
            .create(AgentCreateParam.builder().name("test-agent").model("qwen-max").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().endsWith("/agents"));
    assertEquals("agent_xyz", agent.getId());
    assertEquals("test-agent", agent.getName());
    assertEquals(Integer.valueOf(2), agent.getVersion());
    assertEquals("You are a helpful assistant.", agent.getSystemPrompt());
    assertEquals("qwen-max", agent.getModel().getId());
  }

  @Test
  public void testAgentCreateHttpBody() {
    JsonObject body =
        AgentCreateParam.builder()
            .name("my-agent")
            .model("qwen-plus")
            .description("desc")
            .systemPrompt("be helpful")
            .build()
            .getHttpBody();
    assertEquals("my-agent", body.get("name").getAsString());
    assertEquals("qwen-plus", body.getAsJsonObject("model").get("id").getAsString());
    assertEquals("desc", body.get("description").getAsString());
    assertEquals("be helpful", body.get("system").getAsString());
  }

  @Test
  public void testAgentRetrieve() throws Exception {
    enqueue("agent_response");
    Agent agent = new Agents(null, null, null).retrieve("agent_xyz");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/agents/agent_xyz"));
    assertEquals("A test agent", agent.getDescription());
    assertEquals("ws_001", agent.getWorkspaceId());
  }

  @Test
  public void testAgentUpdate() throws Exception {
    enqueue("agent_response");
    Agent agent =
        new Agents(null, null, null)
            .update(
                "agent_xyz", AgentUpdateParam.builder().version(2).name("updated-agent").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().contains("/agents/agent_xyz"));
    assertEquals("agent_xyz", agent.getId());
  }

  @Test
  public void testAgentUpdateRequiresVersion() {
    assertThrows(
        ApiException.class,
        () ->
            new Agents(null, null, null)
                .update("agent_xyz", AgentUpdateParam.builder().name("no-version").build()));
  }

  @Test
  public void testAgentUpdateRequiresAgentId() {
    assertThrows(
        ApiException.class,
        () ->
            new Agents(null, null, null)
                .update("", AgentUpdateParam.builder().version(1).name("x").build()));
  }

  @Test
  public void testAgentUpdateHttpBody() {
    JsonObject body =
        AgentUpdateParam.builder()
            .version(3)
            .model("qwen-turbo")
            .systemPrompt("new prompt")
            .build()
            .getHttpBody();
    assertEquals(3, body.get("version").getAsInt());
    assertEquals("qwen-turbo", body.getAsJsonObject("model").get("id").getAsString());
    assertEquals("new prompt", body.get("system").getAsString());
  }

  @Test
  public void testAgentList() throws Exception {
    enqueue("agent_list_response");
    CursorPage<Agent> page =
        new Agents(null, null, null).list(AgentListParam.builder().limit(20).build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("limit=20"));
    assertEquals(2, page.getData().size());
    assertEquals("agent_001", page.getData().get(0).getId());
    assertEquals("cursor_agent", page.getNextPage());
  }

  @Test
  public void testAgentListWithIncludeArchived() {
    String qs = AgentListParam.builder().limit(10).includeArchived(true).build().toQueryString();
    assertTrue(qs.contains("limit=10"));
    assertTrue(qs.contains("include_archived=true"));
  }

  @Test
  public void testAgentListVersions() throws Exception {
    enqueue("agent_version_response");
    CursorPage<AgentVersion> page =
        new Agents(null, null, null).listVersions("agent_xyz", AgentListParam.builder().build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/agents/agent_xyz/versions"));
    assertEquals(2, page.getData().size());
    assertEquals("agent_xyz", page.getData().get(0).getAgentId());
    assertEquals(Integer.valueOf(1), page.getData().get(0).getVersion());
    assertNull(page.getNextPage());
  }

  @Test
  public void testAgentModelDeserialization() throws Exception {
    enqueue("agent_response");
    Agent agent = new Agents(null, null, null).retrieve("agent_xyz");
    mockServer.takeRequest();
    assertEquals("agent", agent.getType());
    assertEquals("test-agent", agent.getName());
    assertEquals(Integer.valueOf(2), agent.getVersion());
    assertEquals("You are a helpful assistant.", agent.getSystem());
    assertNotNull(agent.getTools());
    assertEquals(1, agent.getTools().size());
    assertNotNull(agent.getMetadata());
    assertEquals("test", agent.getMetadata().get("env"));
    assertEquals("2025-01-01T00:00:00Z", agent.getCreatedAt());
  }

  // ======================== Sessions ========================

  @Test
  public void testSessionCreate() throws Exception {
    enqueue("session_response");
    Session session =
        new Sessions(null, null, null)
            .create(SessionCreateParam.builder().agent("agent_xyz").title("Test Session").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().endsWith("/sessions"));
    assertEquals("sess_abc123", session.getId());
    assertEquals("idle", session.getStatus());
    assertEquals("agent_xyz", session.getAgentId());
    assertNotNull(session.getStopReason());
    assertEquals("end_turn", session.getStopReason().getType());
    assertNotNull(session.getUsage());
    assertEquals(100L, session.getUsage().getInputTokens().longValue());
    assertEquals(42.5, session.getUsage().getSpeed(), 0.01);
  }

  @Test
  public void testSessionRetrieve() throws Exception {
    enqueue("session_response");
    Session session = new Sessions(null, null, null).retrieve("sess_abc123");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/sessions/sess_abc123"));
    assertEquals("sess_abc123", session.getId());
  }

  @Test
  public void testSessionUpdate() throws Exception {
    enqueue("session_response");
    Session session =
        new Sessions(null, null, null)
            .update("sess_abc123", SessionUpdateParam.builder().title("Updated").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().contains("/sessions/sess_abc123"));
  }

  @Test
  public void testSessionList() throws Exception {
    enqueue("session_list_response");
    CursorPage<Session> page =
        new Sessions(null, null, null)
            .list(
                SessionListParam.builder()
                    .limit(10)
                    .agentId("agent_xyz")
                    .statuses(Arrays.asList("idle", "running"))
                    .build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    String path = req.getPath();
    assertTrue(path.contains("limit=10"));
    assertTrue(path.contains("agent_id=agent_xyz"));
    assertTrue(path.contains("statuses[]=idle"));
    assertEquals(2, page.getData().size());
    assertEquals("cursor_abc", page.getNextPage());
    assertTrue(page.hasNext());
  }

  @Test
  public void testSessionDelete() throws Exception {
    enqueue("delete_response");
    AgentStudioDeletionStatus status = new Sessions(null, null, null).delete("sess_abc123");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("DELETE", req.getMethod());
    assertEquals("sess_abc123", status.getId());
    assertTrue(status.isDeleted());
  }

  @Test
  public void testSessionArchive() throws Exception {
    enqueue("session_response");
    Session session = new Sessions(null, null, null).archive("sess_abc123");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().contains("/sessions/sess_abc123/archive"));
  }

  @Test
  public void testSessionRetrieveRequiresId() {
    assertThrows(ApiException.class, () -> new Sessions(null, null, null).retrieve(null));
  }

  @Test
  public void testSessionListParamQueryString() {
    String qs =
        SessionListParam.builder()
            .limit(5)
            .page("cursor_abc")
            .agentId("agent_1")
            .statuses(Arrays.asList("idle", "running"))
            .createdAtGt("2025-01-01")
            .build()
            .toQueryString();
    assertTrue(qs.contains("limit=5"));
    assertTrue(qs.contains("page=cursor_abc"));
    assertTrue(qs.contains("agent_id=agent_1"));
    assertTrue(qs.contains("statuses[]=idle"));
    assertTrue(qs.contains("created_at[gt]=2025-01-01"));
  }

  @Test
  public void testSessionListParamEmptyQuery() {
    assertEquals("", SessionListParam.builder().build().toQueryString());
  }

  @Test
  public void testSessionModelDeserialization() {
    String json = JsonUtils.toJson(fixtures.get("session_response").getAsJsonObject());
    Session session = JsonUtils.fromJson(json, Session.class);
    assertEquals("sess_abc123", session.getId());
    assertEquals("Test Session", session.getTitle());
    assertNotNull(session.getAgent());
    assertEquals("agent_xyz", session.getAgent().getId());
    assertEquals("test-agent", session.getAgent().getName());
    assertEquals(Integer.valueOf(1), session.getAgentVersion());
    assertEquals("env_001", session.getEnvironmentId());
    assertNotNull(session.getVaultIds());
    assertTrue(session.getVaultIds().isEmpty());
  }

  @Test
  public void testSessionEventList() throws Exception {
    enqueue("event_list_response");
    CursorPage<Message> page =
        new Sessions(null, null, null)
            .events()
            .list("sess_abc123", SessionEventListParam.builder().limit(10).build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/sessions/sess_abc123/events"));
    assertEquals(2, page.getData().size());
    Message msg = page.getData().get(0);
    assertEquals("evt_1", msg.getId());
    assertEquals("user", msg.getRole());
    assertEquals("event", msg.getObject());
    assertEquals("completed", msg.getStatus());
    assertEquals(Long.valueOf(1), msg.getSequenceNumber());
    assertTrue(msg.getContent().get(0) instanceof ContentBlock.Text);
    assertEquals("hello", ((ContentBlock.Text) msg.getContent().get(0)).getText());
  }

  // ======================== Environments ========================

  @Test
  public void testEnvironmentCreate() throws Exception {
    enqueue("environment_response");
    Environment env =
        new Environments(null, null, null)
            .create(
                EnvironmentCreateParam.builder()
                    .name("Test Environment")
                    .config(Collections.singletonMap("type", "cloud"))
                    .build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().endsWith("/environments"));
    assertEquals("env_001", env.getId());
    assertEquals("Test Environment", env.getName());
  }

  @Test
  public void testEnvironmentRetrieve() throws Exception {
    enqueue("environment_response");
    Environment env = new Environments(null, null, null).retrieve("env_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/environments/env_001"));
    assertEquals("env_001", env.getId());
  }

  @Test
  public void testEnvironmentList() throws Exception {
    enqueue("environment_list_response");
    CursorPage<Environment> page =
        new Environments(null, null, null).list(EnvironmentListParam.builder().limit(20).build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertEquals(2, page.getData().size());
    assertNull(page.getNextPage());
  }

  @Test
  public void testEnvironmentDelete() throws Exception {
    enqueue("delete_response");
    AgentStudioDeletionStatus status = new Environments(null, null, null).delete("env_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("DELETE", req.getMethod());
    assertTrue(req.getPath().contains("/environments/env_001"));
    assertTrue(status.isDeleted());
  }

  // ======================== Skills ========================

  @Test
  public void testSkillCreate() throws Exception {
    enqueue("skill_response");
    Skill skill =
        new Skills(null, null, null, null)
            .create(SkillCreateParam.builder().fileId("file_001").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().endsWith("/skills"));
    assertEquals("skill_001", skill.getId());
    assertEquals("bash_tool", skill.getName());
  }

  @Test
  public void testSkillRetrieve() throws Exception {
    enqueue("skill_response");
    Skill skill = new Skills(null, null, null, null).retrieve("skill_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/skills/skill_001"));
    assertEquals("skill_001", skill.getId());
  }

  @Test
  public void testSkillList() throws Exception {
    enqueue("skill_list_response");
    CursorPage<Skill> page =
        new Skills(null, null, null, null).list(SkillListParam.builder().build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertEquals(1, page.getData().size());
  }

  @Test
  public void testSkillDelete() throws Exception {
    enqueue("delete_response");
    AgentStudioDeletionStatus status = new Skills(null, null, null, null).delete("skill_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("DELETE", req.getMethod());
    assertTrue(status.isDeleted());
  }

  @Test
  public void testSkillVersionCreate() throws Exception {
    enqueue("skill_version_response");
    SkillVersion sv =
        new Skills(null, null, null, null)
            .createVersion("skill_001", SkillCreateParam.builder().fileId("file_002").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().contains("/skills/skill_001/versions"));
    assertEquals("sv_001", sv.getId());
  }

  @Test
  public void testSkillVersionList() throws Exception {
    enqueue("skill_list_response");
    CursorPage<SkillVersion> page =
        new Skills(null, null, null, null)
            .listVersions("skill_001", SkillListParam.builder().build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/skills/skill_001/versions"));
  }

  // ======================== ContentBlock ========================

  @Test
  public void testContentBlockText() {
    ContentBlock block =
        JsonUtils.fromJson(
            "{\"type\":\"text\",\"text\":\"hello world\","
                + "\"citations\":[{\"url\":\"http://example.com\"}]}",
            ContentBlock.class);
    assertTrue(block instanceof ContentBlock.Text);
    ContentBlock.Text text = (ContentBlock.Text) block;
    assertEquals("hello world", text.getText());
    assertEquals(1, text.getCitations().size());
  }

  @Test
  public void testContentBlockImage() {
    ContentBlock block =
        JsonUtils.fromJson(
            "{\"type\":\"image\",\"image_url\":\"http://img.png\","
                + "\"file_id\":\"f1\",\"media_type\":\"image/png\"}",
            ContentBlock.class);
    assertTrue(block instanceof ContentBlock.Image);
    ContentBlock.Image img = (ContentBlock.Image) block;
    assertEquals("http://img.png", img.getImageUrl());
    assertEquals("f1", img.getFileId());
  }

  @Test
  public void testContentBlockData() {
    ContentBlock block =
        JsonUtils.fromJson(
            "{\"type\":\"data\",\"data\":{\"key\":\"val\"}," + "\"name\":\"test\"}",
            ContentBlock.class);
    assertTrue(block instanceof ContentBlock.DataContent);
    assertEquals("val", ((ContentBlock.DataContent) block).getData().get("key").getAsString());
  }

  @Test
  public void testContentBlockError() {
    ContentBlock block =
        JsonUtils.fromJson(
            "{\"type\":\"error\",\"error_code\":\"E001\"," + "\"message\":\"something failed\"}",
            ContentBlock.class);
    assertTrue(block instanceof ContentBlock.Error);
    assertEquals("E001", ((ContentBlock.Error) block).getErrorCode());
  }

  @Test
  public void testContentBlockRefusal() {
    ContentBlock block =
        JsonUtils.fromJson(
            "{\"type\":\"refusal\"," + "\"refusal\":\"I cannot do that\"}", ContentBlock.class);
    assertTrue(block instanceof ContentBlock.Refusal);
    assertEquals("I cannot do that", ((ContentBlock.Refusal) block).getRefusal());
  }

  @Test
  public void testContentBlockUnknownFallsBackToData() {
    ContentBlock block =
        JsonUtils.fromJson(
            "{\"type\":\"unknown_future_type\",\"data\":{\"x\":1}}", ContentBlock.class);
    assertTrue(block instanceof ContentBlock.DataContent);
  }

  // ======================== Vaults ========================

  @Test
  public void testVaultCreate() throws Exception {
    enqueue("vault_response");
    Vault vault =
        new Vaults(null, null, null)
            .create(VaultCreateParam.builder().displayName("Test Vault").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().endsWith("/vaults"));
    assertEquals("vault_001", vault.getId());
    assertEquals("Test Vault", vault.getDisplayName());
    assertEquals("vault", vault.getType());
  }

  @Test
  public void testVaultRetrieve() throws Exception {
    enqueue("vault_response");
    Vault vault = new Vaults(null, null, null).retrieve("vault_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/vaults/vault_001"));
    assertEquals("vault_001", vault.getId());
    assertNotNull(vault.getMetadata());
    assertEquals("security", vault.getMetadata().get("team"));
  }

  @Test
  public void testVaultUpdate() throws Exception {
    enqueue("vault_response");
    Vault vault =
        new Vaults(null, null, null)
            .update("vault_001", VaultUpdateParam.builder().displayName("Updated Vault").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().contains("/vaults/vault_001"));
    assertEquals("vault_001", vault.getId());
  }

  @Test
  public void testVaultList() throws Exception {
    enqueue("vault_list_response");
    CursorPage<Vault> page =
        new Vaults(null, null, null).list(VaultListParam.builder().limit(20).build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("limit=20"));
    assertEquals(2, page.getData().size());
    assertEquals("vault_001", page.getData().get(0).getId());
    assertEquals("cursor_vault", page.getNextPage());
    assertTrue(page.hasNext());
  }

  @Test
  public void testVaultDelete() throws Exception {
    enqueue("delete_response");
    AgentStudioDeletionStatus status = new Vaults(null, null, null).delete("vault_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("DELETE", req.getMethod());
    assertTrue(req.getPath().contains("/vaults/vault_001"));
    assertTrue(status.isDeleted());
  }

  @Test
  public void testVaultArchive() throws Exception {
    enqueue("vault_response");
    Vault vault = new Vaults(null, null, null).archive("vault_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().contains("/vaults/vault_001/archive"));
  }

  @Test
  public void testVaultRetrieveRequiresId() {
    assertThrows(ApiException.class, () -> new Vaults(null, null, null).retrieve(null));
  }

  // ======================== Credentials ========================

  @Test
  public void testCredentialCreate() throws Exception {
    enqueue("credential_response");
    JsonObject auth = new JsonObject();
    auth.addProperty("type", "environment_variable");
    auth.addProperty("secret_name", "DASHSCOPE_API_KEY");
    auth.addProperty("secret_value", "sk-test");
    Credential cred =
        new Vaults(null, null, null)
            .credentials()
            .create(
                "vault_001",
                CredentialCreateParam.builder().auth(auth).displayName("Test Credential").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().endsWith("/vaults/vault_001/credentials"));
    assertEquals("cred_001", cred.getId());
    assertEquals("vault_001", cred.getVaultId());
    assertEquals("Test Credential", cred.getDisplayName());
  }

  @Test
  public void testCredentialRetrieve() throws Exception {
    enqueue("credential_response");
    Credential cred = new Vaults(null, null, null).credentials().retrieve("vault_001", "cred_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/vaults/vault_001/credentials/cred_001"));
    assertEquals("cred_001", cred.getId());
    assertNotNull(cred.getAuth());
    assertEquals("environment_variable", cred.getAuth().getType());
  }

  @Test
  public void testCredentialUpdate() throws Exception {
    enqueue("credential_response");
    Credential cred =
        new Vaults(null, null, null)
            .credentials()
            .update(
                "vault_001",
                "cred_001",
                CredentialUpdateParam.builder().displayName("Updated Cred").build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().contains("/vaults/vault_001/credentials/cred_001"));
  }

  @Test
  public void testCredentialList() throws Exception {
    enqueue("credential_list_response");
    CursorPage<Credential> page =
        new Vaults(null, null, null)
            .credentials()
            .list("vault_001", CredentialListParam.builder().build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/vaults/vault_001/credentials"));
    assertEquals(2, page.getData().size());
    assertEquals("cred_001", page.getData().get(0).getId());
    assertNull(page.getNextPage());
  }

  @Test
  public void testCredentialDelete() throws Exception {
    enqueue("delete_response");
    AgentStudioDeletionStatus status =
        new Vaults(null, null, null).credentials().delete("vault_001", "cred_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("DELETE", req.getMethod());
    assertTrue(req.getPath().contains("/vaults/vault_001/credentials/cred_001"));
    assertTrue(status.isDeleted());
  }

  @Test
  public void testCredentialArchive() throws Exception {
    enqueue("credential_response");
    Credential cred = new Vaults(null, null, null).credentials().archive("vault_001", "cred_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().contains("/vaults/vault_001/credentials/cred_001/archive"));
  }

  @Test
  public void testCredentialCreateRequiresVaultId() {
    JsonObject auth = new JsonObject();
    auth.addProperty("type", "environment_variable");
    auth.addProperty("secret_name", "TEST_KEY");
    auth.addProperty("secret_value", "test-val");
    assertThrows(
        ApiException.class,
        () ->
            new Vaults(null, null, null)
                .credentials()
                .create(null, CredentialCreateParam.builder().auth(auth).build()));
  }

  @Test
  public void testCredentialRetrieveRequiresIds() {
    assertThrows(
        ApiException.class,
        () -> new Vaults(null, null, null).credentials().retrieve("vault_001", null));
  }

  @Test
  public void testVaultListWithKeyword() {
    String qs = VaultListParam.builder().keyword("my-vault").limit(10).build().toQueryString();
    assertTrue(qs.contains("keyword=my-vault"));
    assertTrue(qs.contains("limit=10"));
  }

  // ======================== Files ========================

  @Test
  public void testFileUpload() throws Exception {
    String baseUrl = String.format("http://127.0.0.1:%s/api/v1/agentstudio", mockServer.getPort());
    com.alibaba.dashscope.agentstudio.resource.Files filesResource =
        new com.alibaba.dashscope.agentstudio.resource.Files(baseUrl, null, null);
    try {
      JsonObject rsp = fixtures.get("file_response").getAsJsonObject();
      mockServer.enqueue(TestUtils.createMockResponse(JsonUtils.toJson(rsp), 200));
      java.io.File tempFile = java.io.File.createTempFile("test-upload", ".txt");
      tempFile.deleteOnExit();
      java.nio.file.Files.write(tempFile.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
      AgentStudioFile file = filesResource.upload(tempFile.getAbsolutePath(), "text/plain");
      RecordedRequest req = mockServer.takeRequest();
      assertEquals("POST", req.getMethod());
      assertTrue(req.getPath().endsWith("/files"));
      assertTrue(req.getHeader("Content-Type").startsWith("multipart/form-data"));
      assertEquals("file_001", file.getId());
      assertEquals("test.pdf", file.getFilename());
      assertEquals("application/pdf", file.getMimeType());
      assertEquals(Long.valueOf(12345), file.getSizeBytes());
    } finally {
      filesResource.close();
    }
  }

  @Test
  public void testFileUploadInputStream() throws Exception {
    String baseUrl = String.format("http://127.0.0.1:%s/api/v1/agentstudio", mockServer.getPort());
    com.alibaba.dashscope.agentstudio.resource.Files filesResource =
        new com.alibaba.dashscope.agentstudio.resource.Files(baseUrl, null, null);
    try {
      JsonObject rsp = fixtures.get("file_response").getAsJsonObject();
      mockServer.enqueue(TestUtils.createMockResponse(JsonUtils.toJson(rsp), 200));
      final boolean[] closed = {false};
      InputStream is =
          new ByteArrayInputStream("test content".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
              closed[0] = true;
              super.close();
            }
          };
      AgentStudioFile file = filesResource.upload("test.txt", is, "text/plain");
      mockServer.takeRequest();
      assertEquals("file_001", file.getId());
      assertTrue(closed[0], "InputStream should be closed after upload");
    } finally {
      filesResource.close();
    }
  }

  @Test
  public void testFileRetrieve() throws Exception {
    enqueue("file_response");
    com.alibaba.dashscope.agentstudio.resource.Files filesResource =
        new com.alibaba.dashscope.agentstudio.resource.Files(null, null, null);
    AgentStudioFile file = filesResource.retrieve("file_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("/files/file_001"));
    assertEquals("file_001", file.getId());
    assertEquals("completed", file.getStatus());
  }

  @Test
  public void testFileList() throws Exception {
    enqueue("file_list_response");
    com.alibaba.dashscope.agentstudio.resource.Files filesResource =
        new com.alibaba.dashscope.agentstudio.resource.Files(null, null, null);
    CursorPage<AgentStudioFile> page =
        filesResource.list(FileListParam.builder().limit(20).build());
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("GET", req.getMethod());
    assertTrue(req.getPath().contains("limit=20"));
    assertEquals(2, page.getData().size());
    assertEquals("file_001", page.getData().get(0).getId());
    assertEquals("cursor_file", page.getNextPage());
  }

  @Test
  public void testFileDelete() throws Exception {
    enqueue("delete_response");
    com.alibaba.dashscope.agentstudio.resource.Files filesResource =
        new com.alibaba.dashscope.agentstudio.resource.Files(null, null, null);
    AgentStudioDeletionStatus status = filesResource.delete("file_001");
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("DELETE", req.getMethod());
    assertTrue(req.getPath().contains("/files/file_001"));
    assertTrue(status.isDeleted());
  }

  @Test
  public void testFileUploadNullFilePath() {
    com.alibaba.dashscope.agentstudio.resource.Files filesResource =
        new com.alibaba.dashscope.agentstudio.resource.Files(null, null, null);
    assertThrows(ApiException.class, () -> filesResource.upload(null, null));
  }

  @Test
  public void testFileUploadNullInputStream() {
    com.alibaba.dashscope.agentstudio.resource.Files filesResource =
        new com.alibaba.dashscope.agentstudio.resource.Files(null, null, null);
    assertThrows(
        ApiException.class, () -> filesResource.upload("test.txt", (InputStream) null, null));
  }

  @Test
  public void testFileRetrieveRequiresId() {
    com.alibaba.dashscope.agentstudio.resource.Files filesResource =
        new com.alibaba.dashscope.agentstudio.resource.Files(null, null, null);
    assertThrows(ApiException.class, () -> filesResource.retrieve(null));
  }

  @Test
  public void testFileDeleteRequiresId() {
    com.alibaba.dashscope.agentstudio.resource.Files filesResource =
        new com.alibaba.dashscope.agentstudio.resource.Files(null, null, null);
    assertThrows(ApiException.class, () -> filesResource.delete(null));
  }

  // ======================== AgentStudioClient ========================

  @Test
  public void testAgentStudioClientBuilder() {
    String baseUrl = String.format("http://127.0.0.1:%s/api/v1/agentstudio", mockServer.getPort());
    AgentStudioClient client =
        AgentStudioClient.builder().apiKey("test-key").baseUrl(baseUrl).build();
    try {
      assertNotNull(client.agents());
      assertNotNull(client.sessions());
      assertNotNull(client.environments());
      assertNotNull(client.skills());
      assertNotNull(client.vaults());
      assertNotNull(client.files());
      assertEquals(baseUrl, client.getBaseUrl());
    } finally {
      client.close();
    }
  }

  @Test
  public void testAgentStudioClientClose() {
    String baseUrl = String.format("http://127.0.0.1:%s/api/v1/agentstudio", mockServer.getPort());
    AgentStudioClient client =
        AgentStudioClient.builder().apiKey("test-key").baseUrl(baseUrl).build();
    client.close();
  }

  // ======================== SessionEvents.send() ========================

  @Test
  public void testSessionEventSend() throws Exception {
    enqueue("session_event_send_response");
    JsonObject event = ClientEvents.userMessage("hello");
    JsonObject result =
        new Sessions(null, null, null)
            .events()
            .send("sess_abc123", Collections.singletonList(event));
    RecordedRequest req = mockServer.takeRequest();
    assertEquals("POST", req.getMethod());
    assertTrue(req.getPath().contains("/sessions/sess_abc123/events"));
    assertNotNull(result);
  }

  @Test
  public void testSessionEventSendRequiresSessionId() {
    assertThrows(
        ApiException.class,
        () ->
            new Sessions(null, null, null)
                .events()
                .send(null, Collections.singletonList(new JsonObject())));
  }

  @Test
  public void testSessionEventSendRequiresEvents() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Sessions(null, null, null).events().send("sess_abc123", Collections.emptyList()));
  }

  // ======================== URL Encoding ========================

  @Test
  public void testUrlEncodingInQueryParams() {
    String qs =
        SessionListParam.builder().page("cursor with spaces&special=chars").build().toQueryString();
    assertTrue(qs.contains("page=cursor+with+spaces%26special%3Dchars"));
  }

  @Test
  public void testUrlEncodingInSessionEventListParam() {
    String qs =
        SessionEventListParam.builder()
            .createdAtGt("2025-01-01T00:00:00+08:00")
            .build()
            .toQueryString();
    assertTrue(qs.contains("created_at[gt]=2025-01-01T00%3A00%3A00%2B08%3A00"));
  }

  // ======================== EnvironmentCreateParam scope type ========================

  @Test
  public void testEnvironmentCreateWithScopeAsString() {
    JsonObject body =
        EnvironmentCreateParam.builder()
            .name("env-test")
            .config(Collections.singletonMap("type", "cloud"))
            .scope("organization")
            .build()
            .getHttpBody();
    assertEquals("organization", body.get("scope").getAsString());
    assertTrue(body.get("scope").isJsonPrimitive());
  }
}
