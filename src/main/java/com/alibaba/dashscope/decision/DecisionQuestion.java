// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * A single question for the decision model. Use the static factory methods {@link #choice}, {@link
 * #noul} and {@link #score} to create instances.
 */
@Data
@Builder
public class DecisionQuestion {
  /** Question types. */
  public static final class Types {
    /** Multiple choice, criteria is a map of option name to option description. */
    public static final String CHOICE = "choice";

    /** Yes/no judgement, criteria is optional {@code {"true": ..., "false": ...}} descriptions. */
    public static final String NOUL = "noul";

    /** Ordered scale, criteria is a list of level descriptions ordered low to high. */
    public static final String SCORE = "score";

    private Types() {}
  }

  /** The question type, one of {@link Types}. */
  private String type;

  /** The question description or evaluation criteria. Documented as optional but usually set. */
  private String instructions;

  /**
   * The question criteria.
   *
   * <ul>
   *   <li>{@link Types#CHOICE}: {@code Map<String, String>} of option name to option description
   *       (1&ndash;255 entries).
   *   <li>{@link Types#SCORE}: {@code List<String>} of level descriptions ordered low to high
   *       (2&ndash;10 levels).
   *   <li>{@link Types#NOUL}: optional {@code Map<String, String>} with {@code "true"} / {@code
   *       "false"} keys.
   * </ul>
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

  /**
   * Noul question with explicit yes/no descriptions. Both arguments are optional; pass {@code null}
   * to omit either.
   */
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
    return DecisionQuestion.builder()
        .type(Types.NOUL)
        .instructions(instructions)
        .criteria(criteria)
        .build();
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
    if (instructions != null) {
      jsonObject.addProperty("instructions", instructions);
    }
    if (criteria != null) {
      jsonObject.add("criteria", JsonUtils.toJsonElement(criteria));
    }
    return jsonObject;
  }
}
