// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.scenarios.customerservice;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.completions.ChatCompletionMessage;
import com.alibaba.dashscope.types.messages.Message;
import com.alibaba.dashscope.types.messages.ToolCall;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Real-world scenario: an online customer-service assistant with guardrails, tool access and
 * multi-turn memory.
 *
 * <p>This is a complete, production-shaped case rather than a one-line model call. It stitches
 * together the patterns a real support bot needs:
 *
 * <ul>
 *   <li><b>Guardrailed persona</b> — the system prompt scopes the bot to order / logistics / refund
 *       topics, forbids inventing order data, and tells it when to escalate to a human.
 *   <li><b>Business tools</b> — the model reads real order state through {@code query_order} /
 *       {@code track_shipment} / {@code check_refund_eligible} and can open a support ticket via
 *       {@code create_ticket} (all backed here by a small in-memory "database"; swap in your own
 *       service calls).
 *   <li><b>Sliding-window memory</b> — {@link ConversationMemory} pins the system prompt, keeps
 *       only the most recent turns under a budget, and never splits an assistant tool-call turn
 *       from its tool results, so a long chat cannot blow the context window or corrupt the wire
 *       format.
 *   <li><b>Managed tool-call loop</b> — the classic agent state machine with a {@code MAX_TURNS}
 *       guard and defensive JSON handling.
 * </ul>
 *
 * <p>The demo runs a three-turn conversation where turn 2 ("can I refund <i>it</i>?") only makes
 * sense if the bot remembers the order id from turn 1, which is exactly what the memory layer buys
 * you.
 *
 * <p>Translation of the Python blueprint example
 * 04_real_world_scenarios/customer_service_assistant.py.
 *
 * <p>Requires DASHSCOPE_API_KEY (it calls the real chat endpoint).
 */
public final class CustomerServiceAssistant {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String MODEL = envOr("DASHSCOPE_MODEL", "qwen3.7-plus");

  /** Hard stop so a misbehaving model cannot loop on tools forever. */
  private static final int MAX_TURNS = 6;

  /**
   * Number of post-system messages kept in context; older turns are dropped oldest-first. Tune
   * against your model's context window.
   */
  private static final int MAX_HISTORY_TURNS = 20;

  private static final String SYSTEM_PROMPT =
      "你是「示例商城」的在线客服助手。请严格遵守以下规则：\n"
          + "1. 只回答与订单、物流、退款、售后相关的问题；对无关问题礼貌说明"
          + "无法处理并引导用户回到售后话题。\n"
          + "2. 涉及具体订单时，必须调用工具查询真实数据后再回答，"
          + "禁止编造订单状态、物流或退款结论。\n"
          + "3. 当无法解决问题、或用户明确要求人工时，调用 create_ticket "
          + "创建工单，并把工单号告诉用户。\n"
          + "4. 回答简洁、友好，使用中文。";

  /** Refund window in days after shipping, per store policy. */
  private static final int REFUND_WINDOW_DAYS = 7;

  /** One row of the stand-in order database. */
  private static final class OrderInfo {
    final String item;
    final double amount;
    final String status;
    final int shippedDays;
    final String carrier;
    final String trackingNo;
    final String latestEvent;
    final boolean signed;

    OrderInfo(
        String item,
        double amount,
        String status,
        int shippedDays,
        String carrier,
        String trackingNo,
        String latestEvent,
        boolean signed) {
      this.item = item;
      this.amount = amount;
      this.status = status;
      this.shippedDays = shippedDays;
      this.carrier = carrier;
      this.trackingNo = trackingNo;
      this.latestEvent = latestEvent;
      this.signed = signed;
    }
  }

  /** A tiny stand-in order database (replace with your real backend). */
  private static final Map<String, OrderInfo> ORDERS = new LinkedHashMap<>();

  static {
    ORDERS.put(
        "SO2024001",
        new OrderInfo(
            "无线蓝牙耳机", 299.0, "已发货", 2, "示例A速运", "EX1234567890", "【杭州市】快件已到达杭州转运中心", false));
    ORDERS.put(
        "SO2024002",
        new OrderInfo("机械键盘", 459.0, "已签收", 12, "示例B物流", "EX9988776655", "【北京市】已签收，签收人：本人", true));
  }

  private CustomerServiceAssistant() {}

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  // ---------------- business tools (backed by the in-memory "database") ----------------

  /** Return the core state of one order. */
  private static Map<String, Object> queryOrder(String orderId) {
    OrderInfo order = ORDERS.get(orderId);
    Map<String, Object> result = new LinkedHashMap<>();
    if (order == null) {
      result.put("found", false);
      result.put("message", "未找到订单 " + orderId);
      return result;
    }
    result.put("found", true);
    result.put("order_id", orderId);
    result.put("item", order.item);
    result.put("amount", order.amount);
    result.put("status", order.status);
    return result;
  }

  /** Return carrier / tracking / latest logistics event. */
  private static Map<String, Object> trackShipment(String orderId) {
    OrderInfo order = ORDERS.get(orderId);
    Map<String, Object> result = new LinkedHashMap<>();
    if (order == null) {
      result.put("found", false);
      result.put("message", "未找到订单 " + orderId);
      return result;
    }
    result.put("found", true);
    result.put("order_id", orderId);
    result.put("carrier", order.carrier);
    result.put("tracking_no", order.trackingNo);
    result.put("latest_event", order.latestEvent);
    return result;
  }

  /** Decide refund eligibility from the store's 7-day policy. */
  private static Map<String, Object> checkRefundEligible(String orderId) {
    OrderInfo order = ORDERS.get(orderId);
    Map<String, Object> result = new LinkedHashMap<>();
    if (order == null) {
      result.put("found", false);
      result.put("eligible", false);
      result.put("reason", "未找到订单 " + orderId);
      return result;
    }
    if (order.shippedDays <= REFUND_WINDOW_DAYS && !order.signed) {
      result.put("found", true);
      result.put("eligible", true);
      result.put("reason", "发货未超过 7 天且未签收，可申请退款");
      return result;
    }
    String reason;
    if (order.signed) {
      reason = "订单已签收，需走退货流程，建议转人工处理";
    } else {
      reason = "发货已超过 " + REFUND_WINDOW_DAYS + " 天，暂不支持自助退款";
    }
    result.put("found", true);
    result.put("eligible", false);
    result.put("reason", reason);
    return result;
  }

  /** Open a human-support ticket; returns the ticket id. */
  private static Map<String, Object> createTicket(String orderId, String reason) {
    String ticketId =
        String.format("TK%05d", ((orderId + reason).hashCode() & 0x7fffffff) % 100000);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("created", true);
    result.put("ticket_id", ticketId);
    result.put("order_id", orderId);
    result.put("reason", reason);
    return result;
  }

  // ---------------- tool registry ----------------

  /**
   * Build one OpenAI function-schema tool entry. Each {@code param} is a {@code {name,
   * description}} pair; all parameters are required strings.
   */
  private static Map<String, Object> functionTool(
      String name, String description, String[]... params) {
    Map<String, Object> properties = new LinkedHashMap<>();
    List<String> required = new ArrayList<>();
    for (String[] param : params) {
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("type", "string");
      schema.put("description", param[1]);
      properties.put(param[0], schema);
      required.add(param[0]);
    }
    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("type", "object");
    parameters.put("properties", properties);
    parameters.put("required", required);
    Map<String, Object> function = new LinkedHashMap<>();
    function.put("name", name);
    function.put("description", description);
    function.put("parameters", parameters);
    Map<String, Object> tool = new LinkedHashMap<>();
    tool.put("type", "function");
    tool.put("function", function);
    return tool;
  }

  /**
   * Tool schemas the model may call (OpenAI function shape, accepted by the native gateway too).
   */
  private static final List<Map<String, Object>> TOOLS =
      Arrays.asList(
          functionTool(
              "query_order", "查询订单的商品、金额与当前状态", new String[] {"order_id", "订单号，例如 SO2024001"}),
          functionTool("track_shipment", "查询订单的承运商、运单号与最新物流轨迹", new String[] {"order_id", "订单号"}),
          functionTool(
              "check_refund_eligible", "按商城退款政策判断订单是否可退款并给出原因", new String[] {"order_id", "订单号"}),
          functionTool(
              "create_ticket",
              "创建人工客服工单，用于无法自助解决或用户要求转人工",
              new String[] {"order_id", "相关订单号"},
              new String[] {"reason", "转人工的原因，简述用户诉求"}));

  /** Tool implementations, dispatched by name from parsed JSON arguments. */
  private static final Map<String, Function<Map<String, Object>, Map<String, Object>>> TOOL_IMPL =
      new LinkedHashMap<>();

  static {
    TOOL_IMPL.put("query_order", args -> queryOrder((String) args.get("order_id")));
    TOOL_IMPL.put("track_shipment", args -> trackShipment((String) args.get("order_id")));
    TOOL_IMPL.put(
        "check_refund_eligible", args -> checkRefundEligible((String) args.get("order_id")));
    TOOL_IMPL.put(
        "create_ticket",
        args -> createTicket((String) args.get("order_id"), (String) args.get("reason")));
  }

  // ---------------- sliding-window memory ----------------

  /**
   * Sliding-window history that pins the system prompt.
   *
   * <p>Two production concerns are handled here:
   *
   * <ul>
   *   <li><b>Context budget</b> — only the most recent {@code maxTurns} messages after the system
   *       prompt are kept, so a long conversation cannot exceed the model's window.
   *   <li><b>Wire integrity</b> — trimming from the front can leave an orphan {@code role="tool"}
   *       message whose {@code assistant} tool-call turn was dropped, which the API rejects. {@link
   *       #trim()} drops any leading tool messages so every request stays well-formed.
   * </ul>
   */
  static final class ConversationMemory {
    private final Message system;
    private final List<Message> turns = new ArrayList<>();
    private final int maxTurns;

    ConversationMemory(String systemPrompt) {
      this(systemPrompt, MAX_HISTORY_TURNS);
    }

    ConversationMemory(String systemPrompt, int maxTurns) {
      this.system = Message.system(systemPrompt);
      this.maxTurns = maxTurns;
    }

    /** Append one message and re-apply the window. */
    void add(Message message) {
      turns.add(message);
      trim();
    }

    private void trim() {
      while (turns.size() > maxTurns) {
        turns.remove(0);
      }
      while (!turns.isEmpty() && "tool".equals(turns.get(0).getRole())) {
        turns.remove(0);
      }
    }

    /** The full wire-ready message list (system + kept turns). */
    List<Message> messages() {
      List<Message> all = new ArrayList<>(turns.size() + 1);
      all.add(system);
      all.addAll(turns);
      return all;
    }
  }

  // ---------------- tool-call loop ----------------

  /** Execute one tool call, always returning a JSON string result. */
  private static String runTool(String name, String argumentsJson) {
    Map<String, Object> args;
    try {
      args =
          MAPPER.readValue(
              argumentsJson == null || argumentsJson.isEmpty() ? "{}" : argumentsJson,
              new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      return errorJson("invalid JSON arguments");
    }
    Function<Map<String, Object>, Map<String, Object>> impl = TOOL_IMPL.get(name);
    if (impl == null) {
      return errorJson("unknown tool: " + name);
    }
    try {
      return MAPPER.writeValueAsString(impl.apply(args));
    } catch (JsonProcessingException | RuntimeException e) {
      return errorJson("bad arguments: " + e.getMessage());
    }
  }

  private static String errorJson(String message) {
    try {
      return MAPPER.writeValueAsString(Collections.singletonMap("error", message));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("failed to serialize tool error", e);
    }
  }

  /**
   * Process one user message through the tool-call loop.
   *
   * <p>The whole context is re-sent each turn (the API is stateless); the model keeps calling tools
   * until it returns a plain answer, which is recorded back into {@code memory} and returned.
   */
  static String handleUserTurn(
      DashScopeClient client, ConversationMemory memory, String userInput) {
    memory.add(Message.user(userInput));
    for (int turn = 0; turn < MAX_TURNS; turn++) {
      // Non-streaming on purpose: a tool-call turn must be read as a complete message before we
      // can execute it. For a pure-chat streaming reply see 01_atomic_capabilities.
      ChatCompletion resp =
          client
              .chat()
              .completions()
              .create(
                  ChatCompletionCreateParams.builder()
                      .model(MODEL)
                      .messages(memory.messages())
                      .tools(TOOLS)
                      .build());
      ChatCompletionMessage message = resp.getChoices().get(0).getMessage();
      if (message.getToolCalls() == null || message.getToolCalls().isEmpty()) {
        String answer = message.getContent() == null ? "" : message.getContent();
        memory.add(Message.assistant(answer));
        return answer;
      }

      // Echo the assistant tool-call turn verbatim, then feed back each tool result. Both go
      // through memory so the window and wire-integrity rules keep applying.
      memory.add(Message.assistant(message.getContent(), message.getToolCalls()));
      for (ToolCall call : message.getToolCalls()) {
        String result = runTool(call.getFunction().getName(), call.getFunction().getArguments());
        System.out.println(
            "    · [turn "
                + turn
                + "] tool "
                + call.getFunction().getName()
                + "("
                + call.getFunction().getArguments()
                + ") -> "
                + result);
        memory.add(Message.tool(result, call.getFunction().getName()));
      }
    }
    String fallback = "抱歉，我暂时无法处理该请求，正在为您转接人工客服。";
    memory.add(Message.assistant(fallback));
    return fallback;
  }

  /** Run a three-turn support conversation against the real API. */
  public static void main(String[] args) {
    try (DashScopeClient client = ExampleSupport.client()) {
      ConversationMemory memory = new ConversationMemory(SYSTEM_PROMPT);

      // Turn 2 refers to "this order" without repeating the id: it only works because
      // ConversationMemory carries turn 1 forward.
      List<String> conversation = Arrays.asList("我的订单 SO2024001 到哪了？", "那这个订单现在能退款吗？", "帮我转人工吧。");
      for (String userInput : conversation) {
        System.out.println("\n用户: " + userInput);
        String answer = handleUserTurn(client, memory, userInput);
        System.out.println("客服: " + answer);
      }

      System.out.println("\n----- 对话结束，上下文保留 " + memory.messages().size() + " 条消息 -----");
    }
  }
}
