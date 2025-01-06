// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.translation.results;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Word {
  /** Word. */
  String text;

  /** Word begin time in milliseconds. */
  @SerializedName("begin_time")
  long beginTime;

  /** Word end time in milliseconds. */
  @SerializedName("end_time")
  long endTime;

  boolean fixed;
}
