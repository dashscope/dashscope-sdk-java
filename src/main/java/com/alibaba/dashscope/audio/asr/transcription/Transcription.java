// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.transcription;

import com.alibaba.dashscope.api.AsynchronousApi;
import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Transcription {
  private final AsynchronousApi<TranscriptionParam> asyncApi;
  private final ApiServiceOption createServiceOptions;
  private final String baseUrl;

  public Transcription() {
    asyncApi = new AsynchronousApi<TranscriptionParam>();
    createServiceOptions =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.NONE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.ASR.getValue())
            .function(Function.TRANSCRIPTION.getValue())
            .isAsyncTask(true)
            .build();
    this.baseUrl = null;
  }

  public TranscriptionResult asyncCall(TranscriptionParam param) {
    try {
      return TranscriptionResult.fromDashScopeResult(
          asyncApi.asyncCall(param, createServiceOptions));
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
  }

  public TranscriptionResult wait(TranscriptionQueryParam queryParam) {
    try {
      return TranscriptionResult.fromDashScopeResult(
          asyncApi.wait(
              queryParam.getTaskId(),
              queryParam.getApiKey(),
              baseUrl,
              queryParam.getCustomHeaders()));
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
  }

  public TranscriptionResult fetch(TranscriptionQueryParam queryParam) {
    try {
      return TranscriptionResult.fromDashScopeResult(
          asyncApi.fetch(
              queryParam.getTaskId(),
              queryParam.getApiKey(),
              baseUrl,
              queryParam.getCustomHeaders()));
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
  }
}
