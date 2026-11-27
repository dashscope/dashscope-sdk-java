// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.protocol;

/** Internal used for config the service. */
public interface ServiceOption {

  public StreamingMode getStreamingMode();

  public Protocol getProtocol();

  public HttpMethod getHttpMethod();

  public String httpUrl();

  public String getBaseHttpUrl();

  public default Boolean getIsSSE() {
    return false;
  }

  public default Boolean getIsAsyncTask() {
    return false;
  }

  /**
   * Is flatten request and response
   *
   * @return if the result is flatten
   */
  public default boolean getIsFlatten() {
    return false;
  }

  /**
   * SDK module name for tracking headers (e.g. "agentstudio3").
   *
   * @return module identifier, or null if unknown
   */
  public default String getModule() {
    return null;
  }
}
