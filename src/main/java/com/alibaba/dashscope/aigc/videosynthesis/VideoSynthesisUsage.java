package com.alibaba.dashscope.aigc.videosynthesis;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class VideoSynthesisUsage {
  @SerializedName("video_count")
  private Integer videoCount;

  @SerializedName("video_duration")
  private Integer videoDuration;

  @SerializedName("video_ratio")
  private String videoRatio;
}
