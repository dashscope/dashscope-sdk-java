package com.alibaba.dashscope.tools;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
public class ToolFunction extends ToolBase {
  static {
    registerTool("function", ToolFunction.class);
  }

  private FunctionDefinition function;
  @Builder.Default private String type = "function";

  public String getType() {
    return type;
  }
}
