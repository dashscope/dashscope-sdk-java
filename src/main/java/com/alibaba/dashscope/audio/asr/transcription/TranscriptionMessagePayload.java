package com.alibaba.dashscope.audio.asr.transcription;

import static com.alibaba.dashscope.utils.ApiKeywords.REQUEST_ID;
import static com.alibaba.dashscope.utils.ApiKeywords.TASK_GROUP;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TranscriptionMessagePayload {
  /** The model to use. */
  private String model;

  /** The task. */
  private String task;

  /** The taskGroup */
  @SerializedName(TASK_GROUP)
  private String taskGroup;

  @SerializedName(REQUEST_ID)
  private String requestId;

  /** The task function */
  private String function;

  /**
   * The model input, which may contain several keys: prompt: The input prompt. history: The history
   * information.
   */
  private JsonObject input;

  /** The extra parameters to use. */
  private JsonObject parameters;

  /** The model outputs. */
  private JsonObject output;

  /** The data usage. */
  private JsonObject usage;

  public static TranscriptionMessagePayload buildInputPayload(
      String model, JsonObject input, JsonObject parameters) {
    TranscriptionMessagePayload payLoad = new TranscriptionMessagePayload();
    payLoad.setModel(model);
    payLoad.setInput(input);
    payLoad.setParameters(parameters);
    return payLoad;
  }
}
