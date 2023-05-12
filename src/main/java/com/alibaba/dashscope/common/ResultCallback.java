// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

public abstract class ResultCallback<T> {

  /**
   * Will be called as soon as the connection is established, only support the websocket protocol.
   *
   * @param status The connection status.
   */
  public void onOpen(Status status) {}

  /**
   * Will be called as soon as the server replies
   *
   * @param eventType The message type. Please check `EventType` for details.
   * @param message The message body in a structured class.
   */
  public abstract void onEvent(String eventType, T message);

  /**
   * Will be called as soon as the server closed, only support the websocket protocol.
   *
   * @param status The connection status.
   */
  public void onClose(Status status) {}

  /**
   * Will be called when an Exception occurs.
   *
   * @param e The exception instance.
   */
  public void onError(Exception e) {}

  /**
   * Will be called when the server is on closing, only support the websocket protocol.
   *
   * @param status The connection status.
   */
  public void doClose(Status status) {}
}
