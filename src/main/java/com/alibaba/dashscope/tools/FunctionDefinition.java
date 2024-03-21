package com.alibaba.dashscope.tools;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class FunctionDefinition {
  private String name;
  private String description;
  private JsonObject parameters;
}
