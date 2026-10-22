// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.multimodal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code multimodal.chat.create / createStreaming}. Wire shape: {@code {model,
 * input:{messages, ...}, parameters:{...}}} — known optional fields land in {@code parameters};
 * unknown passthrough only via {@link Builder#parameters(Map)} / {@link Builder#extraInput(Map)}.
 */
public final class MultimodalChatCreateParams {
  private final String model;
  private final List<MultimodalMessage> messages;
  private final Double temperature;
  private final Double topP;
  private final Integer topK;
  private final Integer maxTokens;
  private final Integer seed;
  private final List<String> stop;
  private final Double repetitionPenalty;
  private final Boolean enableThinking;
  private final Integer thinkingBudget;
  private final Map<String, Object> responseFormat;
  private final Object toolChoice;
  private final List<Map<String, Object>> tools;
  private final Integer n;
  private final Boolean enableSearch;
  private final Map<String, Object> searchOptions;
  private final Boolean incrementalOutput;
  private final Boolean vlHighResolutionImages;
  private final Map<String, Object> parameters;
  private final Map<String, Object> extraInput;

  private MultimodalChatCreateParams(Builder b) {
    this.model = b.model;
    this.messages = b.messages;
    this.temperature = b.temperature;
    this.topP = b.topP;
    this.topK = b.topK;
    this.maxTokens = b.maxTokens;
    this.seed = b.seed;
    this.stop = b.stop;
    this.repetitionPenalty = b.repetitionPenalty;
    this.enableThinking = b.enableThinking;
    this.thinkingBudget = b.thinkingBudget;
    this.responseFormat = b.responseFormat;
    this.toolChoice = b.toolChoice;
    this.tools = b.tools;
    this.n = b.n;
    this.enableSearch = b.enableSearch;
    this.searchOptions = b.searchOptions;
    this.incrementalOutput = b.incrementalOutput;
    this.vlHighResolutionImages = b.vlHighResolutionImages;
    this.parameters = b.parameters;
    this.extraInput = b.extraInput;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public List<MultimodalMessage> getMessages() {
    return messages;
  }

  public Double getTemperature() {
    return temperature;
  }

  public Double getTopP() {
    return topP;
  }

  public Integer getTopK() {
    return topK;
  }

  public Integer getMaxTokens() {
    return maxTokens;
  }

  public Integer getSeed() {
    return seed;
  }

  public List<String> getStop() {
    return stop;
  }

  public Double getRepetitionPenalty() {
    return repetitionPenalty;
  }

  public Boolean getEnableThinking() {
    return enableThinking;
  }

  public Integer getThinkingBudget() {
    return thinkingBudget;
  }

  public Map<String, Object> getResponseFormat() {
    return responseFormat;
  }

  public Object getToolChoice() {
    return toolChoice;
  }

  public List<Map<String, Object>> getTools() {
    return tools;
  }

  public Integer getN() {
    return n;
  }

  public Boolean getEnableSearch() {
    return enableSearch;
  }

  public Map<String, Object> getSearchOptions() {
    return searchOptions;
  }

  /**
   * Streaming output mode. For models without server-side cumulative-output support the client
   * flips the wire value to incremental and merges frames (2.x parity); see
   * {@code MultimodalChatService}.
   */
  public Boolean getIncrementalOutput() {
    return incrementalOutput;
  }

  public Boolean getVlHighResolutionImages() {
    return vlHighResolutionImages;
  }

  /** Passthrough into the wire {@code parameters} object (unknown/vendor-specific knobs). */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /** Passthrough into the wire {@code input} object. */
  public Map<String, Object> getExtraInput() {
    return extraInput;
  }

  public static final class Builder {
    private String model;
    private List<MultimodalMessage> messages;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Integer seed;
    private List<String> stop;
    private Double repetitionPenalty;
    private Boolean enableThinking;
    private Integer thinkingBudget;
    private Map<String, Object> responseFormat;
    private Object toolChoice;
    private List<Map<String, Object>> tools;
    private Integer n;
    private Boolean enableSearch;
    private Map<String, Object> searchOptions;
    private Boolean incrementalOutput;
    private Boolean vlHighResolutionImages;
    private Map<String, Object> parameters;
    private Map<String, Object> extraInput;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder messages(List<MultimodalMessage> messages) {
      this.messages = messages == null ? null : new ArrayList<>(messages);
      return this;
    }

    public Builder temperature(Double temperature) {
      this.temperature = temperature;
      return this;
    }

    public Builder topP(Double topP) {
      this.topP = topP;
      return this;
    }

    public Builder topK(Integer topK) {
      this.topK = topK;
      return this;
    }

    public Builder maxTokens(Integer maxTokens) {
      this.maxTokens = maxTokens;
      return this;
    }

    public Builder seed(Integer seed) {
      this.seed = seed;
      return this;
    }

    public Builder stop(List<String> stop) {
      this.stop = stop == null ? null : new ArrayList<>(stop);
      return this;
    }

    public Builder repetitionPenalty(Double repetitionPenalty) {
      this.repetitionPenalty = repetitionPenalty;
      return this;
    }

    public Builder enableThinking(Boolean enableThinking) {
      this.enableThinking = enableThinking;
      return this;
    }

    public Builder thinkingBudget(Integer thinkingBudget) {
      this.thinkingBudget = thinkingBudget;
      return this;
    }

    public Builder responseFormat(Map<String, Object> responseFormat) {
      this.responseFormat = responseFormat;
      return this;
    }

    /** String ("auto"/"none") or a structured tool-choice map. */
    public Builder toolChoice(Object toolChoice) {
      this.toolChoice = toolChoice;
      return this;
    }

    /** Tool schemas as wire-shaped maps. */
    public Builder tools(List<Map<String, Object>> tools) {
      this.tools = tools == null ? null : new ArrayList<>(tools);
      return this;
    }

    public Builder n(Integer n) {
      this.n = n;
      return this;
    }

    public Builder enableSearch(Boolean enableSearch) {
      this.enableSearch = enableSearch;
      return this;
    }

    public Builder searchOptions(Map<String, Object> searchOptions) {
      this.searchOptions = searchOptions;
      return this;
    }

    public Builder incrementalOutput(Boolean incrementalOutput) {
      this.incrementalOutput = incrementalOutput;
      return this;
    }

    public Builder vlHighResolutionImages(Boolean vlHighResolutionImages) {
      this.vlHighResolutionImages = vlHighResolutionImages;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder extraInput(Map<String, Object> extraInput) {
      this.extraInput = extraInput;
      return this;
    }

    public MultimodalChatCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (messages == null || messages.isEmpty()) {
        throw new IllegalArgumentException("messages must not be empty");
      }
      return new MultimodalChatCreateParams(this);
    }
  }
}
