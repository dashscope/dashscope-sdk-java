// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.multimodal;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Response-side multimodal message: role plus content parts, with optional reasoning content, tool
 * calls, and audio payload (omni models). Unknown fields are kept in {@link #getExtra()}.
 */
public class MultimodalOutputMessage {
  @JsonProperty("role")
  private String role;

  @JsonProperty("content")
  private List<ContentPart> content;

  @JsonProperty("reasoning_content")
  private String reasoningContent;

  @JsonProperty("tool_calls")
  private List<Map<String, Object>> toolCalls;

  @JsonProperty("audio")
  private Map<String, Object> audio;

  private final Map<String, Object> extra = new LinkedHashMap<>();

  public String getRole() {
    return role;
  }

  public List<ContentPart> getContent() {
    return content;
  }

  public String getReasoningContent() {
    return reasoningContent;
  }

  public List<Map<String, Object>> getToolCalls() {
    return toolCalls;
  }

  /** Omni audio payload: id / url / data / expires_at. */
  public Map<String, Object> getAudio() {
    return audio;
  }

  @JsonIgnore
  public Map<String, Object> getExtra() {
    return Collections.unmodifiableMap(extra);
  }

  @JsonAnySetter
  public void putExtra(String key, Object value) {
    extra.put(key, value);
  }

  /** Joined text of all text parts; empty string when absent. */
  public String text() {
    if (content == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (ContentPart part : content) {
      if (part != null && part.has("text") && part.getText() != null) {
        sb.append(part.getText());
      }
    }
    return sb.toString();
  }
}
