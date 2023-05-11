package com.alibaba.dashscope.conversation;

import static com.alibaba.dashscope.utils.ApiKeywords.PROMPT;
import static com.alibaba.dashscope.utils.ApiKeywords.REQUEST_ID;
import static com.alibaba.dashscope.utils.ApiKeywords.TASK_GROUP;

import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ConversationMessagePayload {
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

  public static ConversationMessagePayload buildInputPayload(String model, String utterance) {
    ConversationMessagePayload payLoad = new ConversationMessagePayload();
    payLoad.setModel(model);
    payLoad.setTask(Task.TEXT_GENERATION.getValue());
    payLoad.setTaskGroup(TaskGroup.AIGC.getValue());
    payLoad.setFunction(Function.GENERATION.getValue());
    JsonObject userInput = new JsonObject();
    userInput.addProperty(PROMPT, utterance);
    payLoad.setInput(userInput);
    return payLoad;
  }

  public static ConversationMessagePayload buildMultiModalInputPayload(
      String model, String utterance) {
    ConversationMessagePayload payLoad = new ConversationMessagePayload();
    payLoad.setModel(model);
    payLoad.setTask(Task.CHAT.getValue());
    payLoad.setTaskGroup(TaskGroup.AIGC.getValue());
    payLoad.setFunction(Function.MULTIMODAL_CHAT.getValue());
    JsonObject userInput = new JsonObject();
    userInput.addProperty(PROMPT, utterance);
    payLoad.setInput(userInput);
    return payLoad;
  }
}
