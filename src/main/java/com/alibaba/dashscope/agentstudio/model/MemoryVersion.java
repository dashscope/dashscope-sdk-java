// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Immutable snapshot produced by every memory mutation; versions are returned newest-first.
 *
 * <p>{@code operation} is {@code created}, {@code modified} or {@code deleted}. After {@code
 * redact()}, {@code path}, {@code content}, {@code contentSha256} and {@code contentSizeBytes} are
 * cleared; the audit trail is preserved.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemoryVersion extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("memory_store_id")
  private String memoryStoreId;

  @SerializedName("memory_id")
  private String memoryId;

  @SerializedName("operation")
  private String operation;

  @SerializedName("path")
  private String path;

  @SerializedName("content")
  private String content;

  @SerializedName("content_sha256")
  private String contentSha256;

  @SerializedName("content_size_bytes")
  private Long contentSizeBytes;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("redacted_at")
  private String redactedAt;
}
