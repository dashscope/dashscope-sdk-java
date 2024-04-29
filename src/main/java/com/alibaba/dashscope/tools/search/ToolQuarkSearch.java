package com.alibaba.dashscope.tools.search;

import com.alibaba.dashscope.tools.ToolBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** CodeInterpreterTool */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class ToolQuarkSearch extends ToolBase {
  static {
    registerTool("quark_search", ToolQuarkSearch.class);
  }
  /**
   * Type
   *
   * <p>(Required)
   */
  @SerializedName("type")
  private final String type = "quark_search";
}
