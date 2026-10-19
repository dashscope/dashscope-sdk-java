// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.decision.DecisionModel;
import com.alibaba.dashscope.decision.DecisionModelParam;
import com.alibaba.dashscope.decision.DecisionModelResult;
import com.alibaba.dashscope.decision.DecisionQuestion;
import com.alibaba.dashscope.utils.JsonUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DecisionModelUsage {
  public static void main(String[] args) throws Exception {
    Map<String, Object> state = new HashMap<>();
    state.put("ticket_id", "T-1001");
    state.put("content", "订单支付后超过 24 小时仍未到账，用户无法继续使用核心服务，要求立即处理。");

    Map<String, String> options = new LinkedHashMap<>();
    options.put("billing", "支付、退款和账单问题");
    options.put("technical", "产品故障和集成问题");

    List<String> levels =
        Arrays.asList(
            "轻微问题，不影响功能",
            "部分功能受影响，但存在替代方案",
            "核心功能不可用，没有替代方案",
            "造成严重业务或安全影响");

    Map<String, DecisionQuestion> questions = new LinkedHashMap<>();
    questions.put("department", DecisionQuestion.choice("应该由哪个团队处理？", options));
    questions.put("escalate", DecisionQuestion.noul("是否需要立即通知值班人员？"));
    questions.put("severity", DecisionQuestion.score("这个问题有多严重？", levels));

    DecisionModelParam param =
        DecisionModelParam.builder()
            .model(DecisionModelParam.Models.DECISION_MODEL_PREVIEW)
            .state(state)
            .questions(questions)
            .build();

    DecisionModel decisionModel = new DecisionModel();
    DecisionModelResult result = decisionModel.call(param);
    System.out.println(JsonUtils.toJson(result));
  }
}
