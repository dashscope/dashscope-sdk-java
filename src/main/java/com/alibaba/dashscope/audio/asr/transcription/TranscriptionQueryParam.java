package com.alibaba.dashscope.audio.asr.transcription;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class TranscriptionQueryParam {
  private String taskId;

  private String apiKey;

  public static TranscriptionQueryParam FromTranscriptionParam(
      TranscriptionParam param, String taskId) {
    return TranscriptionQueryParam.builder().apiKey(param.getApiKey()).taskId(taskId).build();
  }
}
