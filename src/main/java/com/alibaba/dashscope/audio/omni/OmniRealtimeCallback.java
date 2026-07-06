// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import com.google.gson.JsonObject;

/** @author lengjiayi */
public abstract class OmniRealtimeCallback {
  /** Will be called as soon as the connection is established, only for http stream request. */
  public void onOpen() {}

  /**
   * Will be called as soon as the server replies.
   *
   * @param message The message body in json.
   */
  public abstract void onEvent(JsonObject message);

  /** Will be called when the connection is closed. */
  public abstract void onClose(int code, String reason);
}
