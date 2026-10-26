// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.recipes.chat;

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
 * Agent recipe: a managed multi-turn tool-call loop — the classic agent state machine. Send the
 * conversation plus a tool schema; whenever the model asks to call a tool ({@code
 * message.tool_calls}), execute it locally, append the result as a {@code role="tool"} message, and
 * call the model again — until it returns a plain answer with no further tool calls. A {@code
 * MAX_TURNS} guard stops a misbehaving model from looping forever.
 *
 * <p>Translation of the Python blueprint example 03_agent_recipes/tool_call_loop.py.
 *
 * <p>Requires DASHSCOPE_API_KEY (it calls the real chat endpoint).
 */
public final class ToolCallLoop {

  private static final String MODEL = envOr("DASHSCOPE_MODEL", "qwen3.7-plus");
  private static final int MAX_TURNS = 5;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ToolCallLoop() {}

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  /** One OpenAI-shaped function tool schema with a single string parameter. */
  private static Map<String, Object> functionTool(
      String name, String description, String paramName, String paramDescription) {
    Map<String, Object> property = new LinkedHashMap<>();
    property.put("type", "string");
    property.put("description", paramDescription);
    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("type", "object");
    parameters.put("properties", Collections.singletonMap(paramName, property));
    parameters.put("required", Collections.singletonList(paramName));
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
   * Tool schemas the model may choose to call (OpenAI function shape, which the native gateway also
   * accepts).
   */
  private static final List<Map<String, Object>> TOOLS =
      Arrays.asList(
          functionTool("get_weather", "查询某个城市当前的天气", "city", "城市名，例如 杭州"),
          functionTool("get_population", "查询某个城市的常住人口（万人）", "city", "城市名"));

  // ---- local tool implementations (stand-ins for real services) ---- //

  private static String getWeather(String city) {
    Map<String, String> table = new LinkedHashMap<>();
    table.put("杭州", "晴，26℃");
    table.put("北京", "多云，21℃");
    return table.getOrDefault(city, "未知");
  }

  private static int getPopulation(String city) {
    Map<String, Integer> table = new LinkedHashMap<>();
    table.put("杭州", 1237);
    table.put("北京", 2184);
    return table.getOrDefault(city, 0);
  }

  private static final Map<String, Function<Map<String, Object>, Object>> TOOL_IMPL =
      new LinkedHashMap<>();

  static {
    TOOL_IMPL.put("get_weather", args -> getWeather((String) args.get("city")));
    TOOL_IMPL.put("get_population", args -> getPopulation((String) args.get("city")));
  }

  /** Execute one tool call, always returning a JSON string result. */
  private static String runTool(String name, String argumentsJson) {
    Map<String, Object> args;
    try {
      args =
          MAPPER.readValue(
              argumentsJson == null || argumentsJson.isEmpty() ? "{}" : argumentsJson,
              new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      return "{\"error\":\"invalid JSON arguments\"}";
    }
    Function<Map<String, Object>, Object> impl = TOOL_IMPL.get(name);
    if (impl == null) {
      return toJson(Collections.singletonMap("error", "unknown tool: " + name));
    }
    try {
      return toJson(Collections.singletonMap("result", impl.apply(args)));
    } catch (RuntimeException e) {
      return toJson(Collections.singletonMap("error", "bad arguments: " + e.getMessage()));
    }
  }

  private static String toJson(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("failed to serialize tool result", e);
    }
  }

  /** Drive the tool-call state machine until a final answer. */
  static String agentLoop(DashScopeClient client, String userInput) {
    List<Message> messages = new ArrayList<>();
    messages.add(Message.system("你是一个助手，可以调用工具获取实时信息。"));
    messages.add(Message.user(userInput));
    for (int turn = 0; turn < MAX_TURNS; turn++) {
      ChatCompletion resp =
          client
              .chat()
              .completions()
              .create(
                  ChatCompletionCreateParams.builder()
                      .model(MODEL)
                      .messages(messages)
                      .tools(TOOLS)
                      .build());
      ChatCompletionMessage message = resp.getChoices().get(0).getMessage();
      if (message.getToolCalls() == null || message.getToolCalls().isEmpty()) {
        System.out.println("[turn " + turn + "] final: " + message.getContent());
        return message.getContent();
      }

      // Echo the assistant tool-call turn back verbatim ...
      messages.add(Message.assistant(message.getContent(), message.getToolCalls()));
      // ... then run each tool and append its result.
      for (ToolCall call : message.getToolCalls()) {
        String name = call.getFunction().getName();
        String arguments = call.getFunction().getArguments();
        String result = runTool(name, arguments);
        System.out.println("[turn " + turn + "] " + name + "(" + arguments + ") -> " + result);
        messages.add(Message.tool(result, name));
      }
    }
    System.out.println("reached MAX_TURNS=" + MAX_TURNS + " without a final answer");
    return null;
  }

  /** Run the agent loop against the real chat endpoint. */
  public static void main(String[] args) {
    try (DashScopeClient client = ExampleSupport.client()) {
      agentLoop(client, "杭州现在天气怎么样？顺便说下它有多少人口。");
    }
  }
}
