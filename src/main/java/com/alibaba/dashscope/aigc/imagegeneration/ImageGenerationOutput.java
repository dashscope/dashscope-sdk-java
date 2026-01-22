package com.alibaba.dashscope.aigc.imagegeneration;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioResult;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class ImageGenerationOutput {
  // output message.
  @Data
  @SuperBuilder
  @NoArgsConstructor
  public static class Choice {
    @SerializedName("finish_reason")
    private String finishReason;

    private ImageGenerationMessage message;
  }

  private List<Choice> choices;

  @SerializedName("finish_reason")
  private String finishReason;

  @SerializedName("audio")
  private AudioResult audio;

  @SerializedName("task_id")
  private String taskId;

  @SerializedName("task_status")
  private String taskStatus;

  @SerializedName("finished")
  private Boolean finished;

  @SerializedName("submit_time")
  private String submitTime;

  @SerializedName("scheduled_time")
  private String scheduledTime;

  @SerializedName("end_time")
  private String endTime;
}
