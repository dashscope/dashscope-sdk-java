// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AsyncFuturesTest {

  private static final Executor SATURATED =
      task -> {
        throw new RejectedExecutionException("saturated");
      };

  @Test
  void executorSaturationFailsTheFutureInsteadOfThrowing() throws Exception {
    CompletableFuture<String> future = AsyncFutures.supplyAsync(() -> "never runs", SATURATED);

    // The async API contract: errors arrive through the future, never as a synchronous
    // RejectedExecutionException out of create(...).
    ExecutionException e =
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    assertTrue(e.getCause() instanceof RejectedExecutionException);
  }

  @Test
  void supplierFailureCompletesExceptionally() throws Exception {
    IllegalStateException failure = new IllegalStateException("boom");
    CompletableFuture<String> future =
        AsyncFutures.supplyAsync(
            () -> {
              throw failure;
            },
            Runnable::run);

    ExecutionException e =
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    assertEquals(failure, e.getCause());
  }

  @Test
  void successfulSupplyCompletesWithResult() throws Exception {
    CompletableFuture<String> future = AsyncFutures.supplyAsync(() -> "ok", Runnable::run);
    assertEquals("ok", future.get(5, TimeUnit.SECONDS));
  }
}
