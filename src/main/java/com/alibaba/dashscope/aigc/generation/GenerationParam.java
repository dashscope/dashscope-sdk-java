// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.aigc.generation;

import static com.alibaba.dashscope.utils.ApiKeywords.HISTORY;
import static com.alibaba.dashscope.utils.ApiKeywords.MAX_TOKENS;
import static com.alibaba.dashscope.utils.ApiKeywords.MESSAGES;
import static com.alibaba.dashscope.utils.ApiKeywords.PROMPT;
import static com.alibaba.dashscope.utils.ApiKeywords.REPETITION_PENALTY;
import static com.alibaba.dashscope.utils.ApiKeywords.STOP;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResponseFormat;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.tools.ToolBase;
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
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class GenerationParam extends GenerationParamBase {
  public static class ResultFormat {
    public static String TEXT = "text";
    public static String MESSAGE = "message";
  }

  private List<Message> messages;
  /* @deprecated use maxTokens instead */
  @Deprecated private Integer maxLength;
  /* A sampling strategy, called nucleus
  sampling, where the model considers the results of the
  tokens with top_p probability mass. So 0.1 means only
  the tokens comprising the top 10% probability mass are
  considered */
  private Double topP;

  /* A sampling strategy, the k largest elements of the
  given mass are  considered */
  private Integer topK;
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

  /**
   * Used to control the degree of randomness and diversity. Specifically, the temperature value
   * controls the degree to which the probability distribution of each candidate word is smoothed
   * when generating text. A higher temperature value will reduce the peak value of the probability
   * distribution, allowing more low-probability words to be selected, and the generated results
   * will be more diverse; while a lower temperature value will enhance the peak value of the
   * probability distribution, making it easier for high-probability words to be selected, the
   * generated results are more deterministic, range(0, 2).
   */
  private Float temperature;

  /**
   * Used to control the streaming output mode. If true, the subsequent output will include the
   * previously input content by default. Otherwise, the subsequent output will not include the
   * previously output content. Default: false eg(false):
   *
   * <pre>
   * I
   * I like
   * I like apple
   * when true:
   * I
   * like
   * apple
   * </pre>
   */
  @Builder.Default private Boolean incrementalOutput = false;

  /** Maximum tokens to generate. */
  private Integer maxTokens;
  /** repetition penalty */
  private Float repetitionPenalty;

  /** stopString and token are mutually exclusive. */
  @Singular("stopString")
  private List<String> stopStrings;

  @Singular private List<List<Integer>> stopTokens;

  /** Specify which tools the model can use. */
  private List<ToolBase> tools;

  /** Specify tool choice */
  @SerializedName("tool_choice")
  protected Object toolChoice;

  /** 联网搜索的策略。仅当enable_search为true时生效。 */
  private SearchOptions searchOptions;

  /** 返回内容的格式。 */
  private ResponseFormat responseFormat;

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
    if (enableSearch) {
      params.put("enable_search", enableSearch);
    }
    // Server default is text.
    if (ResultFormat.MESSAGE.equals(getResultFormat())) {
      params.put("result_format", getResultFormat());
    }
    if (seed != null) {
      params.put("seed", seed);
    }
    if (temperature != null) {
      params.put("temperature", temperature);
    }
    if (incrementalOutput) {
      params.put("incremental_output", incrementalOutput);
    }
    if (repetitionPenalty != null) {
      params.put(REPETITION_PENALTY, repetitionPenalty);
    }
    if (maxTokens != null) {
      params.put(MAX_TOKENS, maxTokens);
    }
    if (stopStrings != null && !stopStrings.isEmpty()) {
      params.put(STOP, stopStrings);
    } else if (stopTokens != null && !stopTokens.isEmpty()) {
      params.put(STOP, stopTokens);
    }
    if (tools != null && !tools.isEmpty()) {
      params.put("tools", tools);
    }
    if (toolChoice != null) {
      if (toolChoice instanceof String) {
        params.put("tool_choice", (String) toolChoice);
      } else {
        params.put("tool_choice", JsonUtils.toJsonObject(toolChoice));
      }
    }

    if (searchOptions != null) {
      params.put("search_options", searchOptions);
    }

    if (responseFormat != null) {
      params.put("response_format", responseFormat);
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
