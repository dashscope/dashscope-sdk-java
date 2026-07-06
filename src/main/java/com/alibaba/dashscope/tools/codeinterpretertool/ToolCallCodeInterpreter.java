package com.alibaba.dashscope.tools.codeinterpretertool;

import com.alibaba.dashscope.tools.ToolCallBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ToolCallCodeInterpreter extends ToolCallBase {
  private String type = "code_interpreter";
  private String id;
  private Integer index;

  static {
    registerToolCall("code_interpreter", ToolCallCodeInterpreter.class);
  }
}
