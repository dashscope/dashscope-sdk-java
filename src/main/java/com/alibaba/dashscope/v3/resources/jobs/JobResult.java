// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.jobs;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One artifact a finished task produced (Python v2 {@code JobResult} parity). Image tasks answer
 * with {@code url}, transcription tasks with {@code transcription_url}; anything else a service
 * adds stays in {@link #getExtra()}.
 */
public final class JobResult {
  @JsonProperty("url")
  private String url;

  @JsonProperty("transcription_url")
  private String transcriptionUrl;

  private final Map<String, Object> extra = new LinkedHashMap<>();

  public String getUrl() {
    return url;
  }

  public String getTranscriptionUrl() {
    return transcriptionUrl;
  }

  public Map<String, Object> getExtra() {
    return Collections.unmodifiableMap(extra);
  }

  @JsonAnySetter
  public void putExtra(String key, Object value) {
    extra.put(key, value);
  }
}
