package com.alibaba.dashscope.common;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TypeRegistry<T> {
  private final Map<String, Class<? extends T>> registry = new HashMap<>();

  public synchronized void register(String type, Class<? extends T> clazz) {
    if (!registry.containsKey(type)) {
      registry.put(type, clazz);
    }
  }

  public synchronized Class<? extends T> get(String type) {
    Class<? extends T> t = registry.get(type);
    if (t == null) {
      StringBuilder builder = new StringBuilder();
      builder.append("There is no class definition corresponding to the ").append(type).append(";");
      builder.append("ensure that the corresponding class is registered before the list.");
      builder.append("You can register with ToolBase.registerTool(toolType, TheTool.class)");
      log.warn(builder.toString());
      return null;
    }
    return t;
  }
}
