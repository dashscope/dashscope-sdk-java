// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.recipes.agentstudio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.applications.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.applications.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.applications.agentstudio.model.Memory;
import com.alibaba.dashscope.applications.agentstudio.model.MemoryStore;
import com.alibaba.dashscope.applications.agentstudio.model.MemoryVersion;
import com.alibaba.dashscope.applications.agentstudio.pagination.CursorPage;
import com.alibaba.dashscope.applications.agentstudio.param.MemoryCreateParam;
import com.alibaba.dashscope.applications.agentstudio.param.MemoryListParam;
import com.alibaba.dashscope.applications.agentstudio.param.MemoryStoreCreateParam;
import com.alibaba.dashscope.applications.agentstudio.param.MemoryStoreListParam;
import com.alibaba.dashscope.applications.agentstudio.param.MemoryUpdateParam;
import com.alibaba.dashscope.applications.agentstudio.param.MemoryVersionListParam;
import com.alibaba.dashscope.examples.ExampleSupport;

/**
 * Agent recipe: AgentStudio memory stores end-to-end. Flow: create store -> seed memory -> list
 * with content (view=full, auto-pagination) -> update with optimistic lock -> list versions ->
 * redact a version -> list stores -> delete store.
 *
 * <p>3.0 port of {@code legacy_2x/AgentStudioMemoryStores.java}: same flow under {@code
 * com.alibaba.dashscope.applications.agentstudio}, mounted via {@code client.agentStudio()}, plus
 * the {@code redact()} audit step. Unlike the frozen legacy copy, this file is a compiled example
 * source and runs in the offline dry-run suite.
 *
 * <p>Requires DASHSCOPE_API_KEY; DASHSCOPE_WORKSPACE selects the AgentStudio workspace (defaults to
 * trial).
 */
public final class AgentStudioMemoryStores {

  private AgentStudioMemoryStores() {}

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  /** Run the full memory-store flow. */
  public static void main(String[] args) {
    try (DashScopeClient client =
        DashScopeClient.builder()
            .apiKey(ExampleSupport.requireApiKey())
            .workspace(envOr("DASHSCOPE_WORKSPACE", "trial"))
            .build()) {
      AgentStudioClient studio = client.agentStudio();

      // 1. Create a store; description is read by the agent - write it for the model.
      System.out.println("==> creating memory store");
      MemoryStore store =
          studio
              .memoryStores()
              .create(
                  MemoryStoreCreateParam.builder()
                      .name("demo-preferences")
                      .description("Per-user preferences and project context.")
                      .build());
      System.out.println("   store_id=" + store.getId());

      // 2. Seed reference material before any session runs.
      System.out.println("==> seeding memory");
      Memory memory =
          studio
              .memoryStores()
              .memories()
              .create(
                  store.getId(),
                  MemoryCreateParam.builder()
                      .path("/formatting_standards.md")
                      .content("All reports use GAAP formatting.")
                      .build());
      System.out.printf(
          "   memory_id=%s path=%s size=%d%n",
          memory.getId(), memory.getPath(), memory.getContentSizeBytes());

      // 3. Read back with content (view=full; the default basic is metadata only). Print prefixes
      //    before iterating: the loop auto-paginates and prefixes are per page.
      System.out.println("==> listing memories (view=full)");
      CursorPage<Memory> page =
          studio
              .memoryStores()
              .memories()
              .list(store.getId(), MemoryListParam.builder().view("full").build());
      System.out.println("   prefixes=" + page.getPrefixes());
      for (Memory item : page) {
        System.out.printf("   %s -> %s%n", item.getPath(), item.getContent());
      }

      // 4. Update with the optimistic lock (contentSha256 from step 2).
      System.out.println("==> updating memory");
      Memory updated =
          studio
              .memoryStores()
              .memories()
              .update(
                  store.getId(),
                  memory.getId(),
                  MemoryUpdateParam.builder()
                      .content("All reports use GAAP formatting. ISO-8601 dates.")
                      .contentSha256(memory.getContentSha256())
                      .build());
      System.out.println("   updated_sha=" + updated.getContentSha256());

      // 5. Audit history: every mutation produced a memver_ snapshot (newest first).
      System.out.println("==> listing memory versions");
      String firstVersionId = null;
      for (MemoryVersion version :
          studio
              .memoryStores()
              .memoryVersions()
              .list(store.getId(), MemoryVersionListParam.builder().build())) {
        System.out.printf("   version=%s operation=%s%n", version.getId(), version.getOperation());
        if (firstVersionId == null) {
          firstVersionId = version.getId();
        }
      }

      // 6. Redact one snapshot: path/content/sha/size are cleared, the audit trail stays.
      if (firstVersionId != null) {
        System.out.println("==> redacting version " + firstVersionId);
        MemoryVersion redacted =
            studio.memoryStores().memoryVersions().redact(store.getId(), firstVersionId);
        System.out.printf(
            "   redacted_at=%s content=%s%n", redacted.getRedactedAt(), redacted.getContent());
      }

      // 7. List stores and clean up.
      System.out.println("==> listing stores");
      for (MemoryStore item :
          studio.memoryStores().list(MemoryStoreListParam.builder().limit(20).build())) {
        System.out.printf("   store=%s name=%s%n", item.getId(), item.getName());
      }
      AgentStudioDeletionStatus deleted = studio.memoryStores().delete(store.getId());
      System.out.println("   deleted=" + deleted.isDeleted());
      System.out.println("done.");
    }
  }
}
