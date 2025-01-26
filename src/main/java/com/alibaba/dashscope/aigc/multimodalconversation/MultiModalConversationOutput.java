package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.common.MultiModalMessage;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class MultiModalConversationOutput {
  // output message.
  @Data
  public static class Choice {
    @SerializedName("finish_reason")
    private String finishReason;

    private MultiModalMessage message;
  }

  private List<Choice> choices;
}
