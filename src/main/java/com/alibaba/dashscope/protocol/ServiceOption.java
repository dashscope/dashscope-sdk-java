// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.utils.Constants;

/** Internal used for config the service. */
public interface ServiceOption {
  public default String getTaskGroup() {
    return null;
  };

  public default String getTask() {
    return null;
  };

  public default String getFunction() {
    return null;
  };

  public StreamingMode getStreamingMode();

  public default OutputMode getOutputMode() {
    return null;
  };

  public Protocol getProtocol();

  public HttpMethod getHttpMethod();

  public String httpUrl();

  public String getBaseHttpUrl();

  public String getBaseWebSocketUrl();

  public default Boolean getIsSSE() {
    return false;
  }

  public default Boolean getIsAsyncTask() {
    return false;
  }

  public default String webSocketUrl() {
    return Constants.baseWebsocketApiUrl;
  }

  /**
   * Is flatten request and response
   *
   * @return if the result is flatten
   */
  public default boolean getIsFlatten() {
    return false;
  }
}
