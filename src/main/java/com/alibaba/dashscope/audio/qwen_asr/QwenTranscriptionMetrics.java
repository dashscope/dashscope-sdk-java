// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.qwen_asr;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class QwenTranscriptionMetrics {
  @SerializedName("TOTAL")
  private int total;

  @SerializedName("SUCCEEDED")
  private int succeeded;

  @SerializedName("FAILED")
  private int failed;

  public static QwenTranscriptionMetrics from(JsonObject asJsonObject) {
    return JsonUtils.fromJsonObject(asJsonObject, QwenTranscriptionMetrics.class);
  }
}
