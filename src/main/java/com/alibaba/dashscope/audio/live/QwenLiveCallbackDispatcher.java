// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

final class QwenLiveCallbackDispatcher {
  private final Executor executor;
  private final int capacity;
  private final Consumer<Throwable> failSession;
  private final Consumer<Throwable> reportError;
  private final Object lock = new Object();
  private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
  private boolean draining;
  private boolean failed;

  QwenLiveCallbackDispatcher(
      Executor executor,
      int capacity,
      Consumer<Throwable> failSession,
      Consumer<Throwable> reportError) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("callbackQueueCapacity must be positive.");
    }
    this.executor = executor;
    this.capacity = capacity;
    this.failSession = failSession;
    this.reportError = reportError;
  }

  void dispatch(Runnable callback) {
    if (executor == null) {
      callback.run();
      return;
    }
    boolean schedule = false;
    RejectedExecutionException overflow = null;
    synchronized (lock) {
      if (failed) return;
      if (queue.size() >= capacity) {
        failed = true;
        queue.clear();
        overflow =
            new RejectedExecutionException("Qwen Live callback queue is full: " + capacity);
      } else {
        queue.addLast(callback);
        if (!draining) {
          draining = true;
          schedule = true;
        }
      }
    }
    if (overflow != null) {
      failSession.accept(overflow);
      final Throwable failure = overflow;
      synchronized (lock) {
        queue.addLast(() -> reportError.accept(failure));
        if (!draining) {
          draining = true;
          schedule = true;
        }
      }
    }
    if (schedule) scheduleDrain();
  }

  private void scheduleDrain() {
    try {
      executor.execute(this::drain);
    } catch (RuntimeException failure) {
      synchronized (lock) {
        failed = true;
        draining = false;
        queue.clear();
      }
      failSession.accept(failure);
    }
  }

  private void drain() {
    while (true) {
      Runnable callback;
      synchronized (lock) {
        callback = queue.pollFirst();
        if (callback == null) {
          draining = false;
          return;
        }
      }
      callback.run();
    }
  }
}
