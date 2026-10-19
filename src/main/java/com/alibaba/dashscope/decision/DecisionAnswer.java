// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The answer of a single decision question. Which fields are populated depends on {@link #type}:
 *
 * <ul>
 *   <li>{@code choice}: {@link #choice}, {@link #confidence}, {@link #probabilities} (keyed by
 *       option name).
 *   <li>{@code noul}: {@link #noul} only — {@link #confidence} and {@link #probabilities} are
 *       {@code null} per the documented example.
 *   <li>{@code score}: {@link #score}, {@link #confidence}, {@link #legend}, {@link #probabilities}
 *       (keyed by stringified level index {@code "0"}, {@code "1"}, ...).
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionAnswer {
  /** The question type, one of choice/noul/score. */
  private String type;

  /** The selected option name, only for choice questions. */
  private String choice;

  /** P(yes) in [0, 1], only for noul questions. */
  private Double noul;

  /**
   * Probability-weighted expectation of the level index, only for score questions. May fall between
   * two integer levels (e.g. {@code 1.43}).
   */
  private Double score;

  /** The confidence of the answer, for choice and score questions. */
  private Double confidence;

  /** The probability of each option or level; sums to 1. */
  private Map<String, Double> probabilities;

  /** Level index to level description, only for score questions. */
  private Map<String, String> legend;
}
