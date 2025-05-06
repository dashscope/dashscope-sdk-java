// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.generation;

import com.alibaba.dashscope.common.Message;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public final class GenerationOutput {
  // output message.
  @Data
  public class Choice {
    @SerializedName("finish_reason")
    private String finishReason;

    private Integer index;

    private Message message;

    private GenerationLogprobs logprobs;
  }
  // output text
  private String text;

  @SerializedName("finish_reason")
  private String finishReason;

  private List<Choice> choices;

  /** 联网搜索到的信息，在设置search_options参数后会返回该参数。 */
  @SerializedName("search_info")
  private SearchInfo searchInfo;
}
