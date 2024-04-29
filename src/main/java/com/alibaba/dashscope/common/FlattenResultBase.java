// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import com.alibaba.dashscope.threads.AssistantStreamEvents;
import com.alibaba.dashscope.threads.AssistantThread;
import com.alibaba.dashscope.threads.messages.ThreadMessage;
import com.alibaba.dashscope.threads.messages.ThreadMessageDelta;
import com.alibaba.dashscope.threads.runs.AssistantStreamMessage;
import com.alibaba.dashscope.threads.runs.Run;
import com.alibaba.dashscope.threads.runs.RunStep;
import com.alibaba.dashscope.threads.runs.RunStepDelta;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import lombok.Data;

@Data
public abstract class FlattenResultBase {
  /** The request if. */
  @SerializedName("request_id")
  private String requestId;

  protected FlattenResultBase() {}

  public static <T extends FlattenResultBase> T fromDashScopeResult(
      DashScopeResult dashScopeResult, Type type) {
    T obj = JsonUtils.fromJson((JsonObject) dashScopeResult.getOutput(), type);
    return obj;
  }

  public static AssistantStreamMessage fromDashScopeResult(
      DashScopeResult dashScopeResult, Type type, boolean stream) {
    String event = dashScopeResult.getEvent();
    AssistantStreamMessage asm = new AssistantStreamMessage();
    asm.setEvent(AssistantStreamEvents.fromValue(event));
    if (stream) {
      switch (AssistantStreamEvents.fromValue(dashScopeResult.getEvent())) {
        case THREAD_CREATED:
          AssistantThread thread =
              FlattenResultBase.fromDashScopeResult(dashScopeResult, AssistantThread.class);
          asm.setData(thread);
          return asm;
        case THREAD_RUN_CREATED:
        case THREAD_RUN_QUEUED:
        case THREAD_RUN_IN_PROGRESS:
        case THREAD_RUN_REQUIRES_ACTION:
        case THREAD_RUN_COMPLETED:
        case THREAD_RUN_FAILED:
        case THREAD_RUN_CANCELLING:
        case THREAD_RUN_CANCELED:
        case THREAD_RUN_EXPIRED:
          Run run = FlattenResultBase.fromDashScopeResult(dashScopeResult, Run.class);
          asm.setData(run);
          return asm;
        case THREAD_RUN_STEP_CREATED:
        case THREAD_RUN_STEP_IN_PROGRESS:
        case THREAD_RUN_STEP_COMPLETED:
        case THREAD_RUN_STEP_FAILED:
        case THREAD_RUN_STEP_CANCELLED:
        case THREAD_RUN_STEP_EXPIRED:
          RunStep runStep = FlattenResultBase.fromDashScopeResult(dashScopeResult, RunStep.class);
          asm.setData(runStep);
          return asm;
        case THREAD_RUN_STEP_DELTA:
          asm.setData(FlattenResultBase.fromDashScopeResult(dashScopeResult, RunStepDelta.class));
          return asm;
        case THREAD_MESSAGE_CREATED:
        case THREAD_MESSAGE_COMPLETED:
        case THREAD_MESSAGE_INCOMPLETE:
          ThreadMessage threadMessage =
              FlattenResultBase.fromDashScopeResult(dashScopeResult, ThreadMessage.class);
          asm.setData(threadMessage);
          return asm;
        case THREAD_MESSAGE_DELTA:
          asm.setData(
              ThreadMessageDelta.fromDashScopeResult(dashScopeResult, ThreadMessageDelta.class));
          return asm;
        default:
          asm.setData(JsonUtils.toJson(dashScopeResult.getOutput()));
          return asm;
      }
    } else {
      return fromDashScopeResult(dashScopeResult, type);
    }
  }
}
