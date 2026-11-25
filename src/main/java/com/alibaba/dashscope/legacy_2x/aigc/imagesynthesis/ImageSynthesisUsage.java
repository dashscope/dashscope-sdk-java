package com.alibaba.dashscope.legacy_2x.aigc.imagesynthesis;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@Deprecated
public class ImageSynthesisUsage {
  @SerializedName("image_count")
  private Integer imageCount;
}
