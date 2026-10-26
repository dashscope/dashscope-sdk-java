package com.alibaba.dashscope.legacy_2x.aigc.codegeneration.models;

import com.alibaba.dashscope.legacy_2x.common.Role;
import lombok.Data;

@Data
public class UserRoleMessageParam extends MessageParamBase {

  private String content;

  public UserRoleMessageParam(String content) {
    super(Role.USER.getValue());
    this.content = content;
  }
}
