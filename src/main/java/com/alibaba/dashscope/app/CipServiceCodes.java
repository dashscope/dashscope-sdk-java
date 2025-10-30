// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.app;

import lombok.Builder;
import lombok.Data;

/**
 * CIP service codes configuration for content security check.
 *
 * @since jdk8
 */
@Data
@Builder
public class CipServiceCodes {
  /** Text security check configuration */
  private Text text;

  /** Image security check configuration */
  private Image image;

  /**
   * Text security check configuration.
   */
  @Data
  @Builder
  public static class Text {
    /** Input security check service code */
    private String input;

    /** Output security check service code */
    private String output;
  }

  /**
   * Image security check configuration.
   */
  @Data
  @Builder
  public static class Image {
    /** Input security check service code */
    private String input;

    /** Output security check service code */
    private String output;
  }
}
