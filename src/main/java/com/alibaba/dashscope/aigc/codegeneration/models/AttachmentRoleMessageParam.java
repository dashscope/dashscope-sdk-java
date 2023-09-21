package com.alibaba.dashscope.aigc.codegeneration.models;

import com.alibaba.dashscope.common.Role;
import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class AttachmentRoleMessageParam extends MessageParamBase {

  public JsonObject meta;

  public AttachmentRoleMessageParam(JsonObject meta) {
    super(Role.ATTACHMENT.getValue());
    this.meta = meta;
  }
}
