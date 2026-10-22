// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.decision;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One question for the decision model (TypeSafe System One). Use {@link #choice}, {@link #noul},
 * {@link #score} for typed questions, or {@link #raw(Map)} to pass a wire-shaped question through
 * untouched — the escape hatch that keeps future question fields usable before this SDK models
 * them (raw questions skip client-side validation).
 *
 * <p>{@code criteria} shape follows {@code type}: {@code choice} takes a {@code Map<optionName,
 * description>} (1–255 options; give the full set and add an {@code other} fallback); {@code
 * score} takes a {@code List} of level descriptions ordered low to high (2–255 levels; 3–7
 * clearly distinguishable levels work best); {@code noul} takes an optional {@code Map} with
 * {@code "true"} / {@code "false"} keys.
 */
public final class DecisionQuestion {
  /** Multiple choice; the answer carries the selected option name. */
  public static final String TYPE_CHOICE = "choice";
  /** Yes/no judgement; the answer is P(yes), 0 = no and 1 = yes. */
  public static final String TYPE_NOUL = "noul";
  /** Ordinal scale; the answer is the probability-weighted level index. */
  public static final String TYPE_SCORE = "score";

  /** Documented bounds on {@code choice} options / {@code score} levels. */
  public static final int MIN_CRITERIA_SIZE = 1;

  public static final int MAX_CHOICE_OPTIONS = 255;
  public static final int MIN_SCORE_LEVELS = 2;
  public static final int MAX_SCORE_LEVELS = 255;

  private final String type;
  private final String instructions;
  private final Object criteria;
  private final Map<String, Object> raw;

  private DecisionQuestion(String type, String instructions, Object criteria) {
    this.type = type;
    this.instructions = instructions;
    this.criteria = criteria;
    this.raw = null;
  }

  private DecisionQuestion(Map<String, Object> raw) {
    this.type = null;
    this.instructions = null;
    this.criteria = null;
    this.raw = raw;
  }

  /** A pick-one-of-N question. */
  public static DecisionQuestion choice(String instructions, Map<String, String> options) {
    return new DecisionQuestion(TYPE_CHOICE, instructions, options);
  }

  /** A yes/no judgement without explicit outcome descriptions. */
  public static DecisionQuestion noul(String instructions) {
    return new DecisionQuestion(TYPE_NOUL, instructions, null);
  }

  /** A yes/no judgement; either description may be null to omit it. */
  public static DecisionQuestion noul(String instructions, String trueDesc, String falseDesc) {
    if (trueDesc == null && falseDesc == null) {
      return noul(instructions);
    }
    Map<String, String> criteria = new LinkedHashMap<>();
    if (trueDesc != null) {
      criteria.put("true", trueDesc);
    }
    if (falseDesc != null) {
      criteria.put("false", falseDesc);
    }
    return new DecisionQuestion(TYPE_NOUL, instructions, criteria);
  }

  /** An ordinal-scale rating, levels ordered low to high. */
  public static DecisionQuestion score(String instructions, List<String> levels) {
    return new DecisionQuestion(TYPE_SCORE, instructions, levels);
  }

  /** A raw wire-shaped question ({@code {type: ..., ...}}), passed through unvalidated. */
  public static DecisionQuestion raw(Map<String, Object> wire) {
    if (wire == null || wire.isEmpty()) {
      throw new IllegalArgumentException("raw question wire must not be empty");
    }
    return new DecisionQuestion(new LinkedHashMap<>(wire));
  }

  public String getType() {
    return type;
  }

  public String getInstructions() {
    return instructions;
  }

  public Object getCriteria() {
    return criteria;
  }

  /** true when this question is a raw wire passthrough. */
  public boolean isRaw() {
    return raw != null;
  }

  public Map<String, Object> getRaw() {
    return raw;
  }
}
