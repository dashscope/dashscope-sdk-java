// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.apps;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.alibaba.dashscope.v3.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * One Bailian application completion; the same shape serves the non-streaming response and each
 * streaming frame (the wire makes no distinction). Envelope fields live on {@link ResponseBase};
 * per-model usage detail rides in {@link Usage#getExtra()} under {@code "models"} (design §3).
 */
public class AppCompletion extends ResponseBase {
  @JsonProperty("text")
  private String text;

  @JsonProperty("finish_reason")
  private String finishReason;

  @JsonProperty("session_id")
  private String sessionId;

  @JsonProperty("thoughts")
  private List<Thought> thoughts;

  @JsonProperty("doc_references")
  private List<DocReference> docReferences;

  @JsonProperty("workflow_message")
  private WorkflowMessage workflowMessage;

  @JsonProperty("usage")
  private Usage usage;

  public String getText() {
    return text;
  }

  public String getFinishReason() {
    return finishReason;
  }

  public String getSessionId() {
    return sessionId;
  }

  public List<Thought> getThoughts() {
    return thoughts;
  }

  public List<DocReference> getDocReferences() {
    return docReferences;
  }

  public WorkflowMessage getWorkflowMessage() {
    return workflowMessage;
  }

  public Usage getUsage() {
    return usage;
  }
}
