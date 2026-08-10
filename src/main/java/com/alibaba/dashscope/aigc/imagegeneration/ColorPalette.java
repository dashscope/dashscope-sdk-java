package com.alibaba.dashscope.aigc.imagegeneration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Color palette configuration for image generation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorPalette {

  /** Hex color code, e.g., "#FF5733" */
  private String hex;

  /** Ratio of the color in the palette, value range: [0.0, 1.0] */
  private Double ratio;
}
