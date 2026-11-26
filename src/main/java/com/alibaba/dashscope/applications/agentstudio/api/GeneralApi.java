// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.api;

import com.alibaba.dashscope.applications.agentstudio.base.HalfDuplexParamBase;
import com.alibaba.dashscope.applications.agentstudio.common.DashScopeResult;
import com.alibaba.dashscope.applications.agentstudio.common.ResultCallback;
import com.alibaba.dashscope.applications.agentstudio.exception.ApiException;
import com.alibaba.dashscope.applications.agentstudio.exception.NoApiKeyException;
import com.alibaba.dashscope.applications.agentstudio.protocol.ConnectionOptions;
import com.alibaba.dashscope.applications.agentstudio.protocol.HalfDuplexClient;
import com.alibaba.dashscope.applications.agentstudio.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.applications.agentstudio.protocol.ServiceOption;
import com.alibaba.dashscope.applications.agentstudio.protocol.okhttp.OkHttpClientFactory;
import com.alibaba.dashscope.applications.agentstudio.protocol.okhttp.OkHttpHttpClient;

/** AgentStudio-owned async HTTP transport for CRUD-style calls. */
public final class GeneralApi<ParamT extends HalfDuplexParamBase> {
  final HalfDuplexClient client;
  ConnectionOptions connectionOptions;

  /** Create default http client. */
  public GeneralApi() {
    this.client = new OkHttpHttpClient(OkHttpClientFactory.getOkHttpClient());
    this.connectionOptions = null;
  }

  /**
   * Create custom http client
   *
   * @param connectionOptions The client option.
   */
  public GeneralApi(ConnectionOptions connectionOptions) {
    this.client = new OkHttpHttpClient(OkHttpClientFactory.getNewOkHttpClient(connectionOptions));
    this.connectionOptions = connectionOptions;
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @param serviceOptions The service options
   * @param callback The callback to receive response, should be the subclass of `Result`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void call(
      ParamT param, ServiceOption serviceOptions, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException {
    HalfDuplexRequest req = new HalfDuplexRequest(param, serviceOptions);
    client.send(req, callback);
  }

  /**
   * Make a get request.
   *
   * @param param The param
   * @param serviceOption The service options
   * @return The task result.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public DashScopeResult get(HalfDuplexParamBase param, ServiceOption serviceOption)
      throws ApiException, NoApiKeyException {
    DashScopeResult result = client.send(new HalfDuplexRequest(param, serviceOption));
    return result;
  }
}
