package com.alibaba.dashscope.tools;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ToolCallFunction extends ToolCallBase {
  static {
    registerToolCall("function", ToolCallFunction.class);
  }

  @Data
  public class CallFunction {
    private String name;
    private String arguments;
    private String output;
  }

  private String id;
  private String type = "function";

  private CallFunction function;
}
