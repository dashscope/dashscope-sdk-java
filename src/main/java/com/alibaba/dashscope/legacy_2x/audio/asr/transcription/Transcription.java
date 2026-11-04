// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.asr.transcription;

import com.alibaba.dashscope.legacy_2x.api.AsynchronousApi;
import com.alibaba.dashscope.legacy_2x.common.Function;
import com.alibaba.dashscope.legacy_2x.common.Task;
import com.alibaba.dashscope.legacy_2x.common.TaskGroup;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import com.alibaba.dashscope.legacy_2x.protocol.ApiServiceOption;
import com.alibaba.dashscope.legacy_2x.protocol.HttpMethod;
import com.alibaba.dashscope.legacy_2x.protocol.Protocol;
import com.alibaba.dashscope.legacy_2x.protocol.StreamingMode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Deprecated
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
    return wait(queryParam, -1);
  }

  public TranscriptionResult wait(TranscriptionQueryParam queryParam, long timeoutSeconds) {
    try {
      return TranscriptionResult.fromDashScopeResult(
          asyncApi.wait(
              queryParam.getTaskId(),
              queryParam.getApiKey(),
              baseUrl,
              queryParam.getCustomHeaders(),
              timeoutSeconds));
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
