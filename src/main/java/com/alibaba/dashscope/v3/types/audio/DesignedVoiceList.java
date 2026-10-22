// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Page of designed voices; items keep their wire shape. */
public final class DesignedVoiceList extends ResponseBase {
  @JsonProperty("voices")
  @JsonAlias("voice_list")
  private List<Map<String, Object>> voices = new ArrayList<>();

  public List<Map<String, Object>> getVoices() {
    return voices;
  }
}
