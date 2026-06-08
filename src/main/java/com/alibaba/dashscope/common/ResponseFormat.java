// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class ResponseFormat {
  public static final String TEXT = "text";

  public static final String JSON_OBJECT = "json_object";

  @SerializedName("type")
  private Object type;

  public static ResponseFormat from(Object type) {
    return ResponseFormat.builder().type(type).build();
  }
}
