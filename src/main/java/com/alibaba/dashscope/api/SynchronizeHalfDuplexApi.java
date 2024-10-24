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

/** Dashscope synchronize half duplex request processing, both http and websocket support. */
public final class SynchronizeHalfDuplexApi<ParamT extends HalfDuplexParamBase> {
  final HalfDuplexClient client;
  ConnectionOptions connectionOptions;
  final ServiceOption serviceOptions;
  /**
   * Create default client, http or websocket.
   *
   * @param serviceOptions The service option.
   */
  public SynchronizeHalfDuplexApi(ServiceOption serviceOptions) {
    this.client = ClientProviders.getHalfDuplexClient(serviceOptions.getProtocol().getValue());
    this.connectionOptions = null;
    this.serviceOptions = serviceOptions;
  }

  /**
   * Create custom client
   *
   * @param connectionOptions The client option.
   * @param serviceOptions The api service option.
   */
  public SynchronizeHalfDuplexApi(
      ConnectionOptions connectionOptions, ServiceOption serviceOptions) {
    this.client =
        ClientProviders.getHalfDuplexClient(
            connectionOptions, serviceOptions.getProtocol().getValue());
    this.connectionOptions = connectionOptions;
    this.serviceOptions = serviceOptions;
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param, should be the subclass of `ConversationParam`.
   * @return The output structure, should be the subclass of `ConversationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public DashScopeResult call(ParamT param) throws ApiException, NoApiKeyException {
    HalfDuplexRequest req = new HalfDuplexRequest(param, serviceOptions);
    return client.send(req);
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @param callback The callback to receive response, should be the subclass of `Result`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void call(ParamT param, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException {
    HalfDuplexRequest req = new HalfDuplexRequest(param, serviceOptions);
    client.send(req, callback);
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @return A `Flowable` of the output structure, which is the subclass of `Result`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public Flowable<DashScopeResult> streamCall(ParamT param) throws ApiException, NoApiKeyException {
    HalfDuplexRequest req = new HalfDuplexRequest(param, serviceOptions);
    return client.streamOut(req);
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @param callback The result callback.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void streamCall(ParamT param, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException {
    HalfDuplexRequest req = new HalfDuplexRequest(param, serviceOptions);
    client.streamOut(req, callback);
  }

  public boolean close(int code, String reason) {
    if (client != null) {
      return client.close(code, reason);
    } else {
      return true;
    }
  }
}
