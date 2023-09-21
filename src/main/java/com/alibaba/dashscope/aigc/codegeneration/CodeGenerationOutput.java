// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.codegeneration;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public final class CodeGenerationOutput {
  // output message.
  @Data
  public class Choice {
    @SerializedName("finish_reason")
    private String finishReason;

    @SerializedName("frame_timestamp")
    private Double frameTimestamp;

    private Integer index;

    private String content;

    @SerializedName("frame_id")
    private Integer frameId;
  }

  private List<Choice> choices;
}
