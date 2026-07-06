package com.alibaba.dashscope.aigc.multimodalconversation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MultiModalConversationUsage {

  @Data
  public static class Plugins {

    @Data
    public static class Search {
      @SerializedName("count")
      private Integer count;

      @SerializedName("strategy")
      private String strategy;
    }

    @SerializedName("search")
    private Search search;
  }

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

  @SerializedName("image_count")
  private Integer imageCount;

  @SerializedName("width")
  private Integer width;

  @SerializedName("height")
  private Integer height;

  @SerializedName("seconds")
  private Integer seconds;

  @SerializedName("input_tokens_details")
  private MultiModalConversationTokensDetails inputTokensDetails;

  @SerializedName("output_tokens_details")
  private MultiModalConversationTokensDetails outputTokensDetails;

  @SerializedName("prompt_tokens_details")
  private MultiModalConversationTokensDetails promptTokensDetails;

  @SerializedName("characters")
  private Integer characters;

  @SerializedName("plugins")
  private Plugins plugins;
}
