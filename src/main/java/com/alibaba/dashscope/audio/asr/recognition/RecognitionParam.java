// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.recognition;

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
public class RecognitionParam extends FullDuplexServiceParam {

  @Builder.Default private boolean disfluencyRemovalEnabled = false;

  @NonNull private Integer sampleRate;

  @NonNull private String format;

  private String phraseId;

  private String vocabularyId;

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put(RecognitionApiKeywords.FORMAT, format);
    params.put(RecognitionApiKeywords.SAMPLE_RATE, sampleRate);
    params.put(RecognitionApiKeywords.DISFLUENCY_REMOVAL_ENABLED, disfluencyRemovalEnabled);
    if (vocabularyId != null) {
      params.put(RecognitionApiKeywords.VOCABULARY_ID, vocabularyId);
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
