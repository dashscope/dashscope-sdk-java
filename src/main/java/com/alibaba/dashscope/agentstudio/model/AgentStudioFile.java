// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentStudioFile extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("filename")
  private String filename;

  @SerializedName("downloadable")
  private Boolean downloadable;

  @SerializedName("mime_type")
  private String mimeType;

  @SerializedName("size_bytes")
  private Long sizeBytes;

  @SerializedName("status")
  private String status;

  @SerializedName("created_at")
  private String createdAt;
}
