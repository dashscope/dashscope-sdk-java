// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.aigc.generation.models;

import static com.alibaba.dashscope.utils.ApiKeywords.*;

import com.alibaba.dashscope.aigc.generation.GenerationParamBase;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class QwenParam extends GenerationParamBase {
  public static class ResultFormat {
    public static String TEXT = "text";
    public static String MESSAGE = "message";
  }

  private List<Message> messages;
  /* The maximum length of tokens to generate.
  The token count of your prompt plus max_length
  cannot exceed the model's context length. Most models
  have a context length of 2000 tokens */
  private Integer maxLength;
  /* A sampling strategy, called nucleus
  sampling, where the model considers the results of the
  tokens with top_p probability mass. So 0.1 means only
  the tokens comprising the top 10% probability mass are
  considered */
  private Double topP;

  /* A sampling strategy, the k largest elements of the
  given mass are  considered */
  private Double topK;
  /* Whether to enable web search(quark).
  Currently works best only on the first round of conversation.
  Default to False */
  @Builder.Default private Boolean enableSearch = false;
  /*
   * When generating, the seed of the random number is used to control the randomness of the model generation.
   * If you use the same seed, each run will generate the same results;
   * you can use the same seed when you need to reproduce the model's generated results.
   * The seed parameter supports unsigned 64-bit integer types. Default value 1234
   */
  private Integer seed;

  /** The output format, text or message, default message. */
  @SerializedName("result_format")
  @Default
  private String resultFormat = ResultFormat.TEXT;

  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    JsonArray requestMessages = new JsonArray();
    if (getMessages() != null && !getMessages().isEmpty()) {
      requestMessages.addAll(JsonUtils.toJsonArray(getMessages()));
      if (getPrompt() != null) {
        Message msg = Message.builder().role(Role.USER.getValue()).content(getPrompt()).build();
        requestMessages.add(JsonUtils.toJsonElement(msg));
      }
      jsonObject.add(MESSAGES, requestMessages);
    } else if (getHistory() != null && !getHistory().isEmpty()) {
      JsonArray ar = JsonUtils.toJsonElement(getHistory()).getAsJsonArray();
      jsonObject.add(HISTORY, ar);
      if (getPrompt() != null) {
        jsonObject.addProperty(PROMPT, getPrompt());
      }
    } else {
      if (getPrompt() != null) {
        jsonObject.addProperty(PROMPT, getPrompt());
      }
    }
    return jsonObject;
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (maxLength != null) {
      params.put("max_length", maxLength);
    }
    if (topP != null) {
      params.put("top_p", topP);
    }
    if (topK != null) {
      params.put("top_k", topK);
    }
    params.put("enable_search", enableSearch);
    // Server default is text.
    if (ResultFormat.MESSAGE.equals(getResultFormat())) {
      params.put("result_format", getResultFormat());
    }
    if (seed != null) {
      params.put("seed", seed);
    }
    params.putAll(parameters);
    return params;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (getPrompt() == null
        && (getHistory() == null || getHistory().isEmpty())
        && (getMessages() == null || getMessages().isEmpty())) {
      throw new InputRequiredException("messages and prompt must not all null");
    }
  }
}
