package com.dashscope.conversation;

import static com.dashscope.utils.ApiKeywords.DEBUG;
import static com.dashscope.utils.ApiKeywords.HISTORY;

import com.alibaba.fastjson.JSONObject;
import com.dashscope.common.Role;
import com.google.common.collect.Lists;
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

  private static List<JSONObject> prepareHistory(List<ChatMessage> history, Integer nHistory) {
    if (history == null) {
      history = Lists.newArrayList();
    }
    if (nHistory != null && nHistory != -1 && history.size() >= 2 * nHistory) {
      history = history.subList(history.size() - 2 * nHistory, history.size());
    }
    List<JSONObject> list = Lists.newArrayList();
    String user = null;
    String bot = null;
    for (ChatMessage message : history) {
      if (message.getRole().equals(Role.USER.getValue())) {
        user = message.getPayload();
      } else if (message.getRole().equals(Role.BOT.getValue())) {
        bot = message.getPayload();
      }
      if (user != null && bot != null) {
        JSONObject json = new JSONObject();
        json.put(Role.BOT.getValue(), bot);
        json.put(Role.USER.getValue(), user);
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
      JSONObject parameters,
      boolean streaming,
      boolean enableDebug) {
    ConversationMessage input = new ConversationMessage();
    ConversationMessageHeader header =
        ConversationMessageHeader.buildInputHeader(requestId, streaming);
    input.setHeader(header);
    ConversationMessagePayload payload =
        ConversationMessagePayload.buildInputPayload(model, utterance);
    if (history != null && !history.isEmpty()) {
      payload.getInput().put(HISTORY, prepareHistory(history, nHistory));
    }
    payload.getInput().put(DEBUG, enableDebug);
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
      JSONObject parameters,
      boolean streaming,
      boolean enableDebug) {
    ConversationMessage input = new ConversationMessage();
    ConversationMessageHeader header =
        ConversationMessageHeader.buildInputHeader(requestId, streaming);
    input.setHeader(header);
    ConversationMessagePayload payload =
        ConversationMessagePayload.buildMultiModalInputPayload(model, utterance);
    if (history != null && !history.isEmpty()) {
      payload.getInput().put(HISTORY, prepareHistory(history, nHistory));
    }
    payload.getInput().put(DEBUG, enableDebug);
    payload.setParameters(parameters);
    input.setPayload(payload);
    return input;
  }
}
