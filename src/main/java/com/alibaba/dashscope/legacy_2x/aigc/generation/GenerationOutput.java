// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.aigc.generation;

import com.alibaba.dashscope.legacy_2x.common.Message;
import com.alibaba.dashscope.legacy_2x.common.SearchInfo;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
@Deprecated
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

  /** Information returned by web search when search_options is set. */
  @SerializedName("search_info")
  private SearchInfo searchInfo;

  @SerializedName("model_name")
  private String modelName;
}
