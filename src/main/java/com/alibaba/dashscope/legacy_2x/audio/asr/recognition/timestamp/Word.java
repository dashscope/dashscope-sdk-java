// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.asr.recognition.timestamp;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@Deprecated
public class Word {
  /** Word begin time in milliseconds. */
  @SerializedName("begin_time")
  long beginTime;

  /** Word end time in milliseconds. */
  @SerializedName("end_time")
  long endTime;

  /** Word. */
  String text;

  String punctuation;

  boolean fixed;
}
