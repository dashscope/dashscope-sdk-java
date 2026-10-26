// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.tts.timestamp;

import com.google.gson.annotations.SerializedName;
import lombok.*;

@Data
public class Phoneme {
  /** Phoneme begin time in milliseconds. */
  @SerializedName("begin_time")
  int beginTime;

  /** Phoneme end time in milliseconds. */
  @SerializedName("end_time")
  int endTime;

  /** Phoneme. */
  String text;

  /** Phoneme tone. */
  String tone;
}
