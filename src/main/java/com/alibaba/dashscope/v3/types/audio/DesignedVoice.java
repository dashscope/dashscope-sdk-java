// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** One designed voice; its id field is {@code voice} on the wire (not {@code voice_id}). */
public final class DesignedVoice extends ResponseBase {
  @JsonProperty("voice")
  private String voice;

  @JsonProperty("preview_audio")
  private Map<String, Object> previewAudio;

  @JsonProperty("language")
  private String language;

  @JsonProperty("target_model")
  private String targetModel;

  /** The designed voice's id (wire field {@code voice}). */
  public String getVoice() {
    return voice;
  }

  public Map<String, Object> getPreviewAudio() {
    return previewAudio;
  }

  public String getLanguage() {
    return language;
  }

  public String getTargetModel() {
    return targetModel;
  }
}
