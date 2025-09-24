package com.alibaba.dashscope.aigc.multimodalconversation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MultiModalConversationTokensDetails {
  @SerializedName("text_tokens")
  private Integer textTokens;

  @SerializedName("image_tokens")
  private Integer imageTokens;

  @SerializedName("audio_tokens")
  private Integer audioTokens;

  @SerializedName("video_tokens")
  private Integer videoTokens;

  @SerializedName("reasoning_tokens")
  private Integer reasoningTokens;
}
