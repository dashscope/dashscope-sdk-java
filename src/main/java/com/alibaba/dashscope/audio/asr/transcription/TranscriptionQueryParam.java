package com.alibaba.dashscope.audio.asr.transcription;

import com.alibaba.dashscope.common.Param;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.Result;
import com.alibaba.dashscope.protocol.Request;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class TranscriptionQueryParam extends Param {

  private String taskId;

  public static TranscriptionQueryParam FromTranscriptionParam(
      TranscriptionParam param, String taskId) {
    return TranscriptionQueryParam.builder().apiKey(param.getApiKey()).taskId(taskId).build();
  }

  @Override
  public String url() {
    return "/tasks/" + taskId;
  }

  @Override
  public Request toRequest(Protocol protocol) {
    return null;
  }

  @Override
  public Class<? extends Result> resultType() {
    return TranscriptionResult.class;
  }
}
