package com.alibaba.dashscope.legacy_2x.aigc.codegeneration.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class MessageParamBase {

  private String role;
}
