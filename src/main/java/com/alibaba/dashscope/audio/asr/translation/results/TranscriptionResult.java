// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.translation.results;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class TranscriptionResult {
  @SerializedName("sentence_id")
  Long sentenceId;

  @SerializedName("begin_time")
  Long beginTime;

  /** Sentence end time in milliseconds. */
  @SerializedName("end_time")
  Long endTime;

  @SerializedName("sentence_end")
  boolean sentenceEnd;

  String text;

  Stash stash;

  /** Sentence words. */
  List<Word> words;

  @SerializedName("vad_pre_end")
  boolean vadPreEnd;

  @SerializedName("pre_end_failed")
  boolean preEndFailed;

  @SerializedName("pre_end_timemillis")
  Long preEndTimemillis;

  @SerializedName("pre_end_start_time")
  Long preEndStartTime;

  @SerializedName("pre_end_end_time")
  Long preEndEndTime;

  public static TranscriptionResult from(String message) {
    return JsonUtils.fromJson(message, TranscriptionResult.class);
  }

  public static TranscriptionResult from(JsonObject json) {
    return JsonUtils.fromJsonObject(json, TranscriptionResult.class);
  }
}
