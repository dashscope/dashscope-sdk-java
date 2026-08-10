// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import com.google.gson.annotations.SerializedName;

/**
 * Audio codec/format type used in the new-style upstream/downstream audio configuration (see {@link
 * OmniRealtimeAudioFormatConfig}), e.g.
 */
public enum OmniRealtimeAudioCodec {
  @SerializedName("pcm")
  PCM,
  @SerializedName("wav")
  WAV;

  public static OmniRealtimeAudioCodec fromValue(String type) {
    if (type == null) {
      return PCM;
    }
    switch (type.toLowerCase()) {
      case "pcm":
        return PCM;
      case "wav":
        return WAV;
      default:
        throw new IllegalArgumentException(
            "Unsupported audio format: " + type + ", supported values are: pcm, wav");
    }
  }
}
