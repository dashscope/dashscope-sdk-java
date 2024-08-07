package com.alibaba.dashscope.aigc.completion;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class ChatCompletionStreamOptions {
  @SerializedName("include_usage")
  private Boolean includeUsage;
}
