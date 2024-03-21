package com.alibaba.dashscope.tools;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ToolCallFunction implements ToolCallBase {
  @Data
  public class CallFunction {
    private String name;
    private String arguments;
  }

  private String id;
  @Builder.Default private String type = "function";
  private CallFunction function;
}
