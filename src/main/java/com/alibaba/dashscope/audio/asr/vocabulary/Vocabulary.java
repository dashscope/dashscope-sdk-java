package com.alibaba.dashscope.audio.asr.vocabulary;

import com.alibaba.dashscope.common.DashScopeResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Vocabulary {
  @SerializedName("vocabulary_id")
  String vocabularyId;

  @SerializedName("gmt_create")
  String gmtCreate;

  @SerializedName("gmt_modified")
  String gmtModified;

  @SerializedName("status")
  String status;

  @SerializedName("target_model")
  String targetModel;

  @SerializedName("vocabulary")
  JsonArray vocabulary;

  @SerializedName("request_id")
  String requestId;

  @SerializedName("data")
  JsonObject data;

  public static Vocabulary vocabularyFromCreateResult(DashScopeResult dashScopeResult) {
    Vocabulary vocabulary = new Vocabulary();
    JsonObject output = (JsonObject) dashScopeResult.getOutput();
    if (output.has("vocabulary_id")) {
      vocabulary.vocabularyId = output.get("vocabulary_id").getAsString();
      vocabulary.requestId = dashScopeResult.getRequestId();
      vocabulary.data = output;
      return vocabulary;
    } else {
      return null;
    }
  }

  public static Vocabulary[] vocabularyListFromListResult(DashScopeResult dashScopeResult) {
    JsonObject output = (JsonObject) dashScopeResult.getOutput();
    if (output.has("vocabulary_list")) {
      int vocabularyListSize = output.getAsJsonArray("vocabulary_list").size();
      JsonArray vocabularyList = output.getAsJsonArray("vocabulary_list");
      Vocabulary[] vocabularies = new Vocabulary[vocabularyListSize];
      for (int i = 0; i < vocabularyListSize; i++) {
        JsonObject voiceJosn = vocabularyList.get(i).getAsJsonObject();
        Vocabulary vocabulary = new Vocabulary();
        vocabulary.gmtModified = voiceJosn.get("gmt_modified").getAsString();
        vocabulary.gmtCreate = voiceJosn.get("gmt_create").getAsString();
        vocabulary.vocabularyId = voiceJosn.get("vocabulary_id").getAsString();
        vocabulary.status = voiceJosn.get("status").getAsString();
        vocabulary.requestId = dashScopeResult.getRequestId();
        vocabulary.data = voiceJosn;
        vocabularies[i] = vocabulary;
      }
      return vocabularies;
    } else {
      return null;
    }
  }

  public static Vocabulary vocabularyFromQueryResult(DashScopeResult dashScopeResult) {
    Vocabulary vocabulary = new Vocabulary();
    JsonObject output = (JsonObject) dashScopeResult.getOutput();
    if (output.has("vocabulary")) {
      vocabulary.gmtModified = output.get("gmt_modified").getAsString();
      vocabulary.gmtCreate = output.get("gmt_create").getAsString();
      vocabulary.status = output.get("status").getAsString();
      vocabulary.targetModel = output.get("target_model").getAsString();
      vocabulary.vocabulary = output.getAsJsonArray("vocabulary");
      vocabulary.requestId = dashScopeResult.getRequestId();
      vocabulary.data = output;
      return vocabulary;
    } else {
      return null;
    }
  }

  public String toString() {
    return "Vocabulary{"
        + "vocabularyId='"
        + vocabularyId
        + '\''
        + ", gmtCreate='"
        + gmtCreate
        + '\''
        + ", gmtModified='"
        + gmtModified
        + '\''
        + ", status='"
        + status
        + '\''
        + ", targetModel='"
        + targetModel
        + '\''
        + ", vocabulary="
        + vocabulary
        + ", requestId='"
        + requestId
        + '\''
        + '}';
  }
}
