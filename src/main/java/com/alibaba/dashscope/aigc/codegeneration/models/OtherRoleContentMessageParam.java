package com.alibaba.dashscope.aigc.codegeneration.models;

import lombok.Data;

@Data
public class OtherRoleContentMessageParam extends MessageParamBase {

  private String content;

  public OtherRoleContentMessageParam(String role, String content) {
    super(role);
    this.content = content;
  }
}
