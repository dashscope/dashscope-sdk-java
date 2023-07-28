package com.alibaba.dashscope.aigc.imagesynthesis;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ImageSynthesisUsage {
  @SerializedName("image_count")
  private Integer imageCount;
}
