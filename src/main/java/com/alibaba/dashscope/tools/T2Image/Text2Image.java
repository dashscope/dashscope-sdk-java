package com.alibaba.dashscope.tools.T2Image;

import com.alibaba.dashscope.tools.ToolBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
public class Text2Image extends ToolBase {
  static {
    registerTool("text_to_image", Text2Image.class);
  }
  /**
   * Type
   *
   * <p>(Required)
   */
  @SerializedName("type")
  private final String type = "text_to_image";
}
