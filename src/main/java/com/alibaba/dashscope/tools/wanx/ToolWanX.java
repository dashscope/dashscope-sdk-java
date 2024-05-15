package com.alibaba.dashscope.tools.wanx;

import com.alibaba.dashscope.tools.ToolBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** @deprecated use `Text2Image` instead */
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
public class ToolWanX extends ToolBase {
  static {
    registerTool("wanx", ToolWanX.class);
  }
  /**
   * Type
   *
   * <p>(Required)
   */
  @SerializedName("type")
  private final String type = "wanx";
}
