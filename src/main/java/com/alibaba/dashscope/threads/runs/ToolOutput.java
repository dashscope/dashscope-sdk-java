package com.alibaba.dashscope.threads.runs;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ToolOutput {
  @SerializedName("tool_call_id")
  private String toolCallId;

  private String output;
}
