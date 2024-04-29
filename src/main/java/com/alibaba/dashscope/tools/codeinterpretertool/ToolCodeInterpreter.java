package com.alibaba.dashscope.tools.codeinterpretertool;

import com.alibaba.dashscope.tools.ToolBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** CodeInterpreterTool */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class ToolCodeInterpreter extends ToolBase {
  static {
    registerTool("code_interpreter", ToolCodeInterpreter.class);
  }
  /**
   * Type
   *
   * <p>(Required)
   */
  @SerializedName("type")
  private final String type = "code_interpreter";
}
