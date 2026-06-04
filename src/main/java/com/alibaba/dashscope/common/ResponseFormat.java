// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
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

  /**
   * JSON schema definition when type is JSON_SCHEMA. Used to enforce structured output from the
   * model.
   */
  @SerializedName("json_schema")
  private JsonSchema jsonSchema;

  @SuperBuilder
  @Data
  public static class JsonSchema {
    /** A friendly name for the JSON schema. */
    private String name;

    /** The JSON schema definition as a string or object. */
    private Object schema;

    /** Whether to strictly validate the output against the schema. Default: true. */
    @Builder.Default
    @SerializedName("strict")
    private Boolean strict = true;
  }

  public static ResponseFormat from(Object type) {
    return ResponseFormat.builder().type(type).build();
  }

  public static ResponseFormat fromJsonSchema(String name, Object schema) {
    return ResponseFormat.builder()
        .type(JSON_SCHEMA)
        .jsonSchema(JsonSchema.builder().name(name).schema(schema).build())
        .build();
  }
}
