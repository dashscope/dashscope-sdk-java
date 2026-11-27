package com.alibaba.dashscope.legacy_2x.aigc.codegeneration.models;

import com.alibaba.dashscope.legacy_2x.common.Role;
import com.google.gson.JsonObject;
import lombok.Data;

@Data
@Deprecated
public class AttachmentRoleMessageParam extends MessageParamBase {

  public JsonObject meta;

  public AttachmentRoleMessageParam(JsonObject meta) {
    super(Role.ATTACHMENT.getValue());
    this.meta = meta;
  }
}
