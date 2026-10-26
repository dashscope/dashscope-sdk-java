// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.decision;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.decision.DecisionAnswer;
import com.alibaba.dashscope.types.decision.DecisionCreateParams;
import com.alibaba.dashscope.types.decision.DecisionQuestion;
import com.alibaba.dashscope.types.decision.DecisionResult;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decision model (System One): one {@code decision().create} call answers a set of typed questions
 * about a business {@code state} in a single forward pass — multi-choice ({@code choice}), yes/no
 * ({@code noul}) and ordinal scale ({@code score}) — each answer carrying its probability
 * distribution. The model generates no text, so latency and cost do not grow with output length.
 * Typical uses: ticket routing, content moderation, agent routing. Translation of the Python
 * blueprint example 01_atomic_capabilities/decision/decision.py.
 */
public final class DecisionCreate {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "decision-model-preview");

    // The business state under decision: free text, a dialogue, or a structured object like
    // this support ticket.
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("ticket_id", "T-1001");
    state.put("content", "订单支付后超过 24 小时仍未到账，用户无法继续使用核心服务。");

    Map<String, DecisionQuestion> questions = new LinkedHashMap<>();
    // choice: pick one of the criteria options (1-255 options; give the full set and add an
    // "other" fallback).
    Map<String, String> departmentOptions = new LinkedHashMap<>();
    departmentOptions.put("billing", "支付、退款和账单问题");
    departmentOptions.put("technical", "产品故障和集成问题");
    departmentOptions.put("other", "不属于以上两类的问题");
    questions.put("department", DecisionQuestion.choice("应该由哪个团队处理？", departmentOptions));
    // noul: yes/no judgement; the answer is P(yes), 0=no, 1=yes.
    questions.put("escalate", DecisionQuestion.noul("是否需要立即通知值班人员？"));
    // score: ordinal scale described low to high (2-255 levels; 3-7 clearly distinguishable
    // levels work best).
    questions.put(
        "severity",
        DecisionQuestion.score(
            "这个问题有多严重？",
            Arrays.asList("轻微问题，不影响功能", "部分功能受影响，但存在替代方案", "核心功能不可用，没有替代方案", "造成严重业务或安全影响")));

    try (DashScopeClient client = ExampleSupport.client()) {
      System.out.println("----- decision request -----");
      DecisionResult resp =
          client
              .decision()
              .create(
                  DecisionCreateParams.builder()
                      .model(model)
                      .state(state)
                      .questions(questions)
                      .build());

      // getAnswers() keys mirror the request's question ids.
      DecisionAnswer department = resp.getAnswers().get("department");
      System.out.println(
          "department="
              + department.getChoice()
              + ", confidence="
              + department.getConfidence()
              + ", probabilities="
              + department.getProbabilities());

      DecisionAnswer escalate = resp.getAnswers().get("escalate");
      System.out.println("escalate P(yes)=" + escalate.getNoul());

      DecisionAnswer severity = resp.getAnswers().get("severity");
      // score is the probability-weighted level index and may fall between two levels; legend
      // maps an index back to its text.
      System.out.println(
          "severity="
              + severity.getScore()
              + ", confidence="
              + severity.getConfidence()
              + ", legend="
              + severity.getLegend());

      System.out.println(
          "input_tokens="
              + resp.getUsage().getInputTokens()
              + ", latency_ms="
              + resp.getLatencyMs());
    }
  }
}
