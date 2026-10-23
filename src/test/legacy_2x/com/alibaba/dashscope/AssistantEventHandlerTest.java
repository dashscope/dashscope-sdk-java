package com.alibaba.dashscope;

import com.alibaba.dashscope.threads.AssistantThread;
import com.alibaba.dashscope.threads.messages.ThreadMessage;
import com.alibaba.dashscope.threads.runs.AssistantEventHandler;
import com.alibaba.dashscope.threads.runs.LastError;
import com.alibaba.dashscope.threads.runs.Run;
import com.alibaba.dashscope.threads.runs.RunStep;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import lombok.Data;

/**
 * completed event thread.run.completed|thread.run.cancelled|thread.run.expired|thread.run.failed on
 * thread.run.requires_action, we can make function call, get result and submit result.
 */
@Data
public class AssistantEventHandlerTest implements AssistantEventHandler {
  private AssistantThread assistantThread;
  private Run run;
  private RunStep finalRunStep;
  private ThreadMessage finalMessage;
  private LastError error;
  private boolean isCompleted = false;
  private FlowableEmitter<ThreadMessage> threadMessageEmitter;
  private Flowable<ThreadMessage> threadMessageFlowable;
  private FlowableEmitter<RunStep> runStepEmitter;
  private Flowable<RunStep> runStepFlowable;

  public AssistantEventHandlerTest() {
    threadMessageFlowable =
        Flowable.<ThreadMessage>create(
            emitter -> {
              threadMessageEmitter = emitter;
            },
            BackpressureStrategy.BUFFER);
    threadMessageFlowable.subscribe().dispose();
    runStepFlowable =
        Flowable.<RunStep>create(
            emitter -> {
              runStepEmitter = emitter;
            },
            BackpressureStrategy.BUFFER);
    runStepFlowable.subscribe().dispose();
  }

  public Flowable<ThreadMessage> getStreamMessage() {
    return threadMessageFlowable;
  }

  public Flowable<RunStep> getRunStepDelta() {
    return runStepFlowable;
  }

  public synchronized void await() throws InterruptedException {
    while (!isCompleted) {
      wait();
    }
  }

  public synchronized void signal() {
    isCompleted = true;
    notifyAll();
  }

  @Override
  public void onThreadCreated(AssistantThread thread) {
    assistantThread = thread;
  }

  @Override
  public void onThreadRunCreated(Run run) {
    this.run = run;
  }

  @Override
  public void onThreadRunQueued(Run run) {
    this.run = run;
  }

  @Override
  public void onThreadRunInProgress(Run run) {
    this.run = run;
  }

  @Override
  public void onThreadRunRequiresAction(Run run) {
    this.run = run;
    signal();
  }

  @Override
  public void onThreadRunCompleted(Run run) {
    this.run = run;
    signal();
  }

  @Override
  public void onThreadRunFailed(Run run) {
    this.run = run;
    signal();
  }

  @Override
  public void onThreadRunCancelling(Run run) {
    this.run = run;
  }

  @Override
  public void onThreadRunCancelled(Run run) {
    this.run = run;
    signal();
  }

  @Override
  public void onThreadRunExpired(Run run) {
    this.run = run;
    signal();
  }

  @Override
  public void OnThreadRunStepCreated(RunStep runStep) {
    this.finalRunStep = runStep;
  }

  @Override
  public void OnThreadRunStepInProgress(RunStep runStep) {
    this.finalRunStep = runStep;
  }

  @Override
  public void OnThreadRunStepDelta(RunStep runStep) {
    runStepEmitter.onNext(runStep);
  }

  @Override
  public void OnThreadRunStepCompleted(RunStep runStep) {
    this.finalRunStep = runStep;
    runStepEmitter.onComplete();
  }

  @Override
  public void OnThreadRunStepFailed(RunStep runStep) {
    this.finalRunStep = runStep;
    runStepEmitter.onNext(runStep);
    runStepEmitter.onComplete();
  }

  @Override
  public void OnThreadRunStepCancelled(RunStep runStep) {
    this.finalRunStep = runStep;
    runStepEmitter.onComplete();
  }

  @Override
  public void OnThreadRunStepExpired(RunStep runStep) {
    this.finalRunStep = runStep;
    runStepEmitter.onComplete();
  }

  @Override
  public void onThreadMessageCreated(ThreadMessage threadMessage) {
    this.finalMessage = threadMessage;
  }

  @Override
  public void onThreadMessageInProgress(ThreadMessage threadMessage) {
    this.finalMessage = threadMessage;
  }

  @Override
  public void onThreadMessageDelta(ThreadMessage threadMessage) {
    threadMessageEmitter.onNext(threadMessage);
  }

  @Override
  public void onThreadMessageCompleted(ThreadMessage threadMessage) {
    this.finalMessage = threadMessage;
    threadMessageEmitter.onNext(threadMessage);
    threadMessageEmitter.onComplete();
  }

  @Override
  public void onThreadMessageIncomplete(ThreadMessage threadMessage) {
    this.finalMessage = threadMessage;
    threadMessageEmitter.onNext(threadMessage);
    threadMessageEmitter.onComplete();
  }

  @Override
  public void onError(String errorMsg) {
    signal();
  }

  @Override
  public void onUnknown(String msg) {
    System.out.println(msg);
  }

  @Override
  public void onDone() {
    threadMessageEmitter.onComplete();
    runStepEmitter.onComplete();
    signal();
  }
}
