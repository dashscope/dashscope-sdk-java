package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.threads.AssistantStreamEvents;
import com.alibaba.dashscope.threads.AssistantThread;
import com.alibaba.dashscope.threads.messages.ThreadMessage;

final class StreamEventProcessingCallback extends ResultCallback<DashScopeResult> {
  private AssistantEventHandler handler;

  public StreamEventProcessingCallback(AssistantEventHandler handler) {
    this.handler = handler;
  }

  @Override
  public void onError(Exception ex) {
    handler.onError(ex.getMessage());
  }

  @Override
  public void onComplete() {
    handler.onDone();
  }

  @Override
  public void onEvent(DashScopeResult message) {
    switch (AssistantStreamEvents.fromValue(message.getEvent())) {
      case THREAD_CREATED:
        AssistantThread thread =
            FlattenResultBase.fromDashScopeResult(message, AssistantThread.class);
        handler.onThreadCreated(thread);
        break;
      case THREAD_RUN_CREATED:
        Run run = FlattenResultBase.fromDashScopeResult(message, Run.class);
        handler.onThreadRunCreated(run);
        break;
      case THREAD_RUN_QUEUED:
        run = FlattenResultBase.fromDashScopeResult(message, Run.class);
        handler.onThreadRunQueued(run);
        break;
      case THREAD_RUN_IN_PROGRESS:
        run = FlattenResultBase.fromDashScopeResult(message, Run.class);
        handler.onThreadRunInProgress(run);
        break;
      case THREAD_RUN_REQUIRES_ACTION:
        run = FlattenResultBase.fromDashScopeResult(message, Run.class);
        handler.onThreadRunRequiresAction(run);
        break;
      case THREAD_RUN_COMPLETED:
        run = FlattenResultBase.fromDashScopeResult(message, Run.class);
        handler.onThreadRunCompleted(run);
        break;
      case THREAD_RUN_FAILED:
        run = FlattenResultBase.fromDashScopeResult(message, Run.class);
        handler.onThreadRunFailed(run);
        break;
      case THREAD_RUN_CANCELLING:
        run = FlattenResultBase.fromDashScopeResult(message, Run.class);
        handler.onThreadRunCancelling(run);
        break;
      case THREAD_RUN_CANCELED:
        run = FlattenResultBase.fromDashScopeResult(message, Run.class);
        handler.onThreadRunCancelled(run);
        break;
      case THREAD_RUN_EXPIRED:
        run = FlattenResultBase.fromDashScopeResult(message, Run.class);
        handler.onThreadRunExpired(run);
        break;
      case THREAD_RUN_STEP_CREATED:
        RunStep runStep = FlattenResultBase.fromDashScopeResult(message, RunStep.class);
        handler.OnThreadRunStepCreated(runStep);
        break;
      case THREAD_RUN_STEP_IN_PROGRESS:
        runStep = FlattenResultBase.fromDashScopeResult(message, RunStep.class);
        handler.OnThreadRunStepInProgress(runStep);
        break;
      case THREAD_RUN_STEP_DELTA:
        runStep = FlattenResultBase.fromDashScopeResult(message, RunStep.class);
        handler.OnThreadRunStepDelta(runStep);
        break;
      case THREAD_RUN_STEP_COMPLETED:
        runStep = FlattenResultBase.fromDashScopeResult(message, RunStep.class);
        handler.OnThreadRunStepCompleted(runStep);
        break;
      case THREAD_RUN_STEP_FAILED:
        runStep = FlattenResultBase.fromDashScopeResult(message, RunStep.class);
        handler.OnThreadRunStepFailed(runStep);
        break;
      case THREAD_RUN_STEP_CANCELLED:
        runStep = FlattenResultBase.fromDashScopeResult(message, RunStep.class);
        handler.OnThreadRunStepCancelled(runStep);
        break;
      case THREAD_RUN_STEP_EXPIRED:
        runStep = FlattenResultBase.fromDashScopeResult(message, RunStep.class);
        handler.OnThreadRunStepExpired(runStep);
        break;
      case THREAD_MESSAGE_CREATED:
        ThreadMessage threadMessage =
            FlattenResultBase.fromDashScopeResult(message, ThreadMessage.class);
        handler.onThreadMessageCreated(threadMessage);
        break;
      case THREAD_MESSAGE_DELTA:
        threadMessage = FlattenResultBase.fromDashScopeResult(message, ThreadMessage.class);
        handler.onThreadMessageDelta(threadMessage);
        break;
      case THREAD_MESSAGE_COMPLETED:
        threadMessage = FlattenResultBase.fromDashScopeResult(message, ThreadMessage.class);
        handler.onThreadMessageCompleted(threadMessage);
        break;
      case THREAD_MESSAGE_IN_PROGRESS:
        threadMessage = FlattenResultBase.fromDashScopeResult(message, ThreadMessage.class);
        handler.onThreadMessageCompleted(threadMessage);
        break;
      case THREAD_MESSAGE_INCOMPLETE:
        threadMessage = FlattenResultBase.fromDashScopeResult(message, ThreadMessage.class);
        handler.onThreadMessageIncomplete(threadMessage);
        break;
      case ERROR:
        String errorMsg =
            String.format("Event: %s, data: %s", message.getEvent(), message.getOutput());
        handler.onError(errorMsg);
        break;
      default:
        String msg =
            String.format("Unknown event: %s, data: %s", message.getEvent(), message.getOutput());
        handler.onUnknown(msg);
        break;
    }
  }
}
