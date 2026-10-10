// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * A single question for the decision model. Use the static factory methods {@link #choice}, {@link
 * #noul} and {@link #score} to create instances.
 */
@Data
@SuperBuilder
public class DecisionQuestion {
  /** Question types. */
  public static class Types {
    /** Multiple choice, criteria is a map of option key to option description. */
    public static final String CHOICE = "choice";
    /** Yes/no judgement, no criteria needed. */
    public static final String NOUL = "noul";
    /** Ordered scale, criteria is a list of level descriptions. */
    public static final String SCORE = "score";
  }

  /** The question type, one of {@link Types}. */
  private String type;

  /** The question description. */
  private String instructions;

  /**
   * The question criteria. For {@link Types#CHOICE} it is a {@code Map<String, String>} of option
   * key to option description. For {@link Types#SCORE} it is a {@code List<String>} of level
   * descriptions. Not used for {@link Types#NOUL}.
   */
  private Object criteria;

  public static DecisionQuestion choice(String instructions, Map<String, String> options) {
    return DecisionQuestion.builder()
        .type(Types.CHOICE)
        .instructions(instructions)
        .criteria(options)
        .build();
  }

  public static DecisionQuestion noul(String instructions) {
    return DecisionQuestion.builder().type(Types.NOUL).instructions(instructions).build();
  }

  public static DecisionQuestion score(String instructions, List<String> levels) {
    return DecisionQuestion.builder()
        .type(Types.SCORE)
        .instructions(instructions)
        .criteria(levels)
        .build();
  }

  JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("type", type);
    jsonObject.addProperty("instructions", instructions);
    if (criteria != null) {
      jsonObject.add("criteria", JsonUtils.toJsonElement(criteria));
    }
    return jsonObject;
  }
}
