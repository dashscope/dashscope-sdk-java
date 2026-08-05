// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DeploymentResource {
  @SerializedName("type")
  private String type;

  @SerializedName("file_id")
  private String fileId;

  @SerializedName("mount_path")
  private String mountPath;
}
