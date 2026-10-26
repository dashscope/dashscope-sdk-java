package com.alibaba.dashscope.legacy_2x.aigc.imagesynthesis;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ImageSynthesisTaskMetrics {
  @SerializedName("TOTAL")
  private Integer total;

  @SerializedName("SUCCEEDED")
  private Integer succeeded;

  @SerializedName("FAILED")
  private Integer failed;
}
