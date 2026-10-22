// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.protocol;

/**
 * 3.0 copy of the 2.x ServiceOption for the agentstudio subtree (websocket/output-mode members of
 * the 2.x original are unused here and omitted).
 */
public interface ServiceOption {

  public StreamingMode getStreamingMode();

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

  /**
   * Is flatten request and response
   *
   * @return if the result is flatten
   */
  public default boolean getIsFlatten() {
    return false;
  }

  /**
   * SDK module name for tracking headers (e.g. "agentstudio").
   *
   * @return module identifier, or null if unknown
   */
  public default String getModule() {
    return null;
  }
}
