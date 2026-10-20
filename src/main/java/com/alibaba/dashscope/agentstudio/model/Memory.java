// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * One text document inside a memory store, addressed by {@code path}.
 *
 * <p>{@code content} appears only in retrieve responses and in list responses with {@code
 * view="full"}; create and update never return it. {@code contentSha256} identifies the content
 * revision — pass it back to {@code memories().update()} for optimistic concurrency (a mismatch
 * returns 409 {@code memory_precondition_failed_error}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Memory extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("memory_store_id")
  private String memoryStoreId;

  @SerializedName("path")
  private String path;

  @SerializedName("content")
  private String content;

  @SerializedName("content_sha256")
  private String contentSha256;

  @SerializedName("content_size_bytes")
  private Long contentSizeBytes;

  @SerializedName("memory_version_id")
  private String memoryVersionId;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;
}
