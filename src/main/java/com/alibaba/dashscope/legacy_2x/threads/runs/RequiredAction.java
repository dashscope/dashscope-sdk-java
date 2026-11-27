package com.alibaba.dashscope.legacy_2x.threads.runs;

import com.alibaba.dashscope.legacy_2x.tools.ToolCallBase;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
@Deprecated
public class RequiredAction {
  private String type;

  @SerializedName("submit_tool_outputs")
  private InnerRequiredAction submitToolOutputs;

  public static class InnerRequiredAction {
    @SerializedName("tool_calls")
    private List<ToolCallBase> toolCalls;

    public List<ToolCallBase> getToolCalls() {
      return toolCalls;
    }
  }
}
