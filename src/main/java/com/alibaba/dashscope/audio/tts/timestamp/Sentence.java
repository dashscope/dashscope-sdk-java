// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.tts.timestamp;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.*;

@Data
public class Sentence {
  /** Sentence begin time in milliseconds. */
  @SerializedName("begin_time")
  int beginTime;

  /** Sentence end time in milliseconds. */
  @SerializedName("end_time")
  int endTime;

  /** Sentence words. */
  List<Word> words;

  public static Sentence from(String message) {
    return JsonUtils.fromJson(message, Sentence.class);
  }

  public static Sentence from(JsonObject json) {
    return JsonUtils.fromJsonObject(json, Sentence.class);
  }
}
