// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * Bridges synchronous service methods onto an {@link Executor} for the async resource tree.
 *
 * <p>Unlike a bare {@code CompletableFuture.supplyAsync(supplier, executor)}, executor saturation
 * (the bounded default pool rejecting the task) fails the returned future instead of throwing
 * {@link RejectedExecutionException} synchronously on the caller thread, keeping the async API's
 * "errors arrive through the future" contract.
 */
public final class AsyncFutures {
  private AsyncFutures() {}

  /**
   * Runs {@code supplier} on {@code executor}, mirroring {@code
   * CompletableFuture.supplyAsync(supplier, executor)} except that executor rejection fails the
   * returned future instead of throwing synchronously.
   */
  public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor) {
    CompletableFuture<T> future = new CompletableFuture<>();
    try {
      executor.execute(
          () -> {
            if (future.isDone()) {
              return;
            }
            try {
              future.complete(supplier.get());
            } catch (Throwable t) {
              future.completeExceptionally(t);
            }
          });
    } catch (RejectedExecutionException e) {
      future.completeExceptionally(e);
    }
    return future;
  }
}
