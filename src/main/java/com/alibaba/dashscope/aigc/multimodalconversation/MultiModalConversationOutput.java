package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.SearchInfo;
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

  @SerializedName("audio")
  private AudioResult audio;

  @SerializedName("finish_reason")
  private String finishReason;

  /** 联网搜索到的信息，在设置search_options参数后会返回该参数。 */
  @SerializedName("search_info")
  private SearchInfo searchInfo;
}
