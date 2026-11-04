package com.alibaba.dashscope.legacy_2x.common;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Deprecated
public class ImageURL {
  private String url;
  private String detail;
}
