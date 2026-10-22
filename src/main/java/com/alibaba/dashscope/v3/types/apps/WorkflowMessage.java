// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.apps;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Workflow node progress, from {@code output.workflow_message}. */
public final class WorkflowMessage {
  @JsonProperty("node_id")
  private String nodeId;

  @JsonProperty("node_name")
  private String nodeName;

  @JsonProperty("node_type")
  private String nodeType;

  @JsonProperty("node_status")
  private String nodeStatus;

  /** The wire spells the completion flag as a string, not a bool. */
  @JsonProperty("node_is_completed")
  private String nodeIsCompleted;

  @JsonProperty("node_msg_seq_id")
  private Integer nodeMsgSeqId;

  @JsonProperty("message")
  private NodeMessage message;

  private final Map<String, Object> extra = new LinkedHashMap<>();

  public String getNodeId() {
    return nodeId;
  }

  public String getNodeName() {
    return nodeName;
  }

  public String getNodeType() {
    return nodeType;
  }

  public String getNodeStatus() {
    return nodeStatus;
  }

  public String getNodeIsCompleted() {
    return nodeIsCompleted;
  }

  public Integer getNodeMsgSeqId() {
    return nodeMsgSeqId;
  }

  public NodeMessage getMessage() {
    return message;
  }

  /** Fields this SDK version does not model yet (forward compatibility, design §4). */
  public Map<String, Object> getExtra() {
    return Collections.unmodifiableMap(extra);
  }

  @JsonAnySetter
  public void putExtra(String key, Object value) {
    extra.put(key, value);
  }

  /** The node's own message payload. */
  public static final class NodeMessage {
    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;

    private final Map<String, Object> extra = new LinkedHashMap<>();

    public String getRole() {
      return role;
    }

    public String getContent() {
      return content;
    }

    public Map<String, Object> getExtra() {
      return Collections.unmodifiableMap(extra);
    }

    @JsonAnySetter
    public void putExtra(String key, Object value) {
      extra.put(key, value);
    }
  }
}
