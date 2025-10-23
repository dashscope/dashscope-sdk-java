// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import lombok.Builder;
import lombok.Data;

/** @author songsong.shao */
@Builder
@Data
public class OmniRealtimeTranslationParam {
    /** language for translation */
    private String language;
}