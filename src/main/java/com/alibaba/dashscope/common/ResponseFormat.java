// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class ResponseFormat {
  public static final String TEXT = "text";

  public static final String JSON_OBJECT = "json_object";

  public static final String JSON_SCHEMA = "json_schema";

  @SerializedName("type")
  private Object type;

  @SerializedName("json_schema")
  private JsonSchemaFormat jsonSchema;

  @SuperBuilder
  @Data
  public static class JsonSchemaFormat {
    private String name;

    private Boolean strict;

    private JsonObject schema;
  }

  public static ResponseFormat from(Object type) {
    return ResponseFormat.builder().type(type).build();
  }
}
