package com.alibaba.dashscope.audio.ttsv2.enrollment;

import com.alibaba.dashscope.common.DashScopeResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Voice {
  @SerializedName("voice_id")
  String voiceId;

  @SerializedName("gmt_create")
  String gmtCreate;

  @SerializedName("gmt_modified")
  String gmtModified;

  @SerializedName("status")
  String status;

  @SerializedName("target_model")
  String targetModel;

  @SerializedName("resource_link")
  String resourceLink;

  @SerializedName("data")
  JsonObject data;

  public static Voice voiceFromCreateResult(DashScopeResult dashScopeResult) {
    Voice voice = new Voice();
    JsonObject output = (JsonObject) dashScopeResult.getOutput();
    if (output.has("voice_id")) {
      voice.voiceId = output.get("voice_id").getAsString();
      voice.data = output;
      return voice;
    } else {
      return null;
    }
  }

  public static Voice[] voiceListFromListResult(DashScopeResult dashScopeResult) {
    JsonObject output = (JsonObject) dashScopeResult.getOutput();
    if (output.has("voice_list")) {
      int voiceListSize = output.getAsJsonArray("voice_list").size();
      JsonArray voiceList = output.getAsJsonArray("voice_list");
      Voice[] voices = new Voice[voiceListSize];
      for (int i = 0; i < voiceListSize; i++) {
        JsonObject voiceJosn = voiceList.get(i).getAsJsonObject();
        Voice voice = new Voice();
        voice.gmtModified = voiceJosn.get("gmt_modified").getAsString();
        voice.gmtCreate = voiceJosn.get("gmt_create").getAsString();
        voice.voiceId = voiceJosn.get("voice_id").getAsString();
        voice.status = voiceJosn.get("status").getAsString();
        voice.data = voiceJosn;
        voices[i] = voice;
      }
      return voices;
    } else {
      return null;
    }
  }

  public static Voice voiceFromQueryResult(DashScopeResult dashScopeResult) {
    Voice voice = new Voice();
    JsonObject output = (JsonObject) dashScopeResult.getOutput();
    if (output.has("resource_link")) {
      voice.gmtModified = output.get("gmt_modified").getAsString();
      voice.gmtCreate = output.get("gmt_create").getAsString();
      voice.status = output.get("status").getAsString();
      voice.targetModel = output.get("target_model").getAsString();
      voice.resourceLink = output.get("resource_link").getAsString();
      voice.data = output;
      return voice;
    } else {
      return null;
    }
  }

  public String toString() {
    return "Voice{"
        + "voiceId='"
        + voiceId
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
        + ", resourceLink='"
        + resourceLink
        + '\''
        + '}';
  }
}
