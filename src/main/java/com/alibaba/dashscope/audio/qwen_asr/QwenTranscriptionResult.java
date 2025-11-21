// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.qwen_asr;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.TaskStatus;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode()
public class QwenTranscriptionResult {
  @SerializedName(ApiKeywords.REQUEST_ID)
  private String requestId;
  /** The model outputs. */
  private JsonObject output;

  /** The data usage. */
  private JsonObject usage;

  private TaskStatus taskStatus;

  private String taskId;

  private QwenTranscriptionTaskResult result;

  private QwenTranscriptionMetrics metrics;

  public static QwenTranscriptionResult fromDashScopeResult(DashScopeResult dashScopeResult)
      throws ApiException {
    QwenTranscriptionResult result = new QwenTranscriptionResult();
    result.output = (JsonObject) dashScopeResult.getOutput();
    if (dashScopeResult.getUsage() != null) {
      result.usage = dashScopeResult.getUsage().getAsJsonObject();
    }
    result.requestId = dashScopeResult.getRequestId();
    if (dashScopeResult.getOutput() != null) {
      if (result.output.has(QwenTranscriptionApiKeywords.TASK_STATUS)) {
        JsonElement jsonTaskStatus = result.output.get(QwenTranscriptionApiKeywords.TASK_STATUS);
        if (jsonTaskStatus != null) {
          result.taskStatus = TaskStatus.valueOf(jsonTaskStatus.getAsString());
        } else {
          result.taskStatus = TaskStatus.FAILED;
        }
      }
      if (result.output.has(QwenTranscriptionApiKeywords.TASK_ID)) {
        result.taskId = result.output.get(QwenTranscriptionApiKeywords.TASK_ID).getAsString();
      } else {
        result.taskId = null;
      }
      if (result.output.has(QwenTranscriptionApiKeywords.TASK_RESULT)) {
        JsonElement jsonResult = result.output.get(QwenTranscriptionApiKeywords.TASK_RESULT);
        if (jsonResult != null) {
          result.result = QwenTranscriptionTaskResult.from(jsonResult.getAsJsonObject());
        } else {
          result.result = new QwenTranscriptionTaskResult();
        }
      }
      if (result.output.has(QwenTranscriptionApiKeywords.TASK_METRICS)) {
        JsonElement jsonMetrics = result.output.get(QwenTranscriptionApiKeywords.TASK_METRICS);
        if (jsonMetrics != null) {
          result.setMetrics(QwenTranscriptionMetrics.from(jsonMetrics.getAsJsonObject()));
        } else {
          result.setMetrics(new QwenTranscriptionMetrics());
        }
      }
    }
    return result;
  }
}
