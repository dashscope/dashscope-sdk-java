package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.agentstudio.model.Memory;
import com.alibaba.dashscope.agentstudio.model.MemoryStore;
import com.alibaba.dashscope.agentstudio.model.MemoryVersion;
import com.alibaba.dashscope.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.agentstudio.param.MemoryCreateParam;
import com.alibaba.dashscope.agentstudio.param.MemoryListParam;
import com.alibaba.dashscope.agentstudio.param.MemoryStoreCreateParam;
import com.alibaba.dashscope.agentstudio.param.MemoryStoreListParam;
import com.alibaba.dashscope.agentstudio.param.MemoryStoreUpdateParam;
import com.alibaba.dashscope.agentstudio.param.MemoryUpdateParam;
import com.alibaba.dashscope.agentstudio.param.MemoryVersionListParam;
import com.alibaba.dashscope.exception.ApiException;
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
public class TestAgentStudioMemoryStores {
  private static MockWebServer mockServer;
  private static JsonObject fixtures;
  private static String baseUrl;
  private static AgentStudioClient client;

  @BeforeAll
  public static void before() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start();
    baseUrl = mockServer.url("/api/v1/agentstudio/").toString();
    byte[] content = Files.readAllBytes(Paths.get("./src/test/resources/agentstudio.json"));
    fixtures = JsonUtils.parse(new String(content, StandardCharsets.UTF_8));
    client = new AgentStudioClient("test-key", baseUrl, "ws_01", null, null);
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
  public void testStoreCreatePostsBody() throws Exception {
    enqueue("memory_store_response");
    MemoryStore store =
        client
            .memoryStores()
            .create(
                MemoryStoreCreateParam.builder()
                    .name("User Preferences")
                    .description("Per-user preferences.")
                    .metadata(Collections.singletonMap("owner", "demo"))
                    .build());
    RecordedRequest request = mockServer.takeRequest();
    JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().endsWith("/memory_stores"));
    assertEquals("User Preferences", body.get("name").getAsString());
    assertEquals("Per-user preferences.", body.get("description").getAsString());
    assertEquals("demo", body.getAsJsonObject("metadata").get("owner").getAsString());
    assertEquals("memstore_01Hx", store.getId());
    assertEquals("memory_store", store.getType());
    assertNull(store.getArchivedAt());
  }

  @Test
  public void testStoreUpdatePartialBody() throws Exception {
    enqueue("memory_store_response");
    client
        .memoryStores()
        .update("memstore_01Hx", MemoryStoreUpdateParam.builder().name("Renamed").build());
    RecordedRequest request = mockServer.takeRequest();
    JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().endsWith("/memory_stores/memstore_01Hx"));
    assertEquals(1, body.size());
    assertEquals("Renamed", body.get("name").getAsString());
  }

  @Test
  public void testStoreUpdateEmptyMetadataClears() throws Exception {
    // metadata is replaced wholesale: an empty map must reach the wire
    // (an isEmpty() guard would silently turn "clear" into a no-op).
    enqueue("memory_store_response");
    client
        .memoryStores()
        .update(
            "memstore_01Hx",
            MemoryStoreUpdateParam.builder().metadata(Collections.emptyMap()).build());
    RecordedRequest request = mockServer.takeRequest();
    JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
    assertEquals("POST", request.getMethod());
    assertTrue(body.has("metadata"));
    assertEquals(0, body.getAsJsonObject("metadata").size());
  }

  @Test
  public void testStoreListQueryParams() throws Exception {
    enqueue("memory_store_list_response");
    client
        .memoryStores()
        .list(MemoryStoreListParam.builder().includeArchived(true).limit(10).page("tok_1").build());
    RecordedRequest request = mockServer.takeRequest();
    String path = request.getPath();
    assertEquals("GET", request.getMethod());
    assertTrue(path.split("\\?")[0].endsWith("/memory_stores"));
    assertTrue(path.contains("include_archived=true"));
    assertTrue(path.contains("limit=10"));
    assertTrue(path.contains("page=tok_1"));
  }

  @Test
  public void testStoreArchiveAndDelete() throws Exception {
    enqueue("memory_store_response");
    MemoryStore archived = client.memoryStores().archive("memstore_01Hx");
    RecordedRequest archiveRequest = mockServer.takeRequest();
    assertEquals("POST", archiveRequest.getMethod());
    assertTrue(archiveRequest.getPath().endsWith("/memory_stores/memstore_01Hx/archive"));
    assertNotNull(archived);

    enqueue("delete_response");
    AgentStudioDeletionStatus deleted = client.memoryStores().delete("memstore_01Hx");
    RecordedRequest deleteRequest = mockServer.takeRequest();
    assertEquals("DELETE", deleteRequest.getMethod());
    assertTrue(deleteRequest.getPath().endsWith("/memory_stores/memstore_01Hx"));
    assertTrue(deleted.isDeleted());
  }

  @Test
  public void testMemoryCreateAndRetrieve() throws Exception {
    enqueue("memory_created_response");
    Memory memory =
        client
            .memoryStores()
            .memories()
            .create(
                "memstore_01Hx",
                MemoryCreateParam.builder()
                    .path("/formatting.md")
                    .content("Use GAAP formatting.")
                    .build());
    RecordedRequest request = mockServer.takeRequest();
    JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().endsWith("/memory_stores/memstore_01Hx/memories"));
    assertEquals("/formatting.md", body.get("path").getAsString());
    assertEquals("Use GAAP formatting.", body.get("content").getAsString());
    assertEquals("mem_01Hy", memory.getId());
    assertEquals("memstore_01Hx", memory.getMemoryStoreId());
    // Create responses never include content (probe-verified wire shape).
    assertNull(memory.getContent());

    enqueue("memory_response");
    Memory retrieved = client.memoryStores().memories().retrieve("memstore_01Hx", "mem_01Hy");
    RecordedRequest retrieveRequest = mockServer.takeRequest();
    assertEquals("GET", retrieveRequest.getMethod());
    assertTrue(
        retrieveRequest.getPath().endsWith("/memory_stores/memstore_01Hx/memories/mem_01Hy"));
    assertEquals("Use GAAP formatting.", retrieved.getContent());
  }

  @Test
  public void testMemoryUpdateFlattenedPrecondition() throws Exception {
    enqueue("memory_created_response");
    client
        .memoryStores()
        .memories()
        .update(
            "memstore_01Hx",
            "mem_01Hy",
            MemoryUpdateParam.builder().content("CORRECTED").contentSha256("abc123").build());
    RecordedRequest request = mockServer.takeRequest();
    JsonObject body = JsonUtils.parse(request.getBody().readUtf8());
    assertEquals("POST", request.getMethod());
    assertTrue(request.getPath().endsWith("/memory_stores/memstore_01Hx/memories/mem_01Hy"));
    assertEquals("CORRECTED", body.get("content").getAsString());
    assertEquals("abc123", body.get("content_sha256").getAsString());
  }

  @Test
  public void testMemoryListAutoPaginates() throws Exception {
    enqueue("memory_list_response");
    enqueue("memory_list_page2_response");
    CursorPage<Memory> page =
        client
            .memoryStores()
            .memories()
            .list(
                "memstore_01Hx",
                MemoryListParam.builder().pathPrefix("/").depth(1).view("basic").build());
    RecordedRequest request = mockServer.takeRequest();
    String path = request.getPath();
    assertEquals("GET", request.getMethod());
    assertTrue(path.split("\\?")[0].endsWith("/memory_stores/memstore_01Hx/memories"));
    assertTrue(path.contains("path_prefix=%2F"));
    assertTrue(path.contains("depth=1"));
    assertTrue(path.contains("view=basic"));
    // Native depth=1: gson fills prefixes from the envelope.
    assertEquals(Collections.singletonList("/notes/"), page.getPrefixes());

    // Iterating drains the queued second page; the fetchNext closure must
    // rebuild the full filter set with the cursor.
    java.util.List<String> ids = new java.util.ArrayList<>();
    for (Memory item : page) {
      ids.add(item.getId());
    }
    assertEquals(java.util.Arrays.asList("mem_01Hy", "mem_02"), ids);
    RecordedRequest second = mockServer.takeRequest();
    assertTrue(second.getPath().contains("page=tok_2"));
    assertTrue(second.getPath().contains("path_prefix=%2F"));
    assertTrue(second.getPath().contains("depth=1"));
  }

  @Test
  public void testMemoryListDepthTwoEmulatedAcrossPages() throws Exception {
    enqueue("memory_flat_list_response");
    enqueue("memory_flat_list_page2_response");
    CursorPage<Memory> page =
        client
            .memoryStores()
            .memories()
            .list("memstore_01Hx", MemoryListParam.builder().depth(2).build());
    RecordedRequest request = mockServer.takeRequest();
    // depth >= 2 must not reach the wire; the view is derived locally.
    assertFalse(request.getPath().contains("depth="));

    // Page 1: the 3-segment file is cut. Page 2: adds one 1-segment file,
    // drops a 3-segment one; the second request still carries no depth
    // (fetchNext re-enters the emulation with the original depth).
    java.util.List<String> paths = new java.util.ArrayList<>();
    for (Memory item : page) {
      paths.add(item.getPath());
    }
    assertEquals(java.util.Arrays.asList("/root.md", "/notes/top.md", "/flat2.md"), paths);
    // Per-page synthesized prefixes: the cut 3-segment file leaves its
    // frontier directory.
    assertEquals(java.util.Arrays.asList("/notes/", "/notes/deep/"), page.getPrefixes());
    RecordedRequest second = mockServer.takeRequest();
    assertFalse(second.getPath().contains("depth="));
  }

  @Test
  public void testMemoryListDepthOnePassesThrough() throws Exception {
    enqueue("memory_flat_list_response");
    client
        .memoryStores()
        .memories()
        .list("memstore_01Hx", MemoryListParam.builder().depth(1).build());
    RecordedRequest request = mockServer.takeRequest();
    assertTrue(request.getPath().contains("depth=1"));
  }

  @Test
  public void testMemoryErrorPathParsesNestedErrorBody() throws Exception {
    // Live-wire shape: nested error envelope on a 409. OkHttpHttpClient's
    // parseFailedJson fallback must surface code/message to the user.
    String body =
        "{\"type\":\"error\",\"error\":{\"code\":\"memory_path_conflict_error\","
            + "\"message\":\"Memory path already exists.\"},\"request_id\":\"req-e1\"}";
    mockServer.enqueue(TestUtils.createMockResponse(body, 409));
    ApiException ex =
        assertThrows(
            ApiException.class,
            () ->
                client
                    .memoryStores()
                    .memories()
                    .create(
                        "memstore_01Hx",
                        MemoryCreateParam.builder()
                            .path("/e2e/notes.md")
                            .content("duplicate")
                            .build()));
    assertEquals(409, ex.getStatus().getStatusCode());
    assertEquals("memory_path_conflict_error", ex.getStatus().getCode());
    // Drain the recorded request: a test that never takeRequest()s leaves
    // the shared server's request queue off by one and misaligns every
    // later test's assertions.
    assertEquals("POST", mockServer.takeRequest().getMethod());
  }

  @Test
  public void testMemoryDelete() throws Exception {
    enqueue("memory_delete_response");
    AgentStudioDeletionStatus deleted =
        client.memoryStores().memories().delete("memstore_01Hx", "mem_01Hy");
    RecordedRequest request = mockServer.takeRequest();
    assertEquals("DELETE", request.getMethod());
    assertTrue(request.getPath().endsWith("/memory_stores/memstore_01Hx/memories/mem_01Hy"));
    assertEquals("memory_deleted", deleted.getType());
  }

  @Test
  public void testVersionListDottedTimeKeysAndRedact() throws Exception {
    enqueue("memory_version_list_response");
    client
        .memoryStores()
        .memoryVersions()
        .list(
            "memstore_01Hx",
            MemoryVersionListParam.builder()
                .memoryId("mem_01Hy")
                .operation("modified")
                .createdAtGte("2026-10-01T00:00:00Z")
                .createdAtLte("2026-10-31T00:00:00Z")
                .view("full")
                .build());
    RecordedRequest request = mockServer.takeRequest();
    String path = request.getPath();
    assertEquals("GET", request.getMethod());
    assertTrue(path.split("\\?")[0].endsWith("/memory_stores/memstore_01Hx/memory_versions"));
    assertTrue(path.contains("memory_id=mem_01Hy"));
    assertTrue(path.contains("operation=modified"));
    assertTrue(path.contains("created_at.gte=2026-10-01T00%3A00%3A00Z"));
    assertTrue(path.contains("created_at.lte=2026-10-31T00%3A00%3A00Z"));
    assertTrue(path.contains("view=full"));

    enqueue("memory_version_response");
    MemoryVersion version =
        client.memoryStores().memoryVersions().retrieve("memstore_01Hx", "memver_01Hz");
    RecordedRequest retrieveRequest = mockServer.takeRequest();
    assertEquals(
        "/api/v1/agentstudio/memory_stores/memstore_01Hx/memory_versions/memver_01Hz",
        retrieveRequest.getPath());

    enqueue("memory_version_response");
    MemoryVersion redacted =
        client.memoryStores().memoryVersions().redact("memstore_01Hx", "memver_01Hz");
    RecordedRequest redactRequest = mockServer.takeRequest();
    assertEquals("POST", redactRequest.getMethod());
    assertTrue(
        redactRequest
            .getPath()
            .endsWith("/memory_stores/memstore_01Hx/memory_versions/memver_01Hz/redact"));
    assertEquals("memver_01Hz", redacted.getId());
  }
}
