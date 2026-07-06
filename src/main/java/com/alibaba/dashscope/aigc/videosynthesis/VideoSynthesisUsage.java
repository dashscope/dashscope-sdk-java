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

  private Float duration;

  private String size;

  @SerializedName("input_video_duration")
  private Float inputVideoDuration;

  @SerializedName("output_video_duration")
  private Float outputVideoDuration;

  @SerializedName("SR")
  private String SR;

  private String ratio;

  private Boolean audio;

  @SerializedName("shot_type")
  private String shotType;

  private Float fps;

  @SerializedName("reference_type")
  private String referenceType;
}
