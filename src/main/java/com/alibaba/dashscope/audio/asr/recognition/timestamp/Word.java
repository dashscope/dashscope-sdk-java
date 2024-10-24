// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.recognition.timestamp;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
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
