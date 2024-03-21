package com.alibaba.dashscope.tools;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ToolFunction implements ToolBase {
  private FunctionDefinition function;
  @Builder.Default private String type = "function";

  public String getType() {
    return type;
  }
}
