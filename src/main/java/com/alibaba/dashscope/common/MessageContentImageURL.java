package com.alibaba.dashscope.common;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MessageContentImageURL extends MessageContentBase {
  @Builder.Default private String type = "image_url";

  @SerializedName("image_url")
  private ImageURL imageURL;
}
