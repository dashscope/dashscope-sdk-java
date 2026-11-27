// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.multimodal;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A multimodal chat message: role plus a list of content parts (text/image/audio/video). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class MultimodalMessage {
  private final String role;
  private final List<ContentPart> content;
  private final String name;

  @JsonProperty("tool_calls")
  private final List<Map<String, Object>> toolCalls;

  @JsonProperty("tool_call_id")
  private final String toolCallId;

  @JsonProperty("reasoning_content")
  private final String reasoningContent;

  private final Map<String, Object> extra = new LinkedHashMap<>();

  private MultimodalMessage(
      String role,
      List<ContentPart> content,
      String name,
      List<Map<String, Object>> toolCalls,
      String toolCallId,
      String reasoningContent) {
    this.role = role;
    this.content = content;
    this.name = name;
    this.toolCalls = toolCalls;
    this.toolCallId = toolCallId;
    this.reasoningContent = reasoningContent;
  }

  public static MultimodalMessage of(String role, List<ContentPart> content) {
    return new MultimodalMessage(role, content, null, null, null, null);
  }

  public static MultimodalMessage user(List<ContentPart> content) {
    return of("user", content);
  }

  public static MultimodalMessage system(List<ContentPart> content) {
    return of("system", content);
  }

  public static MultimodalMessage assistant(List<ContentPart> content) {
    return of("assistant", content);
  }

  public static MultimodalMessage tool(List<ContentPart> content, String toolCallId) {
    return new MultimodalMessage("tool", content, null, null, toolCallId, null);
  }

  public String getRole() {
    return role;
  }

  public List<ContentPart> getContent() {
    return content;
  }

  public String getName() {
    return name;
  }

  public List<Map<String, Object>> getToolCalls() {
    return toolCalls;
  }

  public String getToolCallId() {
    return toolCallId;
  }

  public String getReasoningContent() {
    return reasoningContent;
  }

  @JsonIgnore
  public Map<String, Object> getExtra() {
    return Collections.unmodifiableMap(extra);
  }

  @JsonAnySetter
  public void putExtra(String key, Object value) {
    extra.put(key, value);
  }

  public Builder toBuilder() {
    return new Builder()
        .role(role)
        .content(content)
        .name(name)
        .toolCalls(toolCalls)
        .toolCallId(toolCallId)
        .reasoningContent(reasoningContent)
        .extra(extra);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String role;
    private List<ContentPart> content;
    private String name;
    private List<Map<String, Object>> toolCalls;
    private String toolCallId;
    private String reasoningContent;
    private Map<String, Object> extra;

    public Builder role(String role) {
      this.role = role;
      return this;
    }

    public Builder content(List<ContentPart> content) {
      this.content = content == null ? null : new ArrayList<>(content);
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder toolCalls(List<Map<String, Object>> toolCalls) {
      this.toolCalls = toolCalls == null ? null : new ArrayList<>(toolCalls);
      return this;
    }

    public Builder toolCallId(String toolCallId) {
      this.toolCallId = toolCallId;
      return this;
    }

    public Builder reasoningContent(String reasoningContent) {
      this.reasoningContent = reasoningContent;
      return this;
    }

    public Builder extra(Map<String, Object> extra) {
      this.extra = extra;
      return this;
    }

    public MultimodalMessage build() {
      MultimodalMessage m =
          new MultimodalMessage(role, content, name, toolCalls, toolCallId, reasoningContent);
      if (extra != null) {
        extra.forEach(m::putExtra);
      }
      return m;
    }
  }
}
