// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.translation.results;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class TranslationResult {
  private Map<String, Translation> translations = new java.util.HashMap<>();

  private boolean sentenceEnd = false;

  public Translation getTranslation(String language) {
    return translations.get(language);
  }

  public List<String> getLanguageList() {
    return new ArrayList<>(translations.keySet());
  }

  public boolean isSentenceEnd() {
    return sentenceEnd;
  }

  public static TranslationResult from(JsonArray json_array) {
    if (json_array != null) {
      TranslationResult result = new TranslationResult();
      for (int i = 0; i < json_array.size(); i++) {
        JsonObject json = json_array.get(i).getAsJsonObject();
        Translation translation = Translation.from(json);
        result.translations.put(translation.getLanguage(), translation);
        if (translation.sentenceEnd) {
          result.sentenceEnd = true;
        }
      }
      return result;
    }
    return null;
  }
}
