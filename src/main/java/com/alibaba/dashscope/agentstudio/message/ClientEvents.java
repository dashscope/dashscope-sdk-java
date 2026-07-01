// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;

public final class ClientEvents {

  private ClientEvents() {}

  public static JsonObject userMessage(String text) {
    return userMessage(text, null, null);
  }

  public static JsonObject userMessage(List<JsonObject> blocks) {
    return userMessage(blocks, null, null);
  }

  public static JsonObject userMessage(String text, String sessionThreadId) {
    return userMessage(text, sessionThreadId, null);
  }

  public static JsonObject userMessage(
      String text, String sessionThreadId, Map<String, String> metadata) {
    JsonArray content = new JsonArray();
    JsonObject textBlock = new JsonObject();
    textBlock.addProperty("type", "text");
    textBlock.addProperty("text", text);
    content.add(textBlock);
    return buildUserMessage(content, sessionThreadId, metadata);
  }

  public static JsonObject userMessage(
      List<JsonObject> blocks, String sessionThreadId, Map<String, String> metadata) {
    JsonArray content = new JsonArray();
    for (JsonObject block : blocks) {
      content.add(block);
    }
    return buildUserMessage(content, sessionThreadId, metadata);
  }

  private static JsonObject buildUserMessage(
      JsonArray content, String sessionThreadId, Map<String, String> metadata) {
    JsonObject event = new JsonObject();
    event.addProperty("type", "message");
    event.addProperty("role", "user");
    event.add("content", content);
    if (sessionThreadId != null) {
      event.addProperty("session_thread_id", sessionThreadId);
    }
    if (metadata != null && !metadata.isEmpty()) {
      JsonObject meta = new JsonObject();
      for (Map.Entry<String, String> e : metadata.entrySet()) {
        meta.addProperty(e.getKey(), e.getValue());
      }
      event.add("metadata", meta);
    }
    return event;
  }

  public static JsonObject userInterrupt() {
    return userInterrupt(null);
  }

  public static JsonObject userInterrupt(String sessionThreadId) {
    JsonObject event = new JsonObject();
    event.addProperty("type", "interrupt");
    event.addProperty("role", "user");
    if (sessionThreadId != null) {
      event.addProperty("session_thread_id", sessionThreadId);
    }
    return event;
  }

  public static JsonObject userToolConfirmation(String toolUseId, String result) {
    return userToolConfirmation(toolUseId, result, null, null);
  }

  public static JsonObject userToolConfirmation(
      String toolUseId, String result, String denyMessage) {
    return userToolConfirmation(toolUseId, result, denyMessage, null);
  }

  public static JsonObject userToolConfirmation(
      String toolUseId, String result, String denyMessage, String sessionThreadId) {
    if (!"allow".equals(result) && !"deny".equals(result)) {
      throw new IllegalArgumentException("tool_confirmation result must be 'allow' or 'deny'");
    }
    JsonObject data = new JsonObject();
    data.addProperty("call_id", toolUseId);
    data.addProperty("result", result);
    if (denyMessage != null && "deny".equals(result)) {
      data.addProperty("deny_message", denyMessage);
    }
    JsonObject dataBlock = new JsonObject();
    dataBlock.addProperty("type", "data");
    dataBlock.add("data", data);
    JsonArray content = new JsonArray();
    content.add(dataBlock);
    JsonObject event = new JsonObject();
    event.addProperty("type", "tool_confirmation");
    event.addProperty("role", "user");
    event.add("content", content);
    if (sessionThreadId != null) {
      event.addProperty("session_thread_id", sessionThreadId);
    }
    return event;
  }

  public static JsonObject userCustomToolResult(
      String customToolUseId, String resultContent, boolean isError) {
    return userCustomToolResult(customToolUseId, resultContent, isError, null);
  }

  public static JsonObject userCustomToolResult(
      String customToolUseId, String resultContent, boolean isError, String sessionThreadId) {
    JsonObject data = new JsonObject();
    data.addProperty("call_id", customToolUseId);
    data.addProperty("output", resultContent);
    JsonObject dataBlock = new JsonObject();
    dataBlock.addProperty("type", "data");
    dataBlock.add("data", data);
    JsonArray content = new JsonArray();
    content.add(dataBlock);
    JsonObject event = new JsonObject();
    event.addProperty("type", "function_call_output");
    event.addProperty("role", "tool");
    event.add("content", content);
    event.addProperty("is_error", isError);
    if (sessionThreadId != null) {
      event.addProperty("session_thread_id", sessionThreadId);
    }
    return event;
  }

  public static JsonObject userToolResult(String toolUseId, String resultContent, boolean isError) {
    return userToolResult(toolUseId, resultContent, isError, null);
  }

  public static JsonObject userToolResult(
      String toolUseId, String resultContent, boolean isError, String sessionThreadId) {
    JsonObject data = new JsonObject();
    data.addProperty("call_id", toolUseId);
    data.addProperty("output", resultContent);
    JsonObject dataBlock = new JsonObject();
    dataBlock.addProperty("type", "data");
    dataBlock.add("data", data);
    JsonArray content = new JsonArray();
    content.add(dataBlock);
    JsonObject event = new JsonObject();
    event.addProperty("type", "tool_call_output");
    event.addProperty("role", "tool");
    event.add("content", content);
    event.addProperty("is_error", isError);
    if (sessionThreadId != null) {
      event.addProperty("session_thread_id", sessionThreadId);
    }
    return event;
  }

  public static JsonObject userDefineOutcome(
      String description, String rubric, Integer maxIterations) {
    return userDefineOutcome(description, rubric, maxIterations, null);
  }

  public static JsonObject userDefineOutcome(
      String description, String rubric, Integer maxIterations, String sessionThreadId) {
    JsonObject data = new JsonObject();
    if (description != null) {
      data.addProperty("description", description);
    }
    if (rubric != null) {
      data.addProperty("rubric", rubric);
    }
    if (maxIterations != null) {
      data.addProperty("max_iterations", maxIterations);
    }
    JsonObject dataBlock = new JsonObject();
    dataBlock.addProperty("type", "data");
    dataBlock.add("data", data);
    JsonArray content = new JsonArray();
    content.add(dataBlock);
    JsonObject event = new JsonObject();
    event.addProperty("type", "define_outcome");
    event.addProperty("role", "user");
    event.add("content", content);
    if (sessionThreadId != null) {
      event.addProperty("session_thread_id", sessionThreadId);
    }
    return event;
  }
}
