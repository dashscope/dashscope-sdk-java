// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.decision;

import com.alibaba.dashscope.types.common.ResponseBase;
import com.alibaba.dashscope.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Map;

/**
 * One System One decision round (design §3). The compatible-mode endpoint answers flat (no
 * {@code output} envelope); {@code answers} keys mirror the request's question ids. Envelope
 * fields live on {@link ResponseBase}.
 */
public class DecisionResult extends ResponseBase {
  @JsonProperty("model")
  private String model;

  @JsonProperty("answers")
  private Map<String, DecisionAnswer> answers;

  @JsonProperty("usage")
  private Usage usage;

  @JsonProperty("latency_ms")
  private Double latencyMs;

  public String getModel() {
    return model;
  }

  /** Answers keyed by the request's question ids; empty when the body omits the table. */
  public Map<String, DecisionAnswer> getAnswers() {
    return answers == null ? Collections.emptyMap() : answers;
  }

  public Usage getUsage() {
    return usage;
  }

  /** Server-side latency in milliseconds. */
  public Double getLatencyMs() {
    return latencyMs;
  }
}
