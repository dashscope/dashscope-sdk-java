// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class QwenLiveCallback {
  public void onOpen() {}

  public abstract void onEvent(JsonObject message);

  public abstract void onClose(int code, String reason);

  public void onError(Throwable throwable) {
    log.warn("Qwen Live callback error.", throwable);
  }
}
