// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.translation;

import com.alibaba.dashscope.audio.asr.phrase.AsrPhraseApiKeywords;
import com.alibaba.dashscope.base.FullDuplexServiceParam;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
public class TranslationRecognizerParam extends FullDuplexServiceParam {

  @Builder.Default private boolean disfluencyRemovalEnabled = false;

  @NonNull private Integer sampleRate;

  @NonNull private String format;

  private String phraseId;

  private String vocabularyId;

  private boolean transcriptionEnabled = true;
  private String sourceLanguage = null;
  private boolean translationEnabled = false;
  private String[] translationLanguages = null;
  private boolean semanticPunctationEnabled = false;
  private Integer maxEndSilence = null;

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put(TranslationRecognizerApiKeywords.FORMAT, format);
    params.put(TranslationRecognizerApiKeywords.SAMPLE_RATE, sampleRate);
    params.put(
        TranslationRecognizerApiKeywords.DISFLUENCY_REMOVAL_ENABLED, disfluencyRemovalEnabled);
    params.put(TranslationRecognizerApiKeywords.TRANSCRIPTION_ENABLED, transcriptionEnabled);
    if (sourceLanguage != null) {
      params.put(TranslationRecognizerApiKeywords.TRANSLATE_SOURCE_LANGUAGE, sourceLanguage);
    }
    params.put(TranslationRecognizerApiKeywords.TRANSLATION_ENABLED, translationEnabled);
    if (translationEnabled && translationLanguages != null && translationLanguages.length > 0) {
      params.put(TranslationRecognizerApiKeywords.TRANSLATE_TARGET_LANGUAGES, translationLanguages);
    }
    params.put(
        TranslationRecognizerApiKeywords.SEMANTIC_PUNCTUATION_ENABLED, semanticPunctationEnabled);
    if (vocabularyId != null) {
      params.put(TranslationRecognizerApiKeywords.VOCABULARY_ID, vocabularyId);
    }
    if (maxEndSilence != null) {
      params.put(TranslationRecognizerApiKeywords.MAX_END_SILENCE, maxEndSilence);
    }
    params.putAll(parameters);
    return params;
  }

  @Override
  public Object getResources() {
    if (phraseId == null || phraseId.isEmpty()) {
      return null;
    }
    JsonElement jsonResources = new JsonArray();
    JsonObject jsonPhraseResource = new JsonObject();
    jsonPhraseResource.addProperty(ApiKeywords.RESOURCE_ID, phraseId);
    jsonPhraseResource.addProperty(
        ApiKeywords.RESOURCE_TYPE, AsrPhraseApiKeywords.RESOURCE_TYPE_PHRASE);
    jsonResources.getAsJsonArray().add(jsonPhraseResource);
    return jsonResources;
  }

  @Override
  public Flowable<Object> getStreamingData() {
    return null;
  }
}
