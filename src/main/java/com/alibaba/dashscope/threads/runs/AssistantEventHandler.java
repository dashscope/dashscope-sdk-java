package com.alibaba.dashscope.threads.runs;

import com.alibaba.dashscope.threads.AssistantThread;
import com.alibaba.dashscope.threads.messages.ThreadMessage;

public interface AssistantEventHandler {
  public void onThreadCreated(AssistantThread thread);

  public void onThreadRunCreated(Run run);

  public void onThreadRunQueued(Run run);

  public void onThreadRunInProgress(Run run);

  public void onThreadRunRequiresAction(Run run);

  public void onThreadRunCompleted(Run run);

  public void onThreadRunFailed(Run run);

  public void onThreadRunCancelling(Run run);

  public void onThreadRunCancelled(Run run);

  public void onThreadRunExpired(Run run);

  public void OnThreadRunStepCreated(RunStep runStep);

  public void OnThreadRunStepInProgress(RunStep runStep);

  public void OnThreadRunStepDelta(RunStep runStep);

  public void OnThreadRunStepCompleted(RunStep runStep);

  public void OnThreadRunStepFailed(RunStep runStep);

  public void OnThreadRunStepCancelled(RunStep runStep);

  public void OnThreadRunStepExpired(RunStep runStep);

  public void onThreadMessageCreated(ThreadMessage threadMessage);

  public void onThreadMessageInProgress(ThreadMessage threadMessage);

  public void onThreadMessageDelta(ThreadMessage threadMessage);

  public void onThreadMessageCompleted(ThreadMessage threadMessage);

  public void onThreadMessageIncomplete(ThreadMessage threadMessage);

  public void onError(String errorMsg);

  public void onUnknown(String msg);

  public void onDone();
}
