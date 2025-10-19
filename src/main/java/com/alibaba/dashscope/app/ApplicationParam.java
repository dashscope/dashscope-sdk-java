// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.app;

import static com.alibaba.dashscope.utils.ApiKeywords.HISTORY;
import static com.alibaba.dashscope.utils.ApiKeywords.MESSAGES;
import static com.alibaba.dashscope.utils.ApiKeywords.PROMPT;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.History;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Title Application call input parameters.<br>
 * Description Application call input parameters.<br>
 * Created at 2024-02-23 16:15
 *
 * @since jdk8
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ApplicationParam extends HalfDuplexParamBase {
  @Builder.Default private Object resources = null;

  /** Id of bailian application */
  @lombok.NonNull private String appId;

  /** prompt */
  private String prompt;

  /** chat history */
  private List<History> history;

  /** chat message */
  private List<Message> messages;

  /** Session id for storing chat history note: this will be ignored if history passed */
  private String sessionId;

  /** Flag to return rag or plugin process details */
  @Builder.Default private Boolean hasThoughts = false;

  /** The extra parameters for flow or plugin. */
  private JsonObject bizParams;

  /**
   * A sampling strategy, called nucleus sampling, where the model considers the results of the
   * tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10%
   * probability mass are considered
   */
  private Double topP;

  /** A sampling strategy, the k largest elements of the given mass are considered */
  private Integer topK;

  /**
   * When generating, the seed of the random number is used to control the randomness of the model
   * generation. If you use the same seed, each run will generate the same results; you can use the
   * same seed when you need to reproduce the model's generated results. The seed parameter supports
   * unsigned 64-bit integer types. Default value 1234
   */
  private Integer seed;

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
   * previously output content. Default: false.
   */
  @Builder.Default private Boolean incrementalOutput = false;

  /**
   * Long term memory id is used to store long term context summary between end users and assistant.
   */
  private String memoryId;

  /** image list */
  private List<String> images;

  /** file list */
  private List<String> files;

  /** rag options */
  private RagOptions ragOptions;

  /**
   * mcp server list
   */
  private List<String> mcpServers;

  /**
   * enable web search
   */
  private Boolean enableWebSearch;

  /**
   * enable system time
   */
  private Boolean enableSystemTime;

  /**
   * enable prem model calling
   */
  private Boolean enablePremium;

  /**
   * dialog round number
   */
  private Integer dialogRound;

  /**
   * model ID
   */
  private String modelId;

  /**
   * stream mode for flow agent
   */
  private FlowStreamMode flowStreamMode;

  /**
   * enable thinking mode
   */
  private Boolean enableThinking;


  @Override
  public String getModel() {
    return null;
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>(0);
    if (topP != null) {
      params.put(AppKeywords.TOP_P, topP);
    }
    if (topK != null) {
      params.put(AppKeywords.TOP_K, topK);
    }
    if (seed != null) {
      params.put(AppKeywords.SEED, seed);
    }
    if (temperature != null) {
      params.put(AppKeywords.TEMPERATURE, temperature);
    }
    if (hasThoughts != null) {
      params.put(AppKeywords.HAS_THOUGHTS, hasThoughts);
    }
    if (incrementalOutput != null) {
      params.put("incremental_output", incrementalOutput);
    }
    if (ragOptions != null) {
      params.put("rag_options", ragOptions);
    }
    if (mcpServers != null) {
      params.put(AppKeywords.MCP_SERVERS, mcpServers);
    }
    if (enableWebSearch != null) {
      params.put(AppKeywords.ENABLE_WEB_SEARCH, enableWebSearch);
    }
    if (enableSystemTime != null) {
      params.put(AppKeywords.ENABLE_SYSTEM_TIME, enableSystemTime);
    }
    if (enablePremium != null) {
      params.put(AppKeywords.ENABLE_PREMIUM, enablePremium);
    }
    if (dialogRound != null) {
      params.put(AppKeywords.DIALOG_ROUND, dialogRound);
    }
    if (modelId != null) {
      params.put(AppKeywords.MODEL_ID, modelId);
    }
    if (flowStreamMode != null) {
      params.put(AppKeywords.FLOW_STREAM_MODE, flowStreamMode.getValue());
    }
    if (enableThinking != null) {
      params.put(AppKeywords.ENABLE_THINKING, enableThinking);
    }

    params.putAll(parameters);

    return params;
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> reqHeaders = new HashMap<>();

    for (Map.Entry<String, Object> entry : headers.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue().toString();
      reqHeaders.put(key, value);
    }

    return reqHeaders;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();

    body.add(ApiKeywords.INPUT, getInput());
    Map<String, Object> params = getParameters();

    if (params != null && !params.isEmpty()) {
      body.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(params));
    }

    return body;
  }

  @Override
  public JsonObject getInput() {
    JsonObject input = new JsonObject();

    input.addProperty(AppKeywords.SESSION_ID, getSessionId());
    input.addProperty(AppKeywords.MEMORY_ID, memoryId);

    if (getMessages() != null && !getMessages().isEmpty()) {
      JsonArray messagesJson = new JsonArray();
      messagesJson.addAll(JsonUtils.toJsonArray(getMessages()));
      if (getPrompt() != null) {
        Message msg = Message.builder().role(Role.USER.getValue()).content(getPrompt()).build();
        messagesJson.add(JsonUtils.toJsonElement(msg));
      }
      input.add(MESSAGES, messagesJson);
    } else if (getHistory() != null && !getHistory().isEmpty()) {
      JsonArray historyJson = JsonUtils.toJsonElement(getHistory()).getAsJsonArray();
      input.add(HISTORY, historyJson);
      if (getPrompt() != null) {
        input.addProperty(PROMPT, getPrompt());
      }
    } else {
      if (getPrompt() != null) {
        input.addProperty(PROMPT, getPrompt());
      }
    }

    if (bizParams != null) {
      input.add(AppKeywords.BIZ_PARAMS, bizParams);
    }

    if (images != null && !images.isEmpty()) {
      JsonArray imagesJson = JsonUtils.toJsonElement(images).getAsJsonArray();
      input.add(AppKeywords.IMAGES, imagesJson);
    }

    if (files != null && !files.isEmpty()) {
      JsonArray fileListJson = JsonUtils.toJsonElement(files).getAsJsonArray();
      input.add(AppKeywords.FILE_LIST, fileListJson);
    }

    return input;
  }

  @Override
  public Object getResources() {
    return resources;
  }

  @Override
  public ByteBuffer getBinaryData() {
    return null;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (getPrompt() == null && (getMessages() == null || getMessages().isEmpty())) {
      throw new InputRequiredException("prompt or messages must not be null");
    }
  }
}
