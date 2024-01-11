// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.utils.Constants;

/** Internal used for config the service. */
public interface ServiceOption {
  public String getTaskGroup();

  public String getTask();

  public String getFunction();

  public StreamingMode getStreamingMode();

  public OutputMode getOutputMode();

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
}
