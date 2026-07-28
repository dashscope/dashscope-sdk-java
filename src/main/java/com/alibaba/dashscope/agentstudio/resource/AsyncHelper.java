// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.agentstudio.AgentStudioException;
import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.ServiceOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

final class AsyncHelper {

  private AsyncHelper() {}

  static CompletableFuture<DashScopeResult> callAsync(
      GeneralApi<HalfDuplexParamBase> api, HalfDuplexParamBase param, ServiceOption opt) {
    CompletableFuture<DashScopeResult> future = new CompletableFuture<>();
    try {
      api.call(
          param,
          opt,
          new ResultCallback<DashScopeResult>() {
            @Override
            public void onEvent(DashScopeResult result) {
              future.complete(result);
            }

            @Override
            public void onComplete() {}

            @Override
            public void onError(Exception e) {
              future.completeExceptionally(normalize(e));
            }
          });
    } catch (Exception e) {
      future.completeExceptionally(normalize(e));
    }
    return future;
  }

  /** Wrap {@link ApiException}s as the unified {@link AgentStudioException}. */
  private static Throwable normalize(Throwable e) {
    return e instanceof ApiException ? AgentStudioException.wrap((ApiException) e) : e;
  }

  static <T> CompletableFuture<T> failedFuture(Throwable ex) {
    CompletableFuture<T> f = new CompletableFuture<>();
    f.completeExceptionally(ex);
    return f;
  }

  static <T> T joinAndUnwrap(CompletableFuture<T> future) {
    try {
      return future.join();
    } catch (CompletionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new ApiException(cause);
    }
  }
}
