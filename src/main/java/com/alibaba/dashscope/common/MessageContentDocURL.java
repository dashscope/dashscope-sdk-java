package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MessageContentDocURL extends MessageContentBase {
  @Builder.Default private String type = "doc_url";

  @SerializedName("doc_url")
  private DocURL docURL;
}
