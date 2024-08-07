package com.alibaba.dashscope.common;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ImageURL {
  private String url;
  private String detail;
}
