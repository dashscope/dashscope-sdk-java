// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import com.alibaba.dashscope.protocol.WebSocketEventType;
import com.alibaba.dashscope.protocol.WebSocketResponse;
import com.alibaba.dashscope.protocol.WebSocketResponseHeader;
import com.alibaba.dashscope.protocol.WebSocketResponsePayload;
import com.google.gson.JsonElement;
import java.util.UUID;

public class WebSocketServerMessage {
  public static WebSocketResponse getTaskStartMessage() {
    WebSocketResponseHeader header = new WebSocketResponseHeader();
    header.setTaskId(UUID.randomUUID().toString());
    header.setEvent(WebSocketEventType.TASK_STARTED);
    WebSocketResponsePayload payload = new WebSocketResponsePayload();
    WebSocketResponse wsResponse = new WebSocketResponse();
    wsResponse.header = header;
    wsResponse.payload = payload; // empty payload
    return wsResponse;
  }

  public static WebSocketResponse getTaskGeneratedMessage(JsonElement output, JsonElement usage) {
    WebSocketResponseHeader header = new WebSocketResponseHeader();
    header.setTaskId(UUID.randomUUID().toString());
    header.setEvent(WebSocketEventType.RESULT_GENERATED);
    WebSocketResponsePayload payload = new WebSocketResponsePayload();
    payload.output = output;
    payload.usage = usage;
    WebSocketResponse wsResponse = new WebSocketResponse();
    wsResponse.header = header;
    wsResponse.payload = payload; // empty payload
    return wsResponse;
  }

  public static WebSocketResponse getTaskFinishedMessage(JsonElement output, JsonElement usage) {
    WebSocketResponseHeader header = new WebSocketResponseHeader();
    header.setTaskId(UUID.randomUUID().toString());
    header.setEvent(WebSocketEventType.TASK_FINISHED);
    WebSocketResponsePayload payload = new WebSocketResponsePayload();
    if (output != null) {
      payload.output = output;
    }
    if (usage != null) {
      payload.usage = usage;
    }
    WebSocketResponse wsResponse = new WebSocketResponse();
    wsResponse.header = header;
    wsResponse.payload = payload; // empty payload
    return wsResponse;
  }
}
