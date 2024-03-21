package com.alibaba.dashscope.tools;

import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ToolChoice {
  /*
   * Tool usage strategy, null means not to use it,
   * auto means to automatically decide whether to use it.
   */
  @Default public String strategy = null;
  /** Specify the tool to use, Set strategy or tool, with tool taking priority */
  @Default public ToolBase tool = null;
}
