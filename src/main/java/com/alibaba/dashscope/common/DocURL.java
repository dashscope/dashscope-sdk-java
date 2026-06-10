package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class DocURL {
  private String url;

  @SerializedName("file_parsing_strategy")
  private String fileParsingStrategy;
}
