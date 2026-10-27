// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;

class AsyncExecutorTest {

  @Test
  void defaultExecutorIsBounded() {
    ExecutorService executor = DashScopeAsyncClient.newExecutor(3);
    try {
      ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
      assertEquals(3, pool.getMaximumPoolSize());
      assertEquals(12, pool.getQueue().remainingCapacity());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void clientOptionsRetainInjectedExecutor() {
    Executor executor = Runnable::run;
    ClientOptions options = ClientOptions.builder().executor(executor).build();
    assertSame(executor, options.getExecutor());
    assertSame(executor, options.toBuilder().build().getExecutor());
  }
}
