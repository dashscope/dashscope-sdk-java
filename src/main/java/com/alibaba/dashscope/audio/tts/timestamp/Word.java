// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.tts.timestamp;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.*;

@Data
public class Word {
  /** Word begin time in milliseconds. */
  @SerializedName("begin_time")
  int beginTime;

  /** Word end time in milliseconds. */
  @SerializedName("end_time")
  int endTime;

  /** Word. */
  String text;

  /** Word phonemes. */
  List<Phoneme> phonemes;
}
