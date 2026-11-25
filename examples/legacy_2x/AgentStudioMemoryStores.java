import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.agentstudio.model.AgentStudioDeletionStatus;
import com.alibaba.dashscope.agentstudio.model.Memory;
import com.alibaba.dashscope.agentstudio.model.MemoryStore;
import com.alibaba.dashscope.agentstudio.model.MemoryVersion;
import com.alibaba.dashscope.agentstudio.param.MemoryCreateParam;
import com.alibaba.dashscope.agentstudio.param.MemoryListParam;
import com.alibaba.dashscope.agentstudio.param.MemoryStoreCreateParam;
import com.alibaba.dashscope.agentstudio.param.MemoryStoreListParam;
import com.alibaba.dashscope.agentstudio.param.MemoryUpdateParam;
import com.alibaba.dashscope.agentstudio.param.MemoryVersionListParam;

/**
 * Memory store example: seed a store, read it back, audit versions, cleanup.
 *
 * <p>Set DASHSCOPE_API_KEY and DASHSCOPE_WORKSPACE before running.
 */
public class AgentStudioMemoryStores {

  public static void main(String[] args) {
    try (AgentStudioClient client = new AgentStudioClient()) {
      // 1. Create a store; description is read by the agent - write it for
      //    the model, not for humans.
      MemoryStore store =
          client
              .memoryStores()
              .create(
                  MemoryStoreCreateParam.builder()
                      .name("demo-preferences")
                      .description("Per-user preferences and project context.")
                      .build());
      System.out.printf("store: %s%n", store.getId());

      // 2. Seed reference material before any session runs.
      Memory memory =
          client
              .memoryStores()
              .memories()
              .create(
                  store.getId(),
                  MemoryCreateParam.builder()
                      .path("/formatting_standards.md")
                      .content("All reports use GAAP formatting.")
                      .build());
      System.out.printf(
          "memory: %s %s (%d bytes)%n",
          memory.getId(), memory.getPath(), memory.getContentSizeBytes());

      // 3. Read back with content (view=full; the default basic is metadata
      //    only). Print prefixes before iterating: the loop auto-paginates
      //    and prefixes are per page.
      com.alibaba.dashscope.agentstudio.pagination.CursorPage<Memory> page =
          client
              .memoryStores()
              .memories()
              .list(store.getId(), MemoryListParam.builder().view("full").build());
      System.out.printf("prefixes: %s%n", page.getPrefixes());
      for (Memory item : page) {
        System.out.printf("%s -> %s%n", item.getPath(), item.getContent());
      }

      // 4. Update with the optimistic lock (contentSha256 from step 2).
      Memory updated =
          client
              .memoryStores()
              .memories()
              .update(
                  store.getId(),
                  memory.getId(),
                  MemoryUpdateParam.builder()
                      .content("All reports use GAAP formatting. ISO-8601 dates.")
                      .contentSha256(memory.getContentSha256())
                      .build());
      System.out.printf("updated sha: %s%n", updated.getContentSha256());

      // 5. Audit history: every mutation produced a memver_ snapshot.
      for (MemoryVersion version :
          client
              .memoryStores()
              .memoryVersions()
              .list(store.getId(), MemoryVersionListParam.builder().build())) {
        System.out.printf("version: %s %s%n", version.getId(), version.getOperation());
      }

      // 6. List stores and clean up.
      for (MemoryStore item :
          client.memoryStores().list(MemoryStoreListParam.builder().limit(20).build())) {
        System.out.printf("store: %s %s%n", item.getId(), item.getName());
      }
      AgentStudioDeletionStatus deleted = client.memoryStores().delete(store.getId());
      System.out.printf("deleted: %s%n", deleted.getType());
    }
  }
}
