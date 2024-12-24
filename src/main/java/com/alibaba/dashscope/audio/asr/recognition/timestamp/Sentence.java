// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.recognition.timestamp;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class Sentence {
  @SerializedName("begin_time")
  Long beginTime;

  /** Sentence end time in milliseconds. */
  @SerializedName("end_time")
  Long endTime;

  String text;

  /** Sentence words. */
  List<Word> words;

  Stash stash;

  @SerializedName("emo_tag")
  String emoTag;

  @SerializedName("emo_confidence")
  Double emoConfidence;

  public static Sentence from(String message) {
    return JsonUtils.fromJson(message, Sentence.class);
  }

  public static Sentence from(JsonObject json) {
    return JsonUtils.fromJsonObject(json, Sentence.class);
  }
}
