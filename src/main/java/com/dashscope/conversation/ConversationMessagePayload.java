package com.dashscope.conversation;

import static com.dashscope.utils.ApiKeywords.PROMPT;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.dashscope.common.Function;
import com.dashscope.common.Task;
import com.dashscope.common.TaskGroup;
import lombok.Data;

@Data
public class ConversationMessagePayload {
  /** The model to use. */
  private String model;

  /** The task. */
  private String task;

  /** The taskGroup */
  @JSONField(name = "task_group")
  private String taskGroup;

  @JSONField(name = "request_id")
  private String requestId;

  /** The task function */
  private String function;

  /**
   * The model input, which may contain several keys: prompt: The input prompt. history: The history
   * information.
   */
  private JSONObject input;

  /** The extra parameters to use. */
  private JSONObject parameters;

  /** The model outputs. */
  private JSONObject output;

  /** The data usage. */
  private JSONObject usage;

  public static ConversationMessagePayload buildInputPayload(String model, String utterance) {
    ConversationMessagePayload payLoad = new ConversationMessagePayload();
    payLoad.setModel(model);
    payLoad.setTask(Task.TEXT_GENERATION.getValue());
    payLoad.setTaskGroup(TaskGroup.AIGC.getValue());
    payLoad.setFunction(Function.GENERATION.getValue());
    JSONObject userInput = new JSONObject();
    userInput.put(PROMPT, utterance);
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
    JSONObject userInput = new JSONObject();
    userInput.put(PROMPT, utterance);
    payLoad.setInput(userInput);
    return payLoad;
  }
}
