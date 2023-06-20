package com.alibaba.dashscope.audio.tts;

import static com.alibaba.dashscope.utils.ApiKeywords.REQUEST_ID;
import static com.alibaba.dashscope.utils.ApiKeywords.TASK_GROUP;

import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class SpeechSynthesisMessagePayload {
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

  /** The model input, which may contain several keys: text: The input text. */
  private JsonObject input;

  /** The extra parameters to use. */
  private JsonObject parameters;

  /** The model outputs. */
  private JsonObject output;

  /** The data usage. */
  private JsonObject usage;

  public static SpeechSynthesisMessagePayload buildInputPayload(String model, JsonObject input) {
    SpeechSynthesisMessagePayload payLoad = new SpeechSynthesisMessagePayload();
    payLoad.setModel(model);
    payLoad.setTask(Task.TEXT_TO_SPEECH.getValue());
    payLoad.setTaskGroup(TaskGroup.AUDIO.getValue());
    payLoad.setFunction(Function.SPEECH_SYNTHESIZER.getValue());
    payLoad.setInput(input);
    return payLoad;
  }
}
