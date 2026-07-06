// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/** @author songsong.shao */
@Builder
@Data
public class OmniRealtimeTranslationParam {
  /** language for translation */
  private String language;

  private Corpus corpus;

  @Builder
  @Data
  public static class Corpus {
    /** Custom phrases to improve translation accuracy */
    private Map<String, Object> phrases; // translation phrases,
  }
}
