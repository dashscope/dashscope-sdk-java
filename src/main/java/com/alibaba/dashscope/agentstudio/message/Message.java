// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.message;

import com.alibaba.dashscope.agentstudio.model.Session;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
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

  @SerializedName("code")
  private String code;

  @SerializedName("message")
  private String message;

  /**
   * Extract stop_reason from a session_status event's data block.
   *
   * <p>The SSE session_status idle event carries stop_reason in its data: {@code
   * {"stop_reason":{"type":"end_turn"},"session_status":"idle"}}. This helper parses it out so
   * callers don't need to manually dig through DataContent blocks.
   *
   * @return stop_reason (has type, eventIds), or null if not a session_status event or absent
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
}
