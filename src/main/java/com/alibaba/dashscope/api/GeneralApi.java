// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.api;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ClientProviders;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.HalfDuplexClient;
import com.alibaba.dashscope.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.protocol.ServiceOption;
import io.reactivex.Flowable;

/** Support DashScope async task CRUD. */
public final class GeneralApi<ParamT extends HalfDuplexParamBase> {
  final HalfDuplexClient client;
  ConnectionOptions connectionOptions;

  /** Create default http client. */
  public GeneralApi() {
    this.client = ClientProviders.getHalfDuplexClient("https");
    this.connectionOptions = null;
  }

  /**
   * Create custom http client
   *
   * @param connectionOptions The client option.
   */
  public GeneralApi(ConnectionOptions connectionOptions) {
    this.client = ClientProviders.getHalfDuplexClient(connectionOptions, "https");
    this.connectionOptions = connectionOptions;
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @param serviceOptions The service option.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @return The output structure, should be the subclass of `Result`.
   */
  public DashScopeResult call(ParamT param, ServiceOption serviceOptions)
      throws ApiException, NoApiKeyException {
    HalfDuplexRequest req = new HalfDuplexRequest(param, serviceOptions);
    return client.send(req);
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
   * Call the server to get the result by stream.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @param serviceOptions The service options
   * @return A `Flowable` of the output structure, which is the subclass of `Result`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public Flowable<DashScopeResult> streamCall(ParamT param, ServiceOption serviceOptions)
      throws ApiException, NoApiKeyException {
    HalfDuplexRequest req = new HalfDuplexRequest(param, serviceOptions);
    return client.streamOut(req);
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @param serviceOptions The service options
   * @param callback The result callback.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void streamCall(
      ParamT param, ServiceOption serviceOptions, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException {
    HalfDuplexRequest req = new HalfDuplexRequest(param, serviceOptions);
    client.streamOut(req, callback);
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

  /**
   * Make a Delete request.
   *
   * @param param The param.
   * @param serviceOption The service options
   * @return The task result or status information.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public DashScopeResult delete(HalfDuplexParamBase param, ServiceOption serviceOption)
      throws ApiException, NoApiKeyException {
    DashScopeResult result = client.send(new HalfDuplexRequest(param, serviceOption));
    return result;
  }
}
