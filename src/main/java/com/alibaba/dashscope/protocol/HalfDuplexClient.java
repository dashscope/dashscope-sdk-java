// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;

public interface HalfDuplexClient {
  /**
   * Blocking send http request and get the response body.
   *
   * @param req the user input data and parameters.
   * @return The the `DashScopeResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  DashScopeResult send(HalfDuplexRequest req) throws NoApiKeyException, ApiException;
  /*
   * send with call back
   */
  void send(HalfDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException;

  /*
   * Multiple outputs, for http sse and websocket streamOut mode.
   */
  Flowable<DashScopeResult> streamOut(HalfDuplexRequest req) throws NoApiKeyException, ApiException;

  void streamOut(HalfDuplexRequest req, ResultCallback<DashScopeResult> callback)
      throws NoApiKeyException, ApiException;

  boolean close(int code, String reason);
}
