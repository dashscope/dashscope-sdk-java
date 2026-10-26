// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.protocol;

import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.common.ResultCallback;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import io.reactivex.Flowable;

public interface FullDuplexClient {
  /*
   * Multiple inputs, only for websocket.
   */
  DashScopeResult streamIn(FullDuplexRequest req) throws NoApiKeyException, ApiException;

  void streamIn(FullDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException;

  /* Send and receive are duplex, only support websocket.
   *
   */
  Flowable<DashScopeResult> duplex(FullDuplexRequest req) throws NoApiKeyException, ApiException;

  void duplex(FullDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException;

  boolean close(int code, String reason);

  void cancel();
}
