package com.alibaba.dashscope.legacy_2x.embeddings;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@Deprecated
public class BatchTextEmbeddingOutput {
  @SerializedName("task_id")
  private String taskId;

  @SerializedName("task_status")
  private String taskStatus;

  private String code;

  private String message;

  private String url;
}
