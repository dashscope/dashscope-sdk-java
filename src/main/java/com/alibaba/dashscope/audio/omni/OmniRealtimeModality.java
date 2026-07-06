// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import com.google.gson.annotations.SerializedName;

/** @author lengjiayi */
public enum OmniRealtimeModality {
  @SerializedName("text")
  TEXT("text"),
  @SerializedName("audio")
  AUDIO("audio");
  private final String name;

  // Constructor
  OmniRealtimeModality(String name) {
    this.name = name;
  }

  // Getter methods
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
