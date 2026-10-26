// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.resource;

import com.alibaba.dashscope.legacy_2x.api.GeneralApi;
import com.alibaba.dashscope.legacy_2x.base.HalfDuplexParamBase;
import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.common.ResultCallback;
import com.alibaba.dashscope.legacy_2x.protocol.ServiceOption;
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
              future.completeExceptionally(AgentStudioErrors.translate(e));
            }
          });
    } catch (Exception e) {
      future.completeExceptionally(AgentStudioErrors.translate(e));
    }
    return future;
  }

  static <T> CompletableFuture<T> failedFuture(Throwable ex) {
    CompletableFuture<T> f = new CompletableFuture<>();
    f.completeExceptionally(AgentStudioErrors.translate(ex));
    return f;
  }

  static <T> T joinAndUnwrap(CompletableFuture<T> future) {
    try {
      return future.join();
    } catch (CompletionException e) {
      throw AgentStudioErrors.translate(e.getCause());
    }
  }
}
