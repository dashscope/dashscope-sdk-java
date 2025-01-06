package com.alibaba.dashscope.audio.asr.translation.results;

import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerApiKeywords;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerUsage;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode()
public class TranslationRecognizerResult {
  @SerializedName(ApiKeywords.REQUEST_ID)
  private String requestId;

  private TranslationResult translationResult;

  private TranscriptionResult transcriptionResult;

  private TranslationRecognizerUsage usage;

  private boolean isSentenceEnd = false;
  private boolean isCompleteResult = false;

  public static TranslationRecognizerResult fromDashScopeResult(DashScopeResult dashScopeResult)
      throws ApiException {
    TranslationRecognizerResult result = new TranslationRecognizerResult();
    result.setRequestId(dashScopeResult.getRequestId());
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), TranslationRecognizerUsage.class));
    }
    JsonObject jsonDashScopeResult = (JsonObject) dashScopeResult.getOutput();
    if (jsonDashScopeResult.has(TranslationRecognizerApiKeywords.TRANSCRIPTION)
        || jsonDashScopeResult.has(TranslationRecognizerApiKeywords.TRANSLATIONS)) {
      JsonObject transcription_json = null;
      JsonArray translations_json = null;
      if (jsonDashScopeResult.has(TranslationRecognizerApiKeywords.TRANSCRIPTION)) {
        transcription_json =
            jsonDashScopeResult.getAsJsonObject(TranslationRecognizerApiKeywords.TRANSCRIPTION);
        result.transcriptionResult = TranscriptionResult.from(transcription_json);
      }
      if (jsonDashScopeResult.has(TranslationRecognizerApiKeywords.TRANSLATIONS)) {
        translations_json =
            jsonDashScopeResult.getAsJsonArray(TranslationRecognizerApiKeywords.TRANSLATIONS);
        result.translationResult = TranslationResult.from(translations_json);
      }
      if (translations_json == null && transcription_json == null) {
        result.isCompleteResult = true;
      } else {
        if (result.transcriptionResult != null && result.transcriptionResult.isSentenceEnd()) {
          result.isSentenceEnd = true;
        } else if (result.translationResult != null && result.translationResult.isSentenceEnd()) {
          result.isSentenceEnd = true;
        } else {
          result.isSentenceEnd = false;
        }
      }
    } else {
      result.isCompleteResult = true;
      result.isSentenceEnd = false;
    }
    return result;
  }
}
