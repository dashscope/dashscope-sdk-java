// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.protocol;

import com.alibaba.dashscope.applications.agentstudio.common.DashScopeResult;
import com.alibaba.dashscope.applications.agentstudio.common.ResultCallback;
import com.alibaba.dashscope.applications.agentstudio.exception.ApiException;
import com.alibaba.dashscope.applications.agentstudio.exception.NoApiKeyException;

/** Half-duplex HTTP client used by the AgentStudio transport. */
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
}
