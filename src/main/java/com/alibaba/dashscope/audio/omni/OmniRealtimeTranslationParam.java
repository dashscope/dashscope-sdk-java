// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;

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
        /** corpus for qwen-asr-realtime */
        private HashMap<String, Object> phrases; // translation phrases,
    }

}