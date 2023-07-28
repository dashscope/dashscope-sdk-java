// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.transcription;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TranscriptionMetrics {
  @SerializedName("TOTAL")
  private int total;

  @SerializedName("SUCCEEDED")
  private int succeeded;

  @SerializedName("FAILED")
  private int failed;

  public static TranscriptionMetrics from(JsonObject asJsonObject) {
    return JsonUtils.fromJsonObject(asJsonObject, TranscriptionMetrics.class);
  }
}
