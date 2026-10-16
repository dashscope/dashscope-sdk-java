// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A workspace-scoped memory store: a collection of small text documents that persists across
 * sessions.
 *
 * <p>Stores are attached to sessions via the session {@code resources[]} array and mounted into the
 * session container at {@code /mnt/memory/<name>/}. {@code archivedAt} is set once the store is
 * archived: read-only afterwards, existing session attachments keep working, but new sessions
 * cannot reference it.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemoryStore extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("name")
  private String name;

  @SerializedName("description")
  private String description;

  @SerializedName("metadata")
  private Map<String, Object> metadata;

  @SerializedName("archived_at")
  private String archivedAt;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;
}
