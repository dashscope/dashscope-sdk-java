// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.conversation;

import static com.alibaba.dashscope.utils.ApiKeywords.HISTORY;
import static com.alibaba.dashscope.utils.ApiKeywords.PROMPT;

import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.protocol.Request;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ConversationParam extends Param {

  /** The input prompt. */
  private String prompt;

  /** The chat history, if not provided, the api will use its internal */
  private List<ChatMessage> history;

  /** The extra parameters in json. */
  @Builder.Default private JsonObject parameters = new JsonObject();

  /**
   * The n-round history to use, if > 0, the conversation api will choose 2 * nHistory ChatMessage
   * to pass to the server side.
   */
  private Integer nHistory;

  protected static JsonArray prepareHistory(List<ChatMessage> history, Integer nHistory) {
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

  protected JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(PROMPT, getPrompt());
    if (history != null && !history.isEmpty()) {
      jsonObject.add(HISTORY, prepareHistory(history, nHistory));
    }
    return jsonObject;
  }

  protected JsonObject getParameters() {
    return parameters;
  }

  @Override
  public String url() {
    return String.format(
        "/services/%s/%s/%s?request_id=%s",
        TaskGroup.AIGC.getValue(),
        Task.TEXT_GENERATION.getValue(),
        Function.GENERATION.getValue(),
        this.getRequestId());
  }

  @Override
  public Request toRequest(Protocol protocol) {
    ConversationMessagePayload payload =
        ConversationMessagePayload.buildInputPayload(getModel(), getInput());
    payload.setParameters(getParameters());
    return Request.builder().message(JsonUtils.toJson(payload)).build();
  }

  @Override
  public Class<? extends Result> resultType() {
    return ConversationResult.class;
  }
}
