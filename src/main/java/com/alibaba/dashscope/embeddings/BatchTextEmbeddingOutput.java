package com.alibaba.dashscope.embeddings;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class BatchTextEmbeddingOutput {
  @SerializedName("task_id")
  private String taskId;

  @SerializedName("task_status")
  private String taskStatus;

  private String code;

  private String message;

  private String url;
}
