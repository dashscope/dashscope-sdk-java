// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.message;

import com.alibaba.dashscope.agentstudio.model.Session;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class Message {
  @SerializedName("object")
  private String object;

  @SerializedName("status")
  private String status;

  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("role")
  private String role;

  @SerializedName("content")
  private List<ContentBlock> content;

  @SerializedName("metadata")
  private Map<String, Object> metadata;

  @SerializedName("is_error")
  private Boolean isError;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("sequence_number")
  private Long sequenceNumber;

  @SerializedName("session_thread_id")
  private String sessionThreadId;

  /** Thread this event belongs to; server sends it on all {@code thread_*} events. */
  @SerializedName("thread_id")
  private String threadId;

  @SerializedName("code")
  private String code;

  @SerializedName("message")
  private String message;

  // ── delta-protocol frame fields (opt-in via event_deltas) ──
  @SerializedName("error")
  private JsonObject errorPayload;

  @SerializedName("event")
  private JsonObject eventPayload;

  @SerializedName("event_id")
  private String deltaEventId;

  @SerializedName("delta")
  private JsonObject deltaPayload;

  /**
   * Extract stop_reason from a session_status event's data block.
   *
   * <p>The SSE session_status idle event carries stop_reason in its data: {@code
   * {"stop_reason":{"type":"end_turn"},"session_status":"idle"}}. This helper parses it out so
   * callers don't need to manually dig through DataContent blocks.
   *
   * @return stop_reason (has type, pendingBatchId, pendingCallIds), or null if not a session_status
   *     event or absent
   */
  public Session.StopReason getStopReason() {
    if (!"session_status".equals(type) || content == null) return null;
    for (ContentBlock block : content) {
      if (block instanceof ContentBlock.DataContent) {
        JsonObject data = ((ContentBlock.DataContent) block).getData();
        if (data != null && data.has("stop_reason") && !data.get("stop_reason").isJsonNull()) {
          return JsonUtils.fromJson(data.get("stop_reason"), Session.StopReason.class);
        }
      }
    }
    return null;
  }

  // typed data accessors

  /** The first content block's {@code data} payload, for events that carry one. */
  public JsonObject getData() {
    if (content == null) return null;
    for (ContentBlock block : content) {
      if (block instanceof ContentBlock.DataContent) {
        return ((ContentBlock.DataContent) block).getData();
      }
    }
    return null;
  }

  /** {@code tool_approval_request} payload: batch_id/call_id/name/arguments/tool_type. */
  public JsonObject getToolApprovalRequest() {
    if (!"tool_approval_request".equals(type)) return null;
    return getData();
  }

  /** {@code {code, message}} from {@code type=error} events. */
  public JsonObject getError() {
    if (!"error".equals(type)) return null;
    return errorPayload;
  }

  /** Suspend signal {@code {batch_id, call_ids}} from metadata while the approval barrier is up. */
  @SuppressWarnings("unchecked")
  public Map<String, Object> getPendingToolApprovals() {
    if (metadata == null) return null;
    Object pending = metadata.get("pending_tool_approvals");
    return pending instanceof Map ? (Map<String, Object>) pending : null;
  }

  /** {@code model_request_end} usage payload. */
  public JsonObject getModelRequestEnd() {
    if (!"model_request_end".equals(type)) return null;
    return getData();
  }

  /** {@code outcome_evaluation} payload (outcome_id/iteration/phase/result). */
  public JsonObject getOutcomeEvaluation() {
    if (!"outcome_evaluation".equals(type)) return null;
    return getData();
  }

  /** {@code thread_status} payload (session_thread_id/agent_name/thread_status/stop_reason). */
  public JsonObject getThreadStatus() {
    if (!"thread_status".equals(type)) return null;
    return getData();
  }

  /** {@code thread_created} payload (session_thread_id/agent_name). */
  public JsonObject getThreadCreated() {
    if (!"thread_created".equals(type)) return null;
    return getData();
  }

  /** {@code session_updated} payload (title/session_metadata/agent). */
  public JsonObject getSessionUpdated() {
    if (!"session_updated".equals(type)) return null;
    return getData();
  }

  /** Sub-agent routing from {@code thread_message_sent}/{@code received} metadata. */
  public Map<String, Object> getThreadMessageRouting() {
    if (!"thread_message_sent".equals(type) && !"thread_message_received".equals(type)) {
      return null;
    }
    if (metadata == null) return null;
    Map<String, Object> routing = new HashMap<>();
    for (String key :
        new String[] {
          "to_session_thread_id", "to_agent_name",
          "from_session_thread_id", "from_agent_name"
        }) {
      if (metadata.containsKey(key)) routing.put(key, metadata.get(key));
    }
    return routing.isEmpty() ? null : routing;
  }

  // ── delta-protocol frames (opt-in via event_deltas) ──

  /** {@code {id, type}} from {@code event_start} frames (preview, no content). */
  public JsonObject getEventStart() {
    if (!"event_start".equals(type)) return null;
    return eventPayload;
  }

  /** Delta payload from {@code event_delta} frames. */
  public JsonObject getEventDelta() {
    if (!"event_delta".equals(type)) return null;
    return deltaPayload;
  }

  /** Incremental text from an {@code event_delta}'s {@code content_delta}. */
  public String getDeltaText() {
    if (!"event_delta".equals(type) || deltaPayload == null) return null;
    if (deltaPayload.has("content") && deltaPayload.get("content").isJsonObject()) {
      JsonObject c = deltaPayload.getAsJsonObject("content");
      if (c.has("text")) return c.get("text").getAsString();
    }
    return null;
  }
}
