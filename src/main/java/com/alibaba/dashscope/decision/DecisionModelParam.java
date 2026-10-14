// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.annotations.NonNull;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * The input param of the decision model api.
 *
 * <p>Limits documented at
 * https://docs.bailian.console.aliyun.com/zh/model-studio/decision-model-api :
 *
 * <ul>
 *   <li>{@code state} accepts a String, an Object, or an Array; the total request must fit within
 *       65536 context tokens.
 *   <li>{@code questions} count is not hard-capped; the doc recommends &le; 16.
 *   <li>{@code choice} criteria: 1&ndash;255 options.
 *   <li>{@code score} criteria: 2&ndash;10 levels (recommended 3&ndash;7).
 * </ul>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class DecisionModelParam extends FlattenHalfDuplexParamBase {

  /** Available decision models. */
  public static class Models {
    public static final String DECISION_MODEL_PREVIEW = "decision-model-preview";

    private Models() {}
  }

  /** Documented upper bound on {@code choice} options. */
  public static final int MAX_CHOICE_OPTIONS = 255;

  /** Documented bounds on {@code score} levels. */
  public static final int MIN_SCORE_LEVELS = 2;

  public static final int MAX_SCORE_LEVELS = 10;

  /** The model name, currently only {@link Models#DECISION_MODEL_PREVIEW}. */
  @NonNull private String model;

  /**
   * The business state: a String, a {@code Map<String, Object>}, or a {@code List<?>}. The doc's
   * noul example passes a bare String; the ticket-routing example passes an object. Kept as {@link
   * Object} so all three shapes serialize verbatim.
   */
  @NonNull private Object state;

  /** The questions to answer, key is the caller-defined question id. */
  @NonNull private Map<String, DecisionQuestion> questions;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty("model", model);
    requestObject.add("state", JsonUtils.toJsonElement(state));
    JsonObject questionsObject = new JsonObject();
    for (Map.Entry<String, DecisionQuestion> entry : questions.entrySet()) {
      questionsObject.add(entry.getKey(), entry.getValue().toJson());
    }
    requestObject.add("questions", questionsObject);
    addExtraBody(requestObject);
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (model == null || model.isEmpty()) {
      throw new InputRequiredException("The model must be set");
    }
    if (state == null) {
      throw new InputRequiredException("The state must be set");
    }
    if (questions == null || questions.isEmpty()) {
      throw new InputRequiredException("The questions must not be null or empty");
    }
    for (Map.Entry<String, DecisionQuestion> entry : questions.entrySet()) {
      validateQuestion(entry.getKey(), entry.getValue());
    }
  }

  private static void validateQuestion(String id, DecisionQuestion question)
      throws InputRequiredException {
    if (question == null) {
      throw new InputRequiredException(String.format("The question [%s] must not be null", id));
    }
    String type = question.getType();
    if (!DecisionQuestion.Types.CHOICE.equals(type)
        && !DecisionQuestion.Types.NOUL.equals(type)
        && !DecisionQuestion.Types.SCORE.equals(type)) {
      throw new InputRequiredException(
          String.format("The type of question [%s] must be one of choice/noul/score", id));
    }
    // `instructions` is documented as optional; only reject when the caller went out of their way
    // to set an empty string.
    if (question.getInstructions() != null && question.getInstructions().isEmpty()) {
      throw new InputRequiredException(
          String.format("The instructions of question [%s] must not be empty", id));
    }
    Object criteria = question.getCriteria();
    if (DecisionQuestion.Types.CHOICE.equals(type)) {
      if (!(criteria instanceof Map)) {
        throw new InputRequiredException(
            String.format(
                "The criteria of choice question [%s] must be a Map<optionName, description>", id));
      }
      int size = ((Map<?, ?>) criteria).size();
      if (size < 1 || size > MAX_CHOICE_OPTIONS) {
        throw new InputRequiredException(
            String.format(
                "The criteria of choice question [%s] must contain 1..%d options, got %d",
                id, MAX_CHOICE_OPTIONS, size));
      }
    } else if (DecisionQuestion.Types.SCORE.equals(type)) {
      if (!(criteria instanceof List)) {
        throw new InputRequiredException(
            String.format(
                "The criteria of score question [%s] must be a List<String> of level descriptions",
                id));
      }
      int size = ((List<?>) criteria).size();
      if (size < MIN_SCORE_LEVELS || size > MAX_SCORE_LEVELS) {
        throw new InputRequiredException(
            String.format(
                "The criteria of score question [%s] must contain %d..%d levels, got %d",
                id, MIN_SCORE_LEVELS, MAX_SCORE_LEVELS, size));
      }
    } else if (DecisionQuestion.Types.NOUL.equals(type)
        && criteria != null
        && !(criteria instanceof Map)) {
      throw new InputRequiredException(
          String.format(
              "The criteria of noul question [%s] must be null or a Map with 'true'/'false' keys",
              id));
    }
  }
}
