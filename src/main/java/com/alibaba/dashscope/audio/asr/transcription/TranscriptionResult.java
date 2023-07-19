package com.alibaba.dashscope.audio.asr.transcription;

import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.Result;
import com.alibaba.dashscope.common.TaskStatus;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.Response;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TranscriptionResult extends Result {

  private TaskStatus taskStatus;

  private String taskId;

  private List<TranscriptionTaskResult> results = new ArrayList<>();

  private TranscriptionMetrics metrics;

  @Override
  public void fromResponse(Protocol protocol, Response response) throws ApiException {
    TranscriptionMessagePayload payload =
        JsonUtils.fromJson(response.getMessage(), TranscriptionMessagePayload.class);
    this.setUsage(payload.getUsage());
    if (payload.getOutput() != null) {
      JsonElement jsonTaskStatus = payload.getOutput().get(TranscriptionApiKeywords.TASK_STATUS);
      if (jsonTaskStatus != null) {
        this.taskStatus = TaskStatus.valueOf(jsonTaskStatus.getAsString());
      } else {
        this.taskStatus = TaskStatus.FAILED;
      }
      JsonElement jsonTaskId = payload.getOutput().get(TranscriptionApiKeywords.TASK_ID);
      if (jsonTaskId != null) {
        this.taskId = jsonTaskId.getAsString();
      } else {
        this.taskId = null;
      }
      JsonElement jsonResults = payload.getOutput().get(TranscriptionApiKeywords.TASK_RESULTS);
      if (jsonResults != null) {
        JsonArray array = jsonResults.getAsJsonArray();
        for (JsonElement object : array) {
          TranscriptionTaskResult taskResult =
              TranscriptionTaskResult.from(object.getAsJsonObject());
          this.results.add(taskResult);
        }
      } else {
        this.results = null;
      }
      JsonElement jsonMetrics = payload.getOutput().get(TranscriptionApiKeywords.TASK_METRICS);
      if (jsonMetrics != null) {
        this.setMetrics(TranscriptionMetrics.from(jsonMetrics.getAsJsonObject()));
      } else {
        this.setMetrics(new TranscriptionMetrics());
      }
    }
    this.setHeaders(response.getHeaders() == null ? Maps.newHashMap() : response.getHeaders());
  }
}
