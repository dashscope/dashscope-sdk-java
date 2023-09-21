// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.codegeneration;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class CodeGenerationUsage {

  @SerializedName("input_tokens")
  private Integer inputTokens;

  @SerializedName("output_tokens")
  private Integer outputTokens;
}
