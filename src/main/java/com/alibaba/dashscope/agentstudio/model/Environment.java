// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Environment extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("name")
  private String name;

  @SerializedName("description")
  private String description;

  @SerializedName("status")
  private String status;

  @SerializedName("config")
  private Config config;

  @SerializedName("metadata")
  private Map<String, String> metadata;

  @SerializedName("scope")
  private String scope;

  @SerializedName("archived_at")
  private String archivedAt;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;

  @Data
  public static class Config {
    @SerializedName("type")
    private String type;

    @SerializedName("networking")
    private Networking networking;

    @SerializedName("packages")
    private Packages packages;

    @Data
    public static class Networking {
      @SerializedName("type")
      private String type;
    }

    @Data
    public static class Packages {
      @SerializedName("apt")
      private List<String> apt;

      @SerializedName("gem")
      private List<String> gem;

      @SerializedName("pip")
      private List<String> pip;

      @SerializedName("cargo")
      private List<String> cargo;

      @SerializedName("go")
      private List<String> go;

      @SerializedName("npm")
      private List<String> npm;
    }
  }
}
