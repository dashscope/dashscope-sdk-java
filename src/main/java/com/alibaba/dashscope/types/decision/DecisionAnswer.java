// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.decision;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The answer to one question; the filled field follows {@link #getType()}:
 *
 * <ul>
 *   <li>{@code choice}: {@link #getChoice()}, {@link #getConfidence()}, {@link #getProbabilities()}
 *       keyed by option name.
 *   <li>{@code noul}: {@link #getNoul()} only (P(yes), 0 = no and 1 = yes).
 *   <li>{@code score}: {@link #getScore()} (probability-weighted level index, may fall between two
 *       levels, e.g. {@code 1.43}), {@link #getConfidence()}, {@link #getLegend()}, {@link
 *       #getProbabilities()} keyed by stringified level index.
 * </ul>
 */
public final class DecisionAnswer {
  @JsonProperty("type")
  private String type;

  @JsonProperty("choice")
  private String choice;

  @JsonProperty("noul")
  private Double noul;

  @JsonProperty("score")
  private Double score;

  /** Every option's / level's probability; sums to 1. */
  @JsonProperty("probabilities")
  private Map<String, Double> probabilities;

  /** Returned for {@code choice} / {@code score} only. */
  @JsonProperty("confidence")
  private Double confidence;

  /** Maps a {@code score} level index back to its description. */
  @JsonProperty("legend")
  private Map<String, String> legend;

  private final Map<String, Object> extra = new LinkedHashMap<>();

  public String getType() {
    return type;
  }

  public String getChoice() {
    return choice;
  }

  public Double getNoul() {
    return noul;
  }

  public Double getScore() {
    return score;
  }

  public Map<String, Double> getProbabilities() {
    return probabilities;
  }

  public Double getConfidence() {
    return confidence;
  }

  public Map<String, String> getLegend() {
    return legend;
  }

  /** Fields this SDK version does not model yet (forward compatibility, design §4). */
  public Map<String, Object> getExtra() {
    return Collections.unmodifiableMap(extra);
  }

  @JsonAnySetter
  public void putExtra(String key, Object value) {
    extra.put(key, value);
  }
}
