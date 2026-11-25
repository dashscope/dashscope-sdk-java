// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.completions;

import com.alibaba.dashscope.types.messages.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code chat.completions.create / createStreaming}. Builder validates required
 * fields at {@code build()} (design §3). Wire shape: {@code {model, input:{messages, ...},
 * parameters:{...}}} — known optional fields land in {@code parameters}; unknown passthrough only
 * via {@link Builder#parameters(Map)} / {@link Builder#extraInput(Map)}.
 */
public final class ChatCompletionCreateParams {
  private final String model;
  private final List<Message> messages;
  private final Double temperature;
  private final Double topP;
  private final Integer maxTokens;
  private final List<String> stop;
  private final Integer seed;
  private final Double repetitionPenalty;
  private final Double presencePenalty;
  private final Boolean enableThinking;
  private final Map<String, Object> responseFormat;
  private final Integer topK;
  private final Object toolChoice;
  private final List<Map<String, Object>> tools;
  private final Integer n;
  private final Boolean logprobs;
  private final Integer topLogprobs;
  private final Integer thinkingBudget;
  private final Boolean enableSearch;
  private final Map<String, Object> searchOptions;
  private final Boolean parallelToolCalls;
  private final Boolean incrementalOutput;
  private final Map<String, Object> parameters;
  private final Map<String, Object> extraInput;

  private ChatCompletionCreateParams(Builder b) {
    this.model = b.model;
    this.messages = b.messages;
    this.temperature = b.temperature;
    this.topP = b.topP;
    this.maxTokens = b.maxTokens;
    this.stop = b.stop;
    this.seed = b.seed;
    this.repetitionPenalty = b.repetitionPenalty;
    this.presencePenalty = b.presencePenalty;
    this.enableThinking = b.enableThinking;
    this.responseFormat = b.responseFormat;
    this.topK = b.topK;
    this.toolChoice = b.toolChoice;
    this.tools = b.tools;
    this.n = b.n;
    this.logprobs = b.logprobs;
    this.topLogprobs = b.topLogprobs;
    this.thinkingBudget = b.thinkingBudget;
    this.enableSearch = b.enableSearch;
    this.searchOptions = b.searchOptions;
    this.parallelToolCalls = b.parallelToolCalls;
    this.incrementalOutput = b.incrementalOutput;
    this.parameters = b.parameters;
    this.extraInput = b.extraInput;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public List<Message> getMessages() {
    return messages;
  }

  public Double getTemperature() {
    return temperature;
  }

  public Double getTopP() {
    return topP;
  }

  public Integer getMaxTokens() {
    return maxTokens;
  }

  public List<String> getStop() {
    return stop;
  }

  public Integer getSeed() {
    return seed;
  }

  public Double getRepetitionPenalty() {
    return repetitionPenalty;
  }

  public Double getPresencePenalty() {
    return presencePenalty;
  }

  public Boolean getEnableThinking() {
    return enableThinking;
  }

  public Map<String, Object> getResponseFormat() {
    return responseFormat;
  }

  public Integer getTopK() {
    return topK;
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

  public Boolean getLogprobs() {
    return logprobs;
  }

  public Integer getTopLogprobs() {
    return topLogprobs;
  }

  public Integer getThinkingBudget() {
    return thinkingBudget;
  }

  public Boolean getEnableSearch() {
    return enableSearch;
  }

  public Map<String, Object> getSearchOptions() {
    return searchOptions;
  }

  public Boolean getParallelToolCalls() {
    return parallelToolCalls;
  }

  /**
   * Streaming output mode. Note: M1 passes this through to the wire as-is; client-side cumulative
   * merging (incremental_output=false semantics for models without server support) arrives with M2.
   */
  public Boolean getIncrementalOutput() {
    return incrementalOutput;
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
    private List<Message> messages;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private List<String> stop;
    private Integer seed;
    private Double repetitionPenalty;
    private Double presencePenalty;
    private Boolean enableThinking;
    private Map<String, Object> responseFormat;
    private Integer topK;
    private Object toolChoice;
    private List<Map<String, Object>> tools;
    private Integer n;
    private Boolean logprobs;
    private Integer topLogprobs;
    private Integer thinkingBudget;
    private Boolean enableSearch;
    private Map<String, Object> searchOptions;
    private Boolean parallelToolCalls;
    private Boolean incrementalOutput;
    private Map<String, Object> parameters;
    private Map<String, Object> extraInput;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder messages(List<Message> messages) {
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

    public Builder maxTokens(Integer maxTokens) {
      this.maxTokens = maxTokens;
      return this;
    }

    public Builder stop(List<String> stop) {
      this.stop = stop == null ? null : new ArrayList<>(stop);
      return this;
    }

    public Builder seed(Integer seed) {
      this.seed = seed;
      return this;
    }

    public Builder repetitionPenalty(Double repetitionPenalty) {
      this.repetitionPenalty = repetitionPenalty;
      return this;
    }

    public Builder presencePenalty(Double presencePenalty) {
      this.presencePenalty = presencePenalty;
      return this;
    }

    public Builder enableThinking(Boolean enableThinking) {
      this.enableThinking = enableThinking;
      return this;
    }

    public Builder responseFormat(Map<String, Object> responseFormat) {
      this.responseFormat = responseFormat;
      return this;
    }

    public Builder topK(Integer topK) {
      this.topK = topK;
      return this;
    }

    /** String ("auto"/"none") or a structured tool-choice map. */
    public Builder toolChoice(Object toolChoice) {
      this.toolChoice = toolChoice;
      return this;
    }

    /** Tool schemas as wire-shaped maps (structured Tool types arrive with multimodal in M2). */
    public Builder tools(List<Map<String, Object>> tools) {
      this.tools = tools == null ? null : new ArrayList<>(tools);
      return this;
    }

    public Builder n(Integer n) {
      this.n = n;
      return this;
    }

    public Builder logprobs(Boolean logprobs) {
      this.logprobs = logprobs;
      return this;
    }

    public Builder topLogprobs(Integer topLogprobs) {
      this.topLogprobs = topLogprobs;
      return this;
    }

    public Builder thinkingBudget(Integer thinkingBudget) {
      this.thinkingBudget = thinkingBudget;
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

    public Builder parallelToolCalls(Boolean parallelToolCalls) {
      this.parallelToolCalls = parallelToolCalls;
      return this;
    }

    public Builder incrementalOutput(Boolean incrementalOutput) {
      this.incrementalOutput = incrementalOutput;
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

    public ChatCompletionCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (messages == null || messages.isEmpty()) {
        throw new IllegalArgumentException("messages must not be empty");
      }
      return new ChatCompletionCreateParams(this);
    }
  }
}
