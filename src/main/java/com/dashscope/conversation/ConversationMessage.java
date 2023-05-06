package com.dashscope.conversation;

import static com.dashscope.utils.ApiKeywords.DEBUG;
import static com.dashscope.utils.ApiKeywords.HISTORY;

import com.dashscope.common.Role;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.Data;

@Data
public class ConversationMessage {

  private ConversationMessageHeader header;

  private ConversationMessagePayload payload;

  public static ConversationMessage buildInputMessageWithPrompt(
      String requestId, String model, String utterance, boolean streaming) {
    ConversationMessage input = new ConversationMessage();
    ConversationMessageHeader header =
        ConversationMessageHeader.buildInputHeader(requestId, streaming);
    input.setHeader(header);
    ConversationMessagePayload payload =
        ConversationMessagePayload.buildInputPayload(model, utterance);
    input.setPayload(payload);
    return input;
  }

  private static JsonArray prepareHistory(List<ChatMessage> history, Integer nHistory) {
    if (history == null) {
      history = Lists.newArrayList();
    }
    if (nHistory != null && nHistory != -1 && history.size() >= 2 * nHistory) {
      history = history.subList(history.size() - 2 * nHistory, history.size());
    }
    JsonArray list = new JsonArray();
    String user = null;
    String bot = null;
    for (ChatMessage message : history) {
      if (message.getRole().equals(Role.USER.getValue())) {
        user = message.getPayload();
      } else if (message.getRole().equals(Role.BOT.getValue())) {
        bot = message.getPayload();
      }
      if (user != null && bot != null) {
        JsonObject json = new JsonObject();
        json.addProperty(Role.BOT.getValue(), bot);
        json.addProperty(Role.USER.getValue(), user);
        list.add(json);
        user = null;
        bot = null;
      }
    }
    return list;
  }

  public static ConversationMessage buildInputMessageWithHistory(
      String requestId,
      String model,
      String utterance,
      List<ChatMessage> history,
      Integer nHistory,
      JsonObject parameters,
      boolean streaming,
      boolean enableDebug) {
    ConversationMessage input = new ConversationMessage();
    ConversationMessageHeader header =
        ConversationMessageHeader.buildInputHeader(requestId, streaming);
    input.setHeader(header);
    ConversationMessagePayload payload =
        ConversationMessagePayload.buildInputPayload(model, utterance);
    if (history != null && !history.isEmpty()) {
      payload.getInput().add(HISTORY, prepareHistory(history, nHistory));
    }
    payload.getInput().addProperty(DEBUG, enableDebug);
    payload.setParameters(parameters);
    input.setPayload(payload);
    return input;
  }

  public static ConversationMessage buildMultiModalInputMessageWithHistory(
      String requestId,
      String model,
      String utterance,
      List<ChatMessage> history,
      int nHistory,
      JsonObject parameters,
      boolean streaming,
      boolean enableDebug) {
    ConversationMessage input = new ConversationMessage();
    ConversationMessageHeader header =
        ConversationMessageHeader.buildInputHeader(requestId, streaming);
    input.setHeader(header);
    ConversationMessagePayload payload =
        ConversationMessagePayload.buildMultiModalInputPayload(model, utterance);
    if (history != null && !history.isEmpty()) {
      payload.getInput().add(HISTORY, prepareHistory(history, nHistory));
    }
    payload.getInput().addProperty(DEBUG, enableDebug);
    payload.setParameters(parameters);
    input.setPayload(payload);
    return input;
  }
}
