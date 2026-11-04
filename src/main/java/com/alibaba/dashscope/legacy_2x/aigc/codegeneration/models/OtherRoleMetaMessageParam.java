package com.alibaba.dashscope.legacy_2x.aigc.codegeneration.models;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
@Deprecated
public class OtherRoleMetaMessageParam extends MessageParamBase {

  public JsonObject meta;

  public OtherRoleMetaMessageParam(String role, JsonObject meta) {
    super(role);
    this.meta = meta;
  }
}
