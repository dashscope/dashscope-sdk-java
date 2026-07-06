// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Skill extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("name")
  private String name;

  @SerializedName("description")
  private String description;

  @SerializedName("source")
  private String source;

  @SerializedName("status")
  private String status;

  @SerializedName("latest_version")
  private String latestVersion;

  @SerializedName("version")
  private String version;

  @SerializedName("file_id")
  private String fileId;

  @SerializedName("scope")
  private Scope scope;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;

  @Data
  public static class Scope {
    @SerializedName("type")
    private String type;

    @SerializedName("id")
    private String id;
  }
}
