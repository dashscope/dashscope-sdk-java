// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.tokenization;

import com.alibaba.dashscope.types.messages.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code tokenization.count}. Wire shape: {@code {model, input:{prompt}|{messages}},
 * plus an always-present {@code parameters} object. When both {@code prompt} and {@code messages}
 * are given, the prompt is appended as a trailing user message (Python v2 parity).
 */
public final class TokenizationCountParams {
  private final String model;
  private final String prompt;
  private final List<Message> messages;
  private final Map<String, Object> parameters;

  private TokenizationCountParams(Builder b) {
    this.model = b.model;
    this.prompt = b.prompt;
    this.messages = b.messages;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public String getPrompt() {
    return prompt;
  }

  public List<Message> getMessages() {
    return messages;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private String prompt;
    private List<Message> messages;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder prompt(String prompt) {
      this.prompt = prompt;
      return this;
    }

    public Builder messages(List<Message> messages) {
      this.messages = messages == null ? null : new ArrayList<>(messages);
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public TokenizationCountParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      boolean hasPrompt = prompt != null && !prompt.isEmpty();
      boolean hasMessages = messages != null && !messages.isEmpty();
      if (!hasPrompt && !hasMessages) {
        throw new IllegalArgumentException("prompt or messages is required");
      }
      return new TokenizationCountParams(this);
    }
  }
}
