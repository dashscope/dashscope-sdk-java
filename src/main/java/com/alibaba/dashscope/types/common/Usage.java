// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.common;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Token usage, the single Usage class for every resource (design §3). Service-specific counters
 * (image_count for text-to-image, models for applications, per-provider billing detail) land in
 * {@link #getExtra()} and are never dropped.
 */
public class Usage {
  @JsonProperty("input_tokens")
  @JsonAlias({"prompt_tokens"})
  private int inputTokens;

  @JsonProperty("output_tokens")
  @JsonAlias({"completion_tokens"})
  private int outputTokens;

  @JsonProperty("total_tokens")
  private int totalTokens;

  private final Map<String, Object> extra = new LinkedHashMap<>();

  public int getInputTokens() {
    return inputTokens;
  }

  public int getOutputTokens() {
    return outputTokens;
  }

  public int getTotalTokens() {
    return totalTokens;
  }

  @JsonAnySetter
  public void putExtra(String key, Object value) {
    extra.put(key, value);
  }

  /** Service-specific counters not covered by the three canonical fields. */
  public Map<String, Object> getExtra() {
    return Collections.unmodifiableMap(extra);
  }
}
