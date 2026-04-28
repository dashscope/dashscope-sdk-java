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

  private float duration;

  private String size;

  @SerializedName("input_video_duration")
  private float inputVideoDuration;

  @SerializedName("output_video_duration")
  private float outputVideoDuration;

  @SerializedName("SR")
  private String SR;

  private String ratio;

  private Boolean audio;

  @SerializedName("shot_type")
  private String shotType;

  private float fps;

  @SerializedName("reference_type")
  private String referenceType;
}
