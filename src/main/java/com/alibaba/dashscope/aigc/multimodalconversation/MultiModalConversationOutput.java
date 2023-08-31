package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.common.Message;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class MultiModalConversationOutput {
  // output message.
  @Data
  public class Choice {
    @SerializedName("finish_reason")
    private String finishReason;

    private Message message;
  }

  private List<Choice> choices;
}
