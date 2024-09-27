// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.api;

import com.alibaba.dashscope.base.FullDuplexServiceParam;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ClientProviders;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.FullDuplexClient;
import com.alibaba.dashscope.protocol.FullDuplexRequest;
import io.reactivex.Flowable;

/** DashScope synchronize full duplex request processing, only websocket support. */
public final class SynchronizeFullDuplexApi<ParamT extends FullDuplexServiceParam> {
  final FullDuplexClient client;
  ConnectionOptions connectionOptions;
  final ApiServiceOption serviceOptions;
  /**
   * Create default http client.
   *
   * @param serviceOptions The service option.
   */
  public SynchronizeFullDuplexApi(ApiServiceOption serviceOptions) {
    this.client = ClientProviders.getFullDuplexClient(null);
    this.connectionOptions = null;
    this.serviceOptions = serviceOptions;
  }

  public boolean close(int code, String reason) {
    if (client != null) {
      return client.close(code, reason);
    } else {
      return true;
    }
  }

  public void cancel() {
    if (client != null) {
      client.cancel();
    }
  }

  /**
   * Create custom http client
   *
   * @param connectionOptions The client option.
   * @param serviceOptions The service option.
   */
  public SynchronizeFullDuplexApi(
      ConnectionOptions connectionOptions, ApiServiceOption serviceOptions) {
    this.client = ClientProviders.getFullDuplexClient(connectionOptions);
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
  public DashScopeResult streamIn(ParamT param) throws ApiException, NoApiKeyException {
    FullDuplexRequest req = new FullDuplexRequest(param, serviceOptions);
    return client.streamIn(req);
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @param callback The callback to receive response, should be the subclass of `Result`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void streamIn(ParamT param, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException {
    FullDuplexRequest req = new FullDuplexRequest(param, serviceOptions);
    client.streamIn(req, callback);
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @return A `Flowable` of the output structure, which is the subclass of `Result`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public Flowable<DashScopeResult> duplexCall(ParamT param) throws ApiException, NoApiKeyException {
    FullDuplexRequest req = new FullDuplexRequest(param, serviceOptions);
    return client.duplex(req);
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @param callback The result callback.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void duplexCall(ParamT param, ResultCallback<DashScopeResult> callback)
      throws ApiException, NoApiKeyException {
    FullDuplexRequest req = new FullDuplexRequest(param, serviceOptions);
    client.duplex(req, callback);
  }
}
