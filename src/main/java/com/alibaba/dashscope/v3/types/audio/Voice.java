// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** One cloned voice; {@code voices().create}/{@code get} return it. */
public final class Voice extends ResponseBase {
  @JsonProperty("voice_id")
  private String voiceId;

  @JsonProperty("status")
  private String status;

  @JsonProperty("preview_audio")
  private Map<String, Object> previewAudio;

  public String getVoiceId() {
    return voiceId;
  }

  public String getStatus() {
    return status;
  }

  public Map<String, Object> getPreviewAudio() {
    return previewAudio;
  }
}
