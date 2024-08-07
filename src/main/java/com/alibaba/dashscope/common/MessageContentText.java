package com.alibaba.dashscope.common;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MessageContentText extends MessageContentBase {
  @Builder.Default private String type = "text";
  private String text;
}
