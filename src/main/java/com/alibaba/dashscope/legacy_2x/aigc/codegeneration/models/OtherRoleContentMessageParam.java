package com.alibaba.dashscope.legacy_2x.aigc.codegeneration.models;

import lombok.Data;

@Data
public class OtherRoleContentMessageParam extends MessageParamBase {

  private String content;

  public OtherRoleContentMessageParam(String role, String content) {
    super(role);
    this.content = content;
  }
}
