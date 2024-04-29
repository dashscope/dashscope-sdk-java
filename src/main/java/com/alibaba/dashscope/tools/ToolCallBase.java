package com.alibaba.dashscope.tools;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/** base class of the tool call response. */
@Data
public abstract class ToolCallBase {
  private static final Map<String, Class<?>> toolCallRegistry = new HashMap<>();

  protected static synchronized void registerToolCall(
      String toolType, Class<? extends ToolCallBase> clazz) {
    toolCallRegistry.put(toolType, clazz);
  }

  public static synchronized Class<?> getToolCallClass(String toolType) {
    return toolCallRegistry.get(toolType);
  }

  public static Map<String, Class<?>> getRegisteredTools() {
    return toolCallRegistry;
  }

  static {
    registerToolCall("function", ToolCallFunction.class);
  }

  public abstract String getType();

  public abstract String getId();
}
