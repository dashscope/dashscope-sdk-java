package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.common.TypeRegistry;
import lombok.Data;

@Data
public class StepDetailBase {
  private static final TypeRegistry<StepDetailBase> stepDetailRegistry = new TypeRegistry<>();

  protected static synchronized void registerStepDetail(
      String type, Class<? extends StepDetailBase> clazz) {
    stepDetailRegistry.register(type, clazz);
  }

  public static synchronized Class<? extends StepDetailBase> getStepDetailClass(String type) {
    return stepDetailRegistry.get(type);
  }

  static {
    registerStepDetail("message_creation", StepMessageCreation.class);
    registerStepDetail("tool_calls", StepToolCalls.class);
  }

  private String type;
}
