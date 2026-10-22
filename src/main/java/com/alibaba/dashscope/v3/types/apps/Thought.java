// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.apps;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One planning or tool-call step; set {@code has_thoughts} to get them. */
public final class Thought {
  @JsonProperty("thought")
  private String thought;

  @JsonProperty("action_type")
  private String actionType;

  @JsonProperty("action_name")
  private String actionName;

  @JsonProperty("action")
  private String action;

  @JsonProperty("action_input")
  private Map<String, Object> actionInput;

  @JsonProperty("action_input_stream")
  private String actionInputStream;

  @JsonProperty("response")
  private String response;

  @JsonProperty("observation")
  private String observation;

  private final Map<String, Object> extra = new LinkedHashMap<>();

  public String getThought() {
    return thought;
  }

  public String getActionType() {
    return actionType;
  }

  public String getActionName() {
    return actionName;
  }

  public String getAction() {
    return action;
  }

  public Map<String, Object> getActionInput() {
    return actionInput;
  }

  public String getActionInputStream() {
    return actionInputStream;
  }

  public String getResponse() {
    return response;
  }

  public String getObservation() {
    return observation;
  }

  /** Fields this SDK version does not model yet (forward compatibility, design §4). */
  public Map<String, Object> getExtra() {
    return Collections.unmodifiableMap(extra);
  }

  @JsonAnySetter
  public void putExtra(String key, Object value) {
    extra.put(key, value);
  }
}
