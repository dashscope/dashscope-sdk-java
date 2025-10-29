// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.audio.asr.recognition.timestamp.Sentence;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode()
public class RecognitionResult {
  @SerializedName(ApiKeywords.REQUEST_ID)
  private String requestId;

  private Sentence sentence;

  private RecognitionUsage usage;

  private boolean isCompleteResult = false;

  public boolean isSentenceBegin() {
    return sentence.isSentenceBegin();
  }

  public boolean isSentenceEnd() {
    if (sentence.isSentenceEnd()) {
      return true;
    }
    return sentence.getEndTime() != null;
  }

  public static boolean IsSentenceEnd(Sentence sentence) {
    return sentence != null && sentence.getEndTime() != null;
  }

  public static RecognitionResult fromDashScopeResult(DashScopeResult dashScopeResult)
      throws ApiException {
    RecognitionResult result = new RecognitionResult();
    result.setRequestId(dashScopeResult.getRequestId());
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), RecognitionUsage.class));
    }
    JsonObject jsonDashScopeResult = (JsonObject) dashScopeResult.getOutput();
    if (jsonDashScopeResult.has(RecognitionApiKeywords.SENTENCE)) {
      JsonObject timestampObject =
          jsonDashScopeResult.getAsJsonObject(RecognitionApiKeywords.SENTENCE);
      if (timestampObject != null) {
        result.sentence = Sentence.from(timestampObject);
      } else {
        result.isCompleteResult = true;
      }
    } else {
      result.isCompleteResult = true;
      result.sentence = new Sentence();
    }
    return result;
  }
}
