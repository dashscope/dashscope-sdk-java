package com.alibaba.dashscope.common;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class History {
  public String user;
  public String bot;
}
