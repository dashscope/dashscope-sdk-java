package com.alibaba.dashscope.legacy_2x.threads.runs;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Deprecated
public class ToolOutput {
  @SerializedName("tool_call_id")
  private String toolCallId;

  private String output;
}
