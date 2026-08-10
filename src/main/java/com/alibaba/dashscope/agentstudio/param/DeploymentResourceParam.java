// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class DeploymentResourceParam {
  public static final String TYPE_FILE = "file";

  @NonNull
  @SerializedName("type")
  private String type;

  @NonNull
  @SerializedName("file_id")
  private String fileId;

  @NonNull
  @SerializedName("mount_path")
  private String mountPath;
}
