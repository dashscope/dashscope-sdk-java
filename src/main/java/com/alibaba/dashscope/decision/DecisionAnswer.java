// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision;

import java.util.Map;
import lombok.Data;

/** The answer of a single decision question. */
@Data
public class DecisionAnswer {
  /** The question type, one of choice/noul/score. */
  private String type;

  /** The selected option key, only for choice questions. */
  private String choice;

  /** The probability of "yes", only for noul questions. */
  private Double noul;

  /** The expected score over the scale levels, only for score questions. */
  private Double score;

  /** The confidence of the answer, for choice and score questions. */
  private Double confidence;

  /** The probability of each option or scale level. */
  private Map<String, Double> probabilities;

  /** The description of each scale level, only for score questions. */
  private Map<String, String> legend;
}
