package com.alibaba.dashscope.legacy_2x.tools;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Deprecated
public class FunctionDefinition {
  private String name;
  private String description;
  private JsonObject parameters;
}
