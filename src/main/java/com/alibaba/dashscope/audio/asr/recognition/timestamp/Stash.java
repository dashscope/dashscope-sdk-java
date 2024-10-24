// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.recognition.timestamp;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class Stash {
  @SerializedName("sentence_id")
  int sentenceId;

  @SerializedName("begin_time")
  Long beginTime;

  /** Sentence end time in milliseconds. */
  @SerializedName("end_time")
  Long endTime;

  String text;

  /** Sentence words. */
  List<Word> words;

  public static Stash from(String message) {
    return JsonUtils.fromJson(message, Stash.class);
  }

  public static Stash from(JsonObject json) {
    return JsonUtils.fromJsonObject(json, Stash.class);
  }
}
