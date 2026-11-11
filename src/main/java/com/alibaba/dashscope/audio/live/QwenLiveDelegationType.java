// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import com.google.gson.annotations.SerializedName;

public enum QwenLiveDelegationType {
  @SerializedName("none")
  NONE,
  @SerializedName("client")
  CLIENT,
  @SerializedName("responses")
  RESPONSES
}
