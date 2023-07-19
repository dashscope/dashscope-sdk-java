package com.alibaba.dashscope.audio.asr.recognition;

import static com.alibaba.dashscope.utils.ApiKeywords.REQUEST_ID;
import static com.alibaba.dashscope.utils.ApiKeywords.TASK_GROUP;

import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RecognitionMessagePayload {
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

  public static RecognitionMessagePayload buildInputPayload(String model, JsonObject parameters) {
    RecognitionMessagePayload payLoad = new RecognitionMessagePayload();
    payLoad.setModel(model);
    payLoad.setTask(Task.ASR.getValue());
    payLoad.setTaskGroup(TaskGroup.AUDIO.getValue());
    payLoad.setFunction(Function.RECOGNITION.getValue());
    payLoad.setInput(new JsonObject());
    payLoad.setParameters(parameters);
    return payLoad;
  }
}
