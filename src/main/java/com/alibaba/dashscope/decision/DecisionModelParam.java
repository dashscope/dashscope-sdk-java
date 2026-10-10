// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.annotations.NonNull;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** The input param of the decision model api. */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class DecisionModelParam extends FlattenHalfDuplexParamBase {

  /** Available decision models. */
  public static class Models {
    public static final String DECISION_MODEL_PREVIEW = "decision-model-preview";
  }

  /** The model name, currently only {@link Models#DECISION_MODEL_PREVIEW}. */
  @NonNull private String model;

  /** The business state, an arbitrary json object, e.g. {"ticket_id": "T-1001", ...}. */
  @NonNull private Map<String, Object> state;

  /** The questions to answer, key is the question id. */
  @NonNull private Map<String, DecisionQuestion> questions;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty("model", model);
    requestObject.add("state", JsonUtils.toJsonObject(state));
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
      DecisionQuestion question = entry.getValue();
      if (question == null) {
        throw new InputRequiredException(
            String.format("The question [%s] must not be null", entry.getKey()));
      }
      String type = question.getType();
      if (!DecisionQuestion.Types.CHOICE.equals(type)
          && !DecisionQuestion.Types.NOUL.equals(type)
          && !DecisionQuestion.Types.SCORE.equals(type)) {
        throw new InputRequiredException(
            String.format(
                "The type of question [%s] must be one of choice/noul/score", entry.getKey()));
      }
      if (question.getInstructions() == null || question.getInstructions().isEmpty()) {
        throw new InputRequiredException(
            String.format("The instructions of question [%s] must be set", entry.getKey()));
      }
      if (!DecisionQuestion.Types.NOUL.equals(type) && question.getCriteria() == null) {
        throw new InputRequiredException(
            String.format("The criteria of question [%s] must be set", entry.getKey()));
      }
    }
  }
}
