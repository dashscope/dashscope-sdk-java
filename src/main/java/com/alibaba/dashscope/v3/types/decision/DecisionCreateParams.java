// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.decision;

import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code decision().create}. Builder validates required fields and typed-question
 * criteria at {@code build()} (design §3); {@link DecisionQuestion#raw} questions pass through
 * unvalidated. Wire shape: {@code {model, state, questions:{id: {type, instructions?, criteria?}}}}
 * — no input/parameters envelope on this compatible-mode endpoint.
 */
public final class DecisionCreateParams {
  private final String model;
  private final Object state;
  private final Map<String, DecisionQuestion> questions;

  private DecisionCreateParams(Builder b) {
    this.model = b.model;
    this.state = b.state;
    this.questions = b.questions;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  /**
   * The business state under decision: free text (String), a structured object (Map), or a list —
   * serialized verbatim.
   */
  public Object getState() {
    return state;
  }

  /** The questions to answer; keys are caller-defined question ids mirrored in the answers. */
  public Map<String, DecisionQuestion> getQuestions() {
    return questions;
  }

  public static final class Builder {
    private String model;
    private Object state;
    private Map<String, DecisionQuestion> questions;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder state(Object state) {
      this.state = state;
      return this;
    }

    public Builder questions(Map<String, DecisionQuestion> questions) {
      this.questions = questions;
      return this;
    }

    public DecisionCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (state == null) {
        throw new IllegalArgumentException("state is required");
      }
      if (questions == null || questions.isEmpty()) {
        throw new IllegalArgumentException("questions must not be empty");
      }
      questions.forEach(DecisionCreateParams::validateQuestion);
      return new DecisionCreateParams(this);
    }
  }

  private static void validateQuestion(String id, DecisionQuestion question) {
    if (question == null) {
      throw new IllegalArgumentException("question [" + id + "] must not be null");
    }
    if (question.isRaw()) {
      return;
    }
    String type = question.getType();
    if (!DecisionQuestion.TYPE_CHOICE.equals(type)
        && !DecisionQuestion.TYPE_NOUL.equals(type)
        && !DecisionQuestion.TYPE_SCORE.equals(type)) {
      throw new IllegalArgumentException(
          "the type of question [" + id + "] must be one of choice/noul/score");
    }
    if (question.getInstructions() != null && question.getInstructions().isEmpty()) {
      throw new IllegalArgumentException(
          "the instructions of question [" + id + "] must not be empty");
    }
    Object criteria = question.getCriteria();
    if (DecisionQuestion.TYPE_CHOICE.equals(type)) {
      if (!(criteria instanceof Map)) {
        throw new IllegalArgumentException(
            "the criteria of choice question [" + id + "] must be a Map<optionName, description>");
      }
      int size = ((Map<?, ?>) criteria).size();
      if (size < DecisionQuestion.MIN_CRITERIA_SIZE || size > DecisionQuestion.MAX_CHOICE_OPTIONS) {
        throw new IllegalArgumentException(
            "the criteria of choice question ["
                + id
                + "] must contain 1.."
                + DecisionQuestion.MAX_CHOICE_OPTIONS
                + " options, got "
                + size);
      }
    } else if (DecisionQuestion.TYPE_SCORE.equals(type)) {
      if (!(criteria instanceof List)) {
        throw new IllegalArgumentException(
            "the criteria of score question [" + id + "] must be a List of level descriptions");
      }
      int size = ((List<?>) criteria).size();
      if (size < DecisionQuestion.MIN_SCORE_LEVELS || size > DecisionQuestion.MAX_SCORE_LEVELS) {
        throw new IllegalArgumentException(
            "the criteria of score question ["
                + id
                + "] must contain "
                + DecisionQuestion.MIN_SCORE_LEVELS
                + ".."
                + DecisionQuestion.MAX_SCORE_LEVELS
                + " levels, got "
                + size);
      }
    } else if (criteria != null && !(criteria instanceof Map)) {
      throw new IllegalArgumentException(
          "the criteria of noul question ["
              + id
              + "] must be null or a Map with 'true'/'false'"
              + " keys");
    }
  }
}
