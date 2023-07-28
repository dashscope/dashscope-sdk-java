// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.transcription;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.TaskStatus;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode()
public class TranscriptionResult {
  @SerializedName(ApiKeywords.REQUEST_ID)
  private String requestId;
  /** The model outputs. */
  private JsonObject output;

  /** The data usage. */
  private JsonObject usage;

  private TaskStatus taskStatus;

  private String taskId;

  private List<TranscriptionTaskResult> results = new ArrayList<>();

  private TranscriptionMetrics metrics;

  public static TranscriptionResult fromDashScopeResult(DashScopeResult dashScopeResult)
      throws ApiException {
    TranscriptionResult result = new TranscriptionResult();
    result.output = (JsonObject) dashScopeResult.getOutput();
    if (dashScopeResult.getUsage() != null) {
      result.usage = dashScopeResult.getUsage().getAsJsonObject();
    }
    result.requestId = dashScopeResult.getRequestId();
    if (dashScopeResult.getOutput() != null) {
      if (result.output.has(TranscriptionApiKeywords.TASK_STATUS)) {
        JsonElement jsonTaskStatus = result.output.get(TranscriptionApiKeywords.TASK_STATUS);
        if (jsonTaskStatus != null) {
          result.taskStatus = TaskStatus.valueOf(jsonTaskStatus.getAsString());
        } else {
          result.taskStatus = TaskStatus.FAILED;
        }
      }
      if (result.output.has(TranscriptionApiKeywords.TASK_ID)) {
        result.taskId = result.output.get(TranscriptionApiKeywords.TASK_ID).getAsString();
      } else {
        result.taskId = null;
      }
      if (result.output.has(TranscriptionApiKeywords.TASK_RESULTS)) {
        JsonElement jsonResults = result.output.get(TranscriptionApiKeywords.TASK_RESULTS);
        if (jsonResults != null) {
          if (result.results == null) {
            result.results = new ArrayList<>();
          }
          JsonArray array = jsonResults.getAsJsonArray();
          for (JsonElement object : array) {
            TranscriptionTaskResult taskResult =
                TranscriptionTaskResult.from(object.getAsJsonObject());
            result.results.add(taskResult);
          }
        } else {
          result.results = new ArrayList<>();
        }
      }
      if (result.output.has(TranscriptionApiKeywords.TASK_METRICS)) {
        JsonElement jsonMetrics = result.output.get(TranscriptionApiKeywords.TASK_METRICS);
        if (jsonMetrics != null) {
          result.setMetrics(TranscriptionMetrics.from(jsonMetrics.getAsJsonObject()));
        } else {
          result.setMetrics(new TranscriptionMetrics());
        }
      }
    }
    return result;
  }
}
