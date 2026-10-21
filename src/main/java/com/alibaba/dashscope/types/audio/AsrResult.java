// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.audio;

import com.alibaba.dashscope.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One typed ASR result, mapped from a {@code result-generated} payload's {@code output.sentence}
 * plus usage. Unknown sentence fields are kept in {@link Sentence#getExtra()}.
 */
public class AsrResult {
  @JsonProperty("sentence")
  private Sentence sentence;

  @JsonProperty("usage")
  private Usage usage;

  public Sentence getSentence() {
    return sentence;
  }

  public Usage getUsage() {
    return usage;
  }

  public static class Sentence {
    @JsonProperty("begin_time")
    private Long beginTime;

    @JsonProperty("end_time")
    private Long endTime;

    @JsonProperty("text")
    private String text;

    @JsonProperty("heartbeat")
    private Boolean heartbeat;

    @JsonProperty("sentence_end")
    private Boolean sentenceEnd;

    private final Map<String, Object> extra = new LinkedHashMap<>();

    public Long getBeginTime() {
      return beginTime;
    }

    public Long getEndTime() {
      return endTime;
    }

    public String getText() {
      return text;
    }

    /** Server-side business heartbeat — {@code results()} filters these out. */
    public Boolean getHeartbeat() {
      return heartbeat;
    }

    public Boolean getSentenceEnd() {
      return sentenceEnd;
    }

    @JsonIgnore
    public Map<String, Object> getExtra() {
      return Collections.unmodifiableMap(extra);
    }

    @JsonAnySetter
    public void putExtra(String key, Object value) {
      extra.put(key, value);
    }
  }
}
