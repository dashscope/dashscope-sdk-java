// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.recognition.timestamp;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

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

  boolean heartbeat = false;

  @SerializedName("sentence_id")
  private Long sentenceId;

  public static Sentence from(String message) {
    return JsonUtils.fromJson(message, Sentence.class);
  }

  public static Sentence from(JsonObject json) {
    return JsonUtils.fromJsonObject(json, Sentence.class);
  }
}
