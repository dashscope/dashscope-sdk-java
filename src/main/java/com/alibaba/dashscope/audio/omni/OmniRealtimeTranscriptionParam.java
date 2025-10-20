package com.alibaba.dashscope.audio.omni;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/** @author songsong.shao */
@Data
public class OmniRealtimeTranscriptionParam {
    /** input audio sample rate*/
    private Integer inputSampleRate = null;
    /** input audio format */
    private String inputAudioFormat = null;
    /** input audio language */
    private String language = null;

    /** corpus for qwen-asr-realtime */
    private Map<String, Object> corpus = null;

    /** text content for corpus */
    private String corpusText;

    /**
     * Set text in corpus to improve model recognition accuracy.
     */
    public void setCorpusText(String text) {
        if (corpus == null) {
            corpus = new HashMap<>();
        }
        this.corpusText = text;
        corpus.put("text", text);
    }

    /**
     * Default constructor
     */
    public OmniRealtimeTranscriptionParam() {
    }

    public OmniRealtimeTranscriptionParam(String audioFormat, int sampleRate) {
        this.inputAudioFormat = audioFormat;
        this.inputSampleRate = sampleRate;
    }
}