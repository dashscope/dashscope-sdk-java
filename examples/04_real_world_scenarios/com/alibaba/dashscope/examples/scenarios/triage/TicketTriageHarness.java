// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.scenarios.triage;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.DashScopeException;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.common.Usage;
import com.alibaba.dashscope.types.decision.DecisionAnswer;
import com.alibaba.dashscope.types.decision.DecisionCreateParams;
import com.alibaba.dashscope.types.decision.DecisionQuestion;
import com.alibaba.dashscope.types.decision.DecisionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Real-world scenario: decision-model ticket triage.
 *
 * <p>This is not "how to call {@code client.decision().create}". It is the production harness a
 * ticket system needs <i>around</i> that call:
 *
 * <ol>
 *   <li><b>One decision round per ticket</b> — the decision model ({@code decision-model-preview})
 *       answers a batch of typed questions about the ticket in a single forward pass: which team
 *       owns it ({@code choice}), whether to page the on-call engineer ({@code noul}) and how
 *       severe it is ({@code score}). No text is generated, so latency and cost stay flat per
 *       ticket.
 *   <li><b>A local threshold policy</b> — the model returns probability distributions, never "what
 *       to do". Harness-owned cut lines (a confidence floor, an escalation threshold, severity
 *       bands) turn distributions into routing actions, so the policy is auditable and tunable
 *       without touching the model.
 *   <li><b>Fail-closed degradation</b> — low routing confidence sends the ticket to the HUMAN
 *       queue; a failed call — an exception, or an {@code isError} envelope, which can ride HTTP
 *       200 — degrades the ticket to the same queue with the error attached. No ticket is ever
 *       auto-routed on a guess.
 *   <li><b>Per-ticket isolation + a token ledger</b> — one ticket's backend failure never stops the
 *       batch, and {@code usage.input_tokens} (the only quantity the decision model bills) is
 *       booked per ticket.
 * </ol>
 *
 * <p>It runs <b>fully offline and deterministically by default</b>: the mock seam is the one-method
 * {@link DecisionBackend}, so {@link MockDecisionBackend} serves scripted answers and injects a
 * fault on demand while the policy, degradation and ledger logic under test stays identical. Set
 * {@code TRIAGE_HARNESS_LIVE=1} to run the exact same harness against the real gateway with {@code
 * DASHSCOPE_API_KEY}.
 *
 * <p>Translation of the Python blueprint example 04_real_world_scenarios/ticket_triage_harness.py.
 */
public final class TicketTriageHarness {

  // ---- configuration (override via env) ----
  private static final String DECISION_MODEL = envOr("DASHSCOPE_MODEL", "decision-model-preview");
  private static final boolean LIVE = "1".equals(System.getenv("TRIAGE_HARNESS_LIVE"));

  // Policy thresholds: business decisions owned by the harness, not by the model. Tune them from
  // observed routing quality.
  /** Below this, routing goes to a human. */
  private static final double CONFIDENCE_FLOOR = 0.6;

  /** P(yes) required to page the on-call engineer. */
  private static final double ESCALATE_THRESHOLD = 0.8;

  /** Probability-weighted severity index cut lines. */
  private static final double P1_SCORE = 2.5;

  private static final double P2_SCORE = 1.5;

  /** Per-call timeout (live mode). */
  private static final Duration DECISION_TIMEOUT = Duration.ofSeconds(10);

  private static final String TRIAGED = "triaged";
  private static final String ESCALATED = "escalated";
  private static final String HUMAN_REVIEW = "human_review";
  private static final String DEGRADED = "degraded";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Options of the department choice question (name -> description). */
  private static final Map<String, String> DEPARTMENT_OPTIONS = new LinkedHashMap<>();

  /** Levels of the severity score question, ordered low to high. */
  private static final List<String> SEVERITY_LEVELS =
      Arrays.asList("轻微问题，不影响功能使用", "部分功能受影响，但存在替代方案", "核心功能不可用，没有替代方案", "造成严重业务资损或安全影响");

  static {
    DEPARTMENT_OPTIONS.put("billing", "支付、退款和账单问题");
    DEPARTMENT_OPTIONS.put("technical", "产品故障、报错和集成问题");
    DEPARTMENT_OPTIONS.put("logistics", "发货、运输和签收问题");
    DEPARTMENT_OPTIONS.put("other", "不属于以上任何一类的问题");
  }

  /**
   * One decision round asks every question at once; {@code answers} come back keyed by these same
   * question ids. The typed {@link DecisionQuestion} factories add local validation; {@code
   * DecisionQuestion.raw(Map)} remains the escape hatch for future question fields.
   */
  private static final Map<String, DecisionQuestion> QUESTIONS = new LinkedHashMap<>();

  static {
    QUESTIONS.put("department", DecisionQuestion.choice("应该由哪个团队处理这张工单？", DEPARTMENT_OPTIONS));
    QUESTIONS.put("escalate", DecisionQuestion.noul("是否紧急到需要立即通知值班工程师？"));
    QUESTIONS.put("severity", DecisionQuestion.score("这张工单有多严重？", SEVERITY_LEVELS));
  }

  /** A stand-in ticket intake queue; load from your ticket system in prod. */
  private static final List<Map<String, String>> TICKETS = new ArrayList<>();

  static {
    TICKETS.add(ticket("T-2001", "订单支付后超过 24 小时仍未到账，用户无法继续使用核心服务。"));
    TICKETS.add(ticket("T-2002", "快递显示已签收但本人没有收到包裹，需要核实。"));
    TICKETS.add(ticket("T-2003", "你们这个系统到底怎么回事，给个说法。"));
    TICKETS.add(ticket("T-2004", "线上服务全面报错，所有用户无法登录，疑似严重故障。"));
    TICKETS.add(ticket("T-2005", "发票抬头开错了，需要重新开具。"));
  }

  private TicketTriageHarness() {}

  private static Map<String, String> ticket(String ticketId, String content) {
    Map<String, String> ticket = new LinkedHashMap<>();
    ticket.put("ticket_id", ticketId);
    ticket.put("content", content);
    return ticket;
  }

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  // ===================== verdict & policy (local, deterministic) =====================

  /** Structured per-ticket outcome: what to do, why, and what it cost. */
  static final class TicketVerdict {
    final String ticketId;
    final String state;
    final String department;
    final String priority;
    final boolean escalate;
    final Double confidence;
    final String reason;
    final int inputTokens;
    final Double latencyMs;
    final String requestId;
    final String errorCode;

    TicketVerdict(
        String ticketId,
        String state,
        String department,
        String priority,
        boolean escalate,
        Double confidence,
        String reason,
        int inputTokens,
        Double latencyMs,
        String requestId,
        String errorCode) {
      this.ticketId = ticketId;
      this.state = state;
      this.department = department;
      this.priority = priority;
      this.escalate = escalate;
      this.confidence = confidence;
      this.reason = reason;
      this.inputTokens = inputTokens;
      this.latencyMs = latencyMs;
      this.requestId = requestId;
      this.errorCode = errorCode;
    }
  }

  /**
   * Turn answer distributions into a routing action.
   *
   * <p>Every threshold here is a local business decision: the model supplies distributions, the
   * policy owns the consequences.
   */
  static final class TriagePolicy {
    private final double floor;
    private final double escalateThreshold;

    TriagePolicy(double confidenceFloor, double escalateThreshold) {
      this.floor = confidenceFloor;
      this.escalateThreshold = escalateThreshold;
    }

    private static String priority(Double score) {
      // score is the probability-WEIGHTED level index (0-based, and it may fall between two
      // levels), not an integer class.
      if (score == null) {
        return "P3";
      }
      if (score >= P1_SCORE) {
        return "P1";
      }
      if (score >= P2_SCORE) {
        return "P2";
      }
      return "P3";
    }

    TicketVerdict decide(String ticketId, DecisionResult response) {
      Map<String, DecisionAnswer> answers = response.getAnswers();
      DecisionAnswer department = answers.get("department");
      DecisionAnswer escalate = answers.get("escalate");
      DecisionAnswer severity = answers.get("severity");
      String deptName = department == null ? null : department.getChoice();
      Double deptConf = department == null ? null : department.getConfidence();
      double pYes = escalate == null || escalate.getNoul() == null ? 0.0 : escalate.getNoul();
      Usage usage = response.getUsage();
      int inputTokens = usage == null ? 0 : usage.getInputTokens();
      String priority = priority(severity == null ? null : severity.getScore());
      boolean escalateFlag = pYes >= escalateThreshold;
      Double latencyMs = response.getLatencyMs();
      String requestId = response.getRequestId();
      // Fail closed #1: uncertain routing is worse than no routing.
      if (deptConf == null || deptConf < floor) {
        return new TicketVerdict(
            ticketId,
            HUMAN_REVIEW,
            deptName,
            priority,
            escalateFlag,
            deptConf,
            "路由置信度 " + deptConf + " 低于阈值 " + floor,
            inputTokens,
            latencyMs,
            requestId,
            null);
      }
      // Fail closed #2: a high P(yes) pages a human by design.
      if (pYes >= escalateThreshold) {
        return new TicketVerdict(
            ticketId,
            ESCALATED,
            deptName,
            priority,
            escalateFlag,
            deptConf,
            String.format("P(需值班介入)=%.2f 达到阈值 %s", pYes, escalateThreshold),
            inputTokens,
            latencyMs,
            requestId,
            null);
      }
      return new TicketVerdict(
          ticketId,
          TRIAGED,
          deptName,
          priority,
          escalateFlag,
          deptConf,
          "置信度与阈值检查均通过",
          inputTokens,
          latencyMs,
          requestId,
          null);
    }
  }

  // ===================== decision backends (the mock seam) =====================

  /** The one-method seam under test: one decision round per ticket. */
  interface DecisionBackend {
    DecisionResult decide(
        String model,
        Map<String, String> state,
        Map<String, DecisionQuestion> questions,
        Duration timeout);
  }

  /**
   * Live backend: one {@code client.decision().create} round per ticket.
   *
   * <p>Only constructed in LIVE mode, so the offline demo never touches the network. {@code
   * decision.create} is non-streaming, so the SDK's own {@code maxRetries} absorbs transient
   * 429/5xx before this ever raises. Note the endpoint lives under the compatible-mode path: the
   * SDK swaps the {@code /api/v1} suffix for it automatically.
   */
  static final class LiveDecisionBackend implements DecisionBackend {
    private final DashScopeClient client;

    LiveDecisionBackend(DashScopeClient client) {
      this.client = client;
    }

    @Override
    public DecisionResult decide(
        String model,
        Map<String, String> state,
        Map<String, DecisionQuestion> questions,
        Duration timeout) {
      return client
          .decision()
          .create(
              DecisionCreateParams.builder().model(model).state(state).questions(questions).build(),
              RequestOptions.builder().timeout(timeout).build());
    }
  }

  /**
   * One scripted outcome: department (name, confidence), escalate P(yes), severity (index, conf).
   */
  private static final class MockSpec {
    final String department;
    final double departmentConfidence;
    final double escalate;
    final double severity;
    final double severityConfidence;

    MockSpec(
        String department,
        double departmentConfidence,
        double escalate,
        double severity,
        double severityConfidence) {
      this.department = department;
      this.departmentConfidence = departmentConfidence;
      this.escalate = escalate;
      this.severity = severity;
      this.severityConfidence = severityConfidence;
    }
  }

  /**
   * Scripted per-ticket outcomes. T-2003 is deliberately vague so its low confidence exercises the
   * human-review path.
   */
  private static final Map<String, MockSpec> MOCK_SCRIPT = new LinkedHashMap<>();

  static {
    MOCK_SCRIPT.put("T-2001", new MockSpec("billing", 0.93, 0.06, 2.3, 0.81));
    MOCK_SCRIPT.put("T-2002", new MockSpec("logistics", 0.88, 0.12, 1.4, 0.77));
    MOCK_SCRIPT.put("T-2003", new MockSpec("other", 0.34, 0.08, 0.9, 0.41));
    MOCK_SCRIPT.put("T-2004", new MockSpec("technical", 0.97, 0.96, 2.9, 0.93));
  }

  /** Tickets whose decision call "fails" in the offline demo. */
  private static final Set<String> FAIL_TICKETS =
      new LinkedHashSet<>(Collections.singletonList("T-2005"));

  /**
   * Offline stand-in for the decision resource.
   *
   * <p>Serves scripted answers (mapped into the SDK's real {@link DecisionAnswer} types) keyed by
   * ticket id and raises a typed SDK error for tickets in {@code failIds}, so the demo exercises
   * the low-confidence and the degradation path without a network. Swap for {@link
   * LiveDecisionBackend} to go live.
   */
  static final class MockDecisionBackend implements DecisionBackend {
    private final Map<String, MockSpec> script;
    private final Set<String> failIds;

    MockDecisionBackend(Map<String, MockSpec> script, Set<String> failIds) {
      this.script = script;
      this.failIds = failIds;
    }

    @Override
    public DecisionResult decide(
        String model,
        Map<String, String> state,
        Map<String, DecisionQuestion> questions,
        Duration timeout) {
      String ticketId = state.getOrDefault("ticket_id", "?");
      if (failIds.contains(ticketId)) {
        throw new ApiConnectionException("mocked decision backend outage for " + ticketId, null);
      }
      ObjectNode answers = MAPPER.createObjectNode();
      answers.set("department", departmentAnswer(script.get(ticketId)));
      answers.set("escalate", escalateAnswer(script.get(ticketId)));
      answers.set("severity", severityAnswer(script.get(ticketId)));
      ObjectNode root = MAPPER.createObjectNode();
      root.set("answers", answers);
      int inputTokens = 40 + state.getOrDefault("content", "").length();
      root.putObject("usage").put("input_tokens", inputTokens);
      root.put("latency_ms", Math.min(37.5, timeout.toMillis()));
      DecisionResult result = MAPPER.convertValue(root, DecisionResult.class);
      result.fillEnvelope("mock-" + model + "-" + ticketId, 200, null, null, null);
      return result;
    }

    /** Probabilities for a choice answer: winner + equal remainder. */
    private static Map<String, Double> spread(String winner, double confidence) {
      List<String> others = new ArrayList<>();
      for (String name : DEPARTMENT_OPTIONS.keySet()) {
        if (!name.equals(winner)) {
          others.add(name);
        }
      }
      double share = others.isEmpty() ? 0.0 : round4((1.0 - confidence) / others.size());
      Map<String, Double> probs = new LinkedHashMap<>();
      for (String name : others) {
        probs.put(name, share);
      }
      probs.put(winner, confidence);
      return probs;
    }

    private static double round4(double value) {
      return Math.round(value * 10000.0) / 10000.0;
    }

    private static ObjectNode departmentAnswer(MockSpec spec) {
      ObjectNode answer = MAPPER.createObjectNode();
      answer.put("type", "choice");
      answer.put("choice", spec.department);
      answer.put("confidence", spec.departmentConfidence);
      answer.set(
          "probabilities", MAPPER.valueToTree(spread(spec.department, spec.departmentConfidence)));
      return answer;
    }

    private static ObjectNode escalateAnswer(MockSpec spec) {
      ObjectNode answer = MAPPER.createObjectNode();
      answer.put("type", "noul");
      answer.put("noul", spec.escalate);
      ObjectNode probs = answer.putObject("probabilities");
      probs.put("no", round4(1.0 - spec.escalate));
      probs.put("yes", spec.escalate);
      return answer;
    }

    private static ObjectNode severityAnswer(MockSpec spec) {
      ObjectNode answer = MAPPER.createObjectNode();
      answer.put("type", "score");
      answer.put("score", spec.severity);
      answer.put("confidence", spec.severityConfidence);
      ObjectNode legend = answer.putObject("legend");
      ObjectNode probs = answer.putObject("probabilities");
      int center = (int) Math.round(spec.severity);
      for (int i = 0; i < SEVERITY_LEVELS.size(); i++) {
        legend.put(Integer.toString(i), SEVERITY_LEVELS.get(i));
        probs.put(Integer.toString(i), round4(i == center ? spec.severityConfidence : 0.05));
      }
      return answer;
    }
  }

  // ===================== the harness =====================

  /**
   * Batch triage driver: per-ticket isolation + a token ledger.
   *
   * <p>A backend failure becomes a DEGRADED verdict for that ticket alone; the batch never stops.
   * The decision model generates no text, so {@code usage.input_tokens} is the whole bill — the
   * ledger is the cost model, exact rather than estimated.
   */
  static final class Harness {
    private final DecisionBackend backend;
    private final TriagePolicy policy;
    private final String model;
    private final Duration timeout;
    int totalInputTokens;
    int triagedOk;

    Harness(DecisionBackend backend, TriagePolicy policy, String model, Duration timeout) {
      this.backend = backend;
      this.policy = policy;
      this.model = model;
      this.timeout = timeout;
    }

    /** Triage one ticket; never raises for backend failures. */
    TicketVerdict triage(Map<String, String> ticket) {
      String ticketId = ticket.get("ticket_id");
      DecisionResult response;
      try {
        response = backend.decide(model, ticket, QUESTIONS, timeout);
      } catch (DashScopeException exc) {
        return degrade(ticketId, exc, null);
      }
      // Belt and suspenders: DashScope can report a failure with HTTP 200, so under
      // raiseOnError(false) the failure arrives as an envelope, not an exception. Never trust an
      // isError body.
      if (response.isError()) {
        return degrade(ticketId, null, response);
      }
      TicketVerdict verdict = policy.decide(ticketId, response);
      totalInputTokens += verdict.inputTokens;
      triagedOk++;
      return verdict;
    }

    List<TicketVerdict> run(List<Map<String, String>> tickets) {
      List<TicketVerdict> verdicts = new ArrayList<>();
      for (Map<String, String> ticket : tickets) {
        TicketVerdict verdict = triage(ticket);
        report(ticket, verdict);
        verdicts.add(verdict);
      }
      return verdicts;
    }

    private static TicketVerdict degrade(
        String ticketId, DashScopeException exc, DecisionResult envelope) {
      String code;
      String note;
      if (envelope != null) {
        code = envelope.getCode();
        note = envelope.getMessage();
      } else {
        code = exc.getCode() != null ? exc.getCode() : exc.getClass().getSimpleName();
        note = exc.toString();
      }
      return new TicketVerdict(
          ticketId,
          DEGRADED,
          null,
          null,
          false,
          null,
          "决策后端不可用（" + note + "），已转人工队列，不凭空猜测路由",
          0,
          null,
          null,
          code);
    }

    private static void report(Map<String, String> ticket, TicketVerdict verdict) {
      String content = ticket.get("content");
      String preview = content.length() > 22 ? content.substring(0, 22) : content;
      System.out.println("\n  工单 " + ticket.get("ticket_id") + ": " + preview + "...");
      System.out.println(
          "    状态="
              + verdict.state
              + ", 团队="
              + verdict.department
              + ", 优先级="
              + verdict.priority
              + ", 值班介入="
              + verdict.escalate);
      System.out.println(
          "    置信度="
              + verdict.confidence
              + ", input_tokens="
              + verdict.inputTokens
              + ", request_id="
              + verdict.requestId);
      System.out.println("    判定依据: " + verdict.reason);
      if (verdict.errorCode != null) {
        System.out.println("    error_code: " + verdict.errorCode);
      }
    }
  }

  // ===================== wiring & demo =====================

  /** Build the harness on a live or scripted decision backend. */
  private static Harness buildHarness() {
    DecisionBackend backend;
    if (LIVE) {
      DashScopeClient client =
          DashScopeClient.builder().apiKey(ExampleSupport.requireApiKey()).maxRetries(2).build();
      backend = new LiveDecisionBackend(client);
    } else {
      backend = new MockDecisionBackend(MOCK_SCRIPT, FAIL_TICKETS);
    }
    return new Harness(
        backend,
        new TriagePolicy(CONFIDENCE_FLOOR, ESCALATE_THRESHOLD),
        DECISION_MODEL,
        DECISION_TIMEOUT);
  }

  /** Run the batch over the five stand-in tickets and print the ledger summary. */
  public static void main(String[] args) {
    Harness harness = buildHarness();
    String mode = LIVE ? "LIVE 真实网关" : "OFFLINE 脚本化 Mock 后端";
    System.out.println("模式: " + mode + "，模型: " + DECISION_MODEL);
    System.out.println(
        "策略阈值: 置信度下限="
            + CONFIDENCE_FLOOR
            + ", 值班介入 P(yes)>="
            + ESCALATE_THRESHOLD
            + ", 严重度 P1/P2 分数线="
            + P1_SCORE
            + "/"
            + P2_SCORE);
    if (LIVE) {
      System.out.println("(LIVE 模式：全部工单走真实决策模型，无故障注入)");
    }
    List<TicketVerdict> verdicts = harness.run(TICKETS);
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (TicketVerdict verdict : verdicts) {
      counts.merge(verdict.state, 1, Integer::sum);
    }
    System.out.println("\n===== 批次汇总 =====");
    System.out.println("  工单总数: " + verdicts.size() + ", 状态分布: " + counts);
    System.out.println(
        "  自动路由成功 " + harness.triagedOk + " 单，计费 input_tokens 合计 " + harness.totalInputTokens);
    System.out.println("  说明: 决策模型只按输入 token 计费（不生成文本）；");
    System.out.println("  转人工/降级的工单不计入自动路由量。");
  }
}
