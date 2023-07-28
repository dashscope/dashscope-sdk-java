package com.alibaba.dashscope.aigc.imagesynthesis;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ImageSynthesisOutput {
  @SerializedName("task_id")
  private String taskId;

  @SerializedName("task_status")
  private String taskStatus;

  private String code;

  private String message;

  private List<Map<String, String>> results;

  @SerializedName("task_metrics")
  private ImageSynthesisTaskMetrics taskMetrics;
}
