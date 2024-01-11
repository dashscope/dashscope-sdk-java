// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.common.OutputMode;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class AsyncTaskOption implements ServiceOption {
  @Default private final Protocol protocol = Protocol.HTTP;
  @Default private final HttpMethod httpMethod = HttpMethod.POST;

  /** The request base url */
  @Default private String baseHttpUrl = null;

  @Default private String baseWebSocketUrl = null;

  private String url;

  @Override
  public String httpUrl() {
    return url;
  }

  @Override
  public Protocol getProtocol() {
    return protocol;
  }

  @Override
  public HttpMethod getHttpMethod() {
    return httpMethod;
  }

  @Override
  public String getTaskGroup() {
    throw new UnsupportedOperationException("Unimplemented method 'getTaskGroup'");
  }

  @Override
  public String getTask() {
    throw new UnsupportedOperationException("Unimplemented method 'getTask'");
  }

  @Override
  public String getFunction() {
    throw new UnsupportedOperationException("Unimplemented method 'getFunction'");
  }

  @Override
  public StreamingMode getStreamingMode() {
    return StreamingMode.NONE;
  }

  @Override
  public OutputMode getOutputMode() {
    throw new UnsupportedOperationException("Unimplemented method 'getOutputMode'");
  }
}
