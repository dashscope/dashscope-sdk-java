package com.alibaba.dashscope.aigc.videosynthesis;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class VideoSynthesisOutput {
  @SerializedName("task_id")
  private String taskId;

  @SerializedName("task_status")
  private String taskStatus;

  private String code;

  private String message;

  @SerializedName("video_url")
  private String videoUrl;

  @SerializedName("check_audio")
  private String checkAudio;

  @SerializedName("orig_prompt")
  private String origPrompt;
}
