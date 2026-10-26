package com.alibaba.dashscope.legacy_2x.common;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class MessageContentBase {
  public abstract String getType();
}
