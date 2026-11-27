// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.asr.transcription;

import com.alibaba.dashscope.legacy_2x.common.TaskStatus;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@Deprecated
public class TranscriptionTaskResult {
  @SerializedName("file_url")
  String fileUrl;

  @SerializedName("transcription_url")
  String transcriptionUrl;

  @SerializedName("subtask_status")
  TaskStatus subTaskStatus;

  String message;

  public static TranscriptionTaskResult from(JsonObject json) {
    return JsonUtils.fromJsonObject(json, TranscriptionTaskResult.class);
  }
}
