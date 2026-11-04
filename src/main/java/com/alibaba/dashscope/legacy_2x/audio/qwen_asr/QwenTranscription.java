// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.qwen_asr;

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
public final class QwenTranscription {
  private final AsynchronousApi<QwenTranscriptionParam> asyncApi;
  private final ApiServiceOption createServiceOptions;
  private final String baseUrl;

  public QwenTranscription() {
    asyncApi = new AsynchronousApi<QwenTranscriptionParam>();
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

  public QwenTranscriptionResult asyncCall(QwenTranscriptionParam param) {
    try {
      return QwenTranscriptionResult.fromDashScopeResult(
          asyncApi.asyncCall(param, createServiceOptions));
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
  }

  public QwenTranscriptionResult wait(QwenTranscriptionQueryParam queryParam) {
    return wait(queryParam, -1);
  }

  public QwenTranscriptionResult wait(QwenTranscriptionQueryParam queryParam, long timeoutSeconds) {
    try {
      return QwenTranscriptionResult.fromDashScopeResult(
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

  public QwenTranscriptionResult fetch(QwenTranscriptionQueryParam queryParam) {
    try {
      return QwenTranscriptionResult.fromDashScopeResult(
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
