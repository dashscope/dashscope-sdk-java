// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.audio.live;

import com.google.gson.annotations.SerializedName;

@Deprecated
public enum QwenLiveDelegationType {
  @SerializedName("none")
  NONE,
  @SerializedName("client")
  CLIENT,
  @SerializedName("responses")
  RESPONSES
}
