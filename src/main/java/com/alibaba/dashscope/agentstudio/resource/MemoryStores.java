// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
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
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

/**
 * Memory stores: workspace-scoped collections of small text documents that persist across sessions.
 * Stores hold {@code memories} ({@code mem_...}, one text file per path) and their {@code
 * memory_versions} ({@code memver_...}, immutable snapshots per mutation).
 *
 * <p>Follows the Claude Managed Agents {@code client.beta.memoryStores()} surface, with these
 * verified divergences (the backend is bailian-app-control {@code MemoryStoreController} over
 * bailian-app-context OTS storage):
 *
 * <ul>
 *   <li>updates use POST; the optimistic-lock hash rides the top-level {@code content_sha256} body
 *       field (CMA uses PATCH with a {@code precondition} object);
 *   <li>store {@code metadata} on update is replaced wholesale; CMA merge-patches it;
 *   <li>{@code view="full"} caps {@code limit} at 20 and rejects higher values with 400; CMA
 *       silently clamps;
 *   <li>directory nodes come back as a separate {@code prefixes} page field ({@code
 *       page.getPrefixes()}) instead of inline {@code memory_prefix} items in {@code data};
 *   <li>depth accepts 0/1 natively; larger values are derived client-side (see {@link
 *       #MAX_WIRE_DEPTH});
 *   <li>create and update responses never include {@code content};
 *   <li>no store-list time filters, no conditional memory delete, no version attribution fields or
 *       filters — CMA has all of them.
 * </ul>
 */
public final class MemoryStores {
  // The backend accepts only depth null/0 (flat) or 1 (single-level tree);
  // anything else returns 400 (CMA likewise defines only 0/1). depth >= 2 is
  // this SDK's own generalization over the flat listing.
  static final int MAX_WIRE_DEPTH = 1;

  private final GeneralApi<HalfDuplexParamBase> api;
  private final String baseUrl;
  private final String apiKey;
  private final Memories memories;
  private final MemoryVersions memoryVersions;

  public MemoryStores(String baseUrl, ConnectionOptions connectionOptions, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.api = connectionOptions != null ? new GeneralApi<>(connectionOptions) : new GeneralApi<>();
    this.memories = new Memories();
    this.memoryVersions = new MemoryVersions();
  }

  public Memories memories() {
    return memories;
  }

  public MemoryVersions memoryVersions() {
    return memoryVersions;
  }

  public MemoryStore create(MemoryStoreCreateParam param) {
    return AsyncHelper.joinAndUnwrap(createAsync(param));
  }

  public MemoryStore retrieve(String storeId) {
    return retrieve(storeId, null, null);
  }

  public MemoryStore retrieve(String storeId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(retrieveAsync(storeId, apiKey, headers));
  }

  public MemoryStore update(String storeId, MemoryStoreUpdateParam param) {
    return AsyncHelper.joinAndUnwrap(updateAsync(storeId, param));
  }

  public CursorPage<MemoryStore> list(MemoryStoreListParam param) {
    return AsyncHelper.joinAndUnwrap(listAsync(param));
  }

  public MemoryStore archive(String storeId) {
    return AsyncHelper.joinAndUnwrap(archiveAsync(storeId));
  }

  public AgentStudioDeletionStatus delete(String storeId) {
    return delete(storeId, null, null);
  }

  public AgentStudioDeletionStatus delete(
      String storeId, String apiKey, Map<String, String> headers) {
    return AsyncHelper.joinAndUnwrap(deleteAsync(storeId, apiKey, headers));
  }

  public CompletableFuture<MemoryStore> createAsync(MemoryStoreCreateParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(HttpMethod.POST, "memory_stores", baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, MemoryStore.class));
  }

  public CompletableFuture<MemoryStore> retrieveAsync(String storeId) {
    return retrieveAsync(storeId, null, null);
  }

  public CompletableFuture<MemoryStore> retrieveAsync(
      String storeId, String apiKey, Map<String, String> headers) {
    if (storeId == null || storeId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("storeId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.GET, StringUtils.format("memory_stores/%s", storeId), baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, MemoryStore.class));
  }

  public CompletableFuture<MemoryStore> updateAsync(String storeId, MemoryStoreUpdateParam param) {
    if (storeId == null || storeId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("storeId is required!"));
    }
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("memory_stores/%s", storeId), baseUrl);
    return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, MemoryStore.class));
  }

  public CompletableFuture<CursorPage<MemoryStore>> listAsync(MemoryStoreListParam param) {
    if (param == null) {
      return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
    }
    String query = param.toQueryString();
    String path = query.isEmpty() ? "memory_stores" : "memory_stores?" + query;
    GeneralServiceOption opt = AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
    return AsyncHelper.callAsync(
            api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
        .thenApply(
            r -> {
              Type type = new TypeToken<CursorPage<MemoryStore>>() {}.getType();
              CursorPage<MemoryStore> page = FlattenResultBase.fromDashScopeResult(r, type);
              page.setFetchNext(
                  cursor ->
                      listAsync(
                          MemoryStoreListParam.builder()
                              .limit(param.getLimit())
                              .includeArchived(param.getIncludeArchived())
                              .page(cursor)
                              .build()));
              return page;
            });
  }

  public CompletableFuture<MemoryStore> archiveAsync(String storeId) {
    if (storeId == null || storeId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("storeId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.POST, StringUtils.format("memory_stores/%s/archive", storeId), baseUrl);
    return AsyncHelper.callAsync(
            api,
            AgentStudioConstants.withApiKey(apiKey, MemoryStoreUpdateParam.builder().build()),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, MemoryStore.class));
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(String storeId) {
    return deleteAsync(storeId, null, null);
  }

  public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
      String storeId, String apiKey, Map<String, String> headers) {
    if (storeId == null || storeId.isEmpty()) {
      return AsyncHelper.failedFuture(new InputRequiredException("storeId is required!"));
    }
    GeneralServiceOption opt =
        AgentStudioConstants.newServiceOption(
            HttpMethod.DELETE, StringUtils.format("memory_stores/%s", storeId), baseUrl);
    String resolvedKey = apiKey != null ? apiKey : this.apiKey;
    return AsyncHelper.callAsync(
            api,
            GeneralGetParam.builder()
                .apiKey(resolvedKey)
                .headers(headers != null ? headers : new HashMap<>())
                .build(),
            opt)
        .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, AgentStudioDeletionStatus.class));
  }

  /**
   * Depth emulation: files more than {@code depth} segments below {@code pathPrefix} are dropped
   * and directory prefixes are synthesized — files cut by the bound still contribute their frontier
   * directory, matching the native {@code depth=1} behavior. Kept files and prefixes are both per
   * page.
   */
  private static void filterByDepth(CursorPage<Memory> page, int depth, String pathPrefix) {
    String base = pathPrefix == null || pathPrefix.isEmpty() ? "/" : pathPrefix;
    if (!base.endsWith("/")) {
      base = base + "/";
    }
    List<Memory> kept = new ArrayList<>();
    TreeSet<String> dirs = new TreeSet<>();
    List<Memory> data = page.getData() != null ? page.getData() : Collections.emptyList();
    for (Memory memory : data) {
      String path = memory.getPath() == null ? "" : memory.getPath();
      String rel = path.startsWith(base) ? path.substring(base.length()) : path;
      List<String> segs = new ArrayList<>();
      for (String segment : rel.split("/")) {
        if (!segment.isEmpty()) {
          segs.add(segment);
        }
      }
      if (segs.size() <= depth) {
        kept.add(memory);
      }
      for (int level = 1; level <= Math.min(segs.size() - 1, depth); level++) {
        dirs.add(base + String.join("/", segs.subList(0, level)) + "/");
      }
    }
    page.setData(kept);
    page.setPrefixes(new ArrayList<>(dirs));
  }

  /** Memories (files) inside a memory store. */
  public final class Memories {

    public Memory create(String storeId, MemoryCreateParam param) {
      return AsyncHelper.joinAndUnwrap(createAsync(storeId, param));
    }

    public Memory retrieve(String storeId, String memoryId) {
      return retrieve(storeId, memoryId, null, null);
    }

    public Memory retrieve(
        String storeId, String memoryId, String apiKey, Map<String, String> headers) {
      return AsyncHelper.joinAndUnwrap(retrieveAsync(storeId, memoryId, apiKey, headers));
    }

    public Memory update(String storeId, String memoryId, MemoryUpdateParam param) {
      return AsyncHelper.joinAndUnwrap(updateAsync(storeId, memoryId, param));
    }

    public CursorPage<Memory> list(String storeId, MemoryListParam param) {
      return AsyncHelper.joinAndUnwrap(listAsync(storeId, param));
    }

    public AgentStudioDeletionStatus delete(String storeId, String memoryId) {
      return delete(storeId, memoryId, null, null);
    }

    public AgentStudioDeletionStatus delete(
        String storeId, String memoryId, String apiKey, Map<String, String> headers) {
      return AsyncHelper.joinAndUnwrap(deleteAsync(storeId, memoryId, apiKey, headers));
    }

    public CompletableFuture<Memory> createAsync(String storeId, MemoryCreateParam param) {
      if (storeId == null || storeId.isEmpty()) {
        return AsyncHelper.failedFuture(new InputRequiredException("storeId is required!"));
      }
      if (param == null) {
        return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.POST, StringUtils.format("memory_stores/%s/memories", storeId), baseUrl);
      return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
          .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Memory.class));
    }

    public CompletableFuture<Memory> retrieveAsync(String storeId, String memoryId) {
      return retrieveAsync(storeId, memoryId, null, null);
    }

    public CompletableFuture<Memory> retrieveAsync(
        String storeId, String memoryId, String apiKey, Map<String, String> headers) {
      if (storeId == null || storeId.isEmpty() || memoryId == null || memoryId.isEmpty()) {
        return AsyncHelper.failedFuture(
            new InputRequiredException("storeId and memoryId are required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.GET,
              StringUtils.format("memory_stores/%s/memories/%s", storeId, memoryId),
              baseUrl);
      String resolvedKey = apiKey != null ? apiKey : MemoryStores.this.apiKey;
      return AsyncHelper.callAsync(
              api,
              GeneralGetParam.builder()
                  .apiKey(resolvedKey)
                  .headers(headers != null ? headers : new HashMap<>())
                  .build(),
              opt)
          .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Memory.class));
    }

    public CompletableFuture<Memory> updateAsync(
        String storeId, String memoryId, MemoryUpdateParam param) {
      if (storeId == null || storeId.isEmpty() || memoryId == null || memoryId.isEmpty()) {
        return AsyncHelper.failedFuture(
            new InputRequiredException("storeId and memoryId are required!"));
      }
      if (param == null) {
        return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.POST,
              StringUtils.format("memory_stores/%s/memories/%s", storeId, memoryId),
              baseUrl);
      return AsyncHelper.callAsync(api, AgentStudioConstants.withApiKey(apiKey, param), opt)
          .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, Memory.class));
    }

    public CompletableFuture<CursorPage<Memory>> listAsync(String storeId, MemoryListParam param) {
      if (storeId == null || storeId.isEmpty()) {
        return AsyncHelper.failedFuture(new InputRequiredException("storeId is required!"));
      }
      if (param == null) {
        return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
      }
      boolean depthEmulated = param.getDepth() != null && param.getDepth() > MAX_WIRE_DEPTH;
      // depth >= 2 must not reach the wire; everything else passes through.
      MemoryListParam wireParam = param;
      if (depthEmulated) {
        wireParam =
            MemoryListParam.builder()
                .pathPrefix(param.getPathPrefix())
                .view(param.getView())
                .limit(param.getLimit())
                .page(param.getPage())
                .build();
      }
      String query = wireParam.toQueryString();
      String path = StringUtils.format("memory_stores/%s/memories", storeId);
      if (!query.isEmpty()) {
        path = path + "?" + query;
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
      return AsyncHelper.callAsync(
              api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
          .thenApply(
              r -> {
                Type type = new TypeToken<CursorPage<Memory>>() {}.getType();
                CursorPage<Memory> page = FlattenResultBase.fromDashScopeResult(r, type);
                page.setFetchNext(
                    cursor ->
                        listAsync(
                            storeId,
                            MemoryListParam.builder()
                                .pathPrefix(param.getPathPrefix())
                                .depth(param.getDepth())
                                .view(param.getView())
                                .limit(param.getLimit())
                                .page(cursor)
                                .build()));
                if (depthEmulated) {
                  filterByDepth(page, param.getDepth(), param.getPathPrefix());
                }
                return page;
              });
    }

    public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
        String storeId, String memoryId) {
      return deleteAsync(storeId, memoryId, null, null);
    }

    public CompletableFuture<AgentStudioDeletionStatus> deleteAsync(
        String storeId, String memoryId, String apiKey, Map<String, String> headers) {
      if (storeId == null || storeId.isEmpty() || memoryId == null || memoryId.isEmpty()) {
        return AsyncHelper.failedFuture(
            new InputRequiredException("storeId and memoryId are required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.DELETE,
              StringUtils.format("memory_stores/%s/memories/%s", storeId, memoryId),
              baseUrl);
      String resolvedKey = apiKey != null ? apiKey : MemoryStores.this.apiKey;
      return AsyncHelper.callAsync(
              api,
              GeneralGetParam.builder()
                  .apiKey(resolvedKey)
                  .headers(headers != null ? headers : new HashMap<>())
                  .build(),
              opt)
          .thenApply(
              r -> FlattenResultBase.fromDashScopeResult(r, AgentStudioDeletionStatus.class));
    }
  }

  /** Immutable version snapshots of the memories in a store. */
  public final class MemoryVersions {

    public CursorPage<MemoryVersion> list(String storeId, MemoryVersionListParam param) {
      return AsyncHelper.joinAndUnwrap(listAsync(storeId, param));
    }

    public MemoryVersion retrieve(String storeId, String versionId) {
      return retrieve(storeId, versionId, null, null);
    }

    public MemoryVersion retrieve(
        String storeId, String versionId, String apiKey, Map<String, String> headers) {
      return AsyncHelper.joinAndUnwrap(retrieveAsync(storeId, versionId, apiKey, headers));
    }

    public MemoryVersion redact(String storeId, String versionId) {
      return AsyncHelper.joinAndUnwrap(redactAsync(storeId, versionId));
    }

    public CompletableFuture<CursorPage<MemoryVersion>> listAsync(
        String storeId, MemoryVersionListParam param) {
      if (storeId == null || storeId.isEmpty()) {
        return AsyncHelper.failedFuture(new InputRequiredException("storeId is required!"));
      }
      if (param == null) {
        return AsyncHelper.failedFuture(new InputRequiredException("param is required!"));
      }
      String query = param.toQueryString();
      String path = StringUtils.format("memory_stores/%s/memory_versions", storeId);
      if (!query.isEmpty()) {
        path = path + "?" + query;
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(HttpMethod.GET, path, baseUrl);
      return AsyncHelper.callAsync(
              api, GeneralGetParam.builder().apiKey(apiKey).headers(new HashMap<>()).build(), opt)
          .thenApply(
              r -> {
                Type type = new TypeToken<CursorPage<MemoryVersion>>() {}.getType();
                CursorPage<MemoryVersion> page = FlattenResultBase.fromDashScopeResult(r, type);
                page.setFetchNext(
                    cursor ->
                        listAsync(
                            storeId,
                            MemoryVersionListParam.builder()
                                .memoryId(param.getMemoryId())
                                .operation(param.getOperation())
                                .createdAtGte(param.getCreatedAtGte())
                                .createdAtLte(param.getCreatedAtLte())
                                .view(param.getView())
                                .limit(param.getLimit())
                                .page(cursor)
                                .build()));
                return page;
              });
    }

    public CompletableFuture<MemoryVersion> retrieveAsync(String storeId, String versionId) {
      return retrieveAsync(storeId, versionId, null, null);
    }

    public CompletableFuture<MemoryVersion> retrieveAsync(
        String storeId, String versionId, String apiKey, Map<String, String> headers) {
      if (storeId == null || storeId.isEmpty() || versionId == null || versionId.isEmpty()) {
        return AsyncHelper.failedFuture(
            new InputRequiredException("storeId and versionId are required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.GET,
              StringUtils.format("memory_stores/%s/memory_versions/%s", storeId, versionId),
              baseUrl);
      String resolvedKey = apiKey != null ? apiKey : MemoryStores.this.apiKey;
      return AsyncHelper.callAsync(
              api,
              GeneralGetParam.builder()
                  .apiKey(resolvedKey)
                  .headers(headers != null ? headers : new HashMap<>())
                  .build(),
              opt)
          .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, MemoryVersion.class));
    }

    /** Redact a version: idempotent and irreversible. */
    public CompletableFuture<MemoryVersion> redactAsync(String storeId, String versionId) {
      if (storeId == null || storeId.isEmpty() || versionId == null || versionId.isEmpty()) {
        return AsyncHelper.failedFuture(
            new InputRequiredException("storeId and versionId are required!"));
      }
      GeneralServiceOption opt =
          AgentStudioConstants.newServiceOption(
              HttpMethod.POST,
              StringUtils.format("memory_stores/%s/memory_versions/%s/redact", storeId, versionId),
              baseUrl);
      return AsyncHelper.callAsync(
              api,
              AgentStudioConstants.withApiKey(apiKey, MemoryUpdateParam.builder().build()),
              opt)
          .thenApply(r -> FlattenResultBase.fromDashScopeResult(r, MemoryVersion.class));
    }
  }
}
