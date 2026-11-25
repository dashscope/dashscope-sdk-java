package com.alibaba.dashscope.legacy_2x.aigc.multimodalconversation;

import com.alibaba.dashscope.legacy_2x.common.MultiModalMessage;
import com.alibaba.dashscope.legacy_2x.common.SearchInfo;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
@Deprecated
public class MultiModalConversationOutput {
  // output message.
  @Data
  public static class Choice {
    @SerializedName("finish_reason")
    private String finishReason;

    private MultiModalMessage message;
  }

  private List<Choice> choices;

  @SerializedName("audio")
  private AudioResult audio;

  @SerializedName("finish_reason")
  private String finishReason;

  /** Information returned by web search when search_options is set. */
  @SerializedName("search_info")
  private SearchInfo searchInfo;
}
