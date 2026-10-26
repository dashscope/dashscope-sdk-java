package com.alibaba.dashscope.legacy_2x.threads.runs;

import com.alibaba.dashscope.legacy_2x.tools.ToolCallBase;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StepToolCalls extends StepDetailBase {
  @SerializedName("tool_calls")
  private List<ToolCallBase> toolCalls;
}
