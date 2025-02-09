package com.alibaba.dashscope.aigc.multimodalconversation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MultiModalConversationUsage {
  @SerializedName("input_tokens")
  private Integer inputTokens;

  @SerializedName("output_tokens")
  private Integer outputTokens;

  @SerializedName("total_tokens")
  private Integer totalTokens;

  @SerializedName("image_tokens")
  private Integer imageTokens;

  @SerializedName("video_tokens")
  private Integer videoTokens;

  @SerializedName("audio_tokens")
  private Integer audioTokens;
}
