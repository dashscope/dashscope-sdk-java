// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.base.FullDuplexServiceParam;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.UUID;

public class FullDuplexRequest {
  FullDuplexServiceParam param;
  ServiceOption serviceOption;

  public FullDuplexRequest(FullDuplexServiceParam param, ServiceOption option) {
    this.param = param;
    this.serviceOption = option;
  }

  public String getApiKey() {
    return param.getApiKey();
  }

  public StreamingMode getStreamingMode() {
    return serviceOption.getStreamingMode();
  }

  public OutputMode getOutputMode() {
    return serviceOption.getOutputMode();
  }

  public boolean isSecurityCheck() {
    return param.isSecurityCheck();
  }

  public JsonObject getWebSocketPayload() {
    JsonObject request = new JsonObject();
    request.addProperty("model", param.getModel());
    request.addProperty("task_group", serviceOption.getTaskGroup());
    request.addProperty("task", serviceOption.getTask());
    request.addProperty("function", serviceOption.getFunction());
    request.add("input", new JsonObject());
    if (param.getParameters() != null) {
      request.add("parameters", JsonUtils.parametersToJsonObject(param.getParameters()));
    }
    return request;
  }

  public JsonObject getWebSocketPayload(Object data) {
    JsonObject request = new JsonObject();
    request.addProperty("model", param.getModel());
    request.addProperty("task_group", serviceOption.getTaskGroup());
    request.addProperty("task", serviceOption.getTask());
    request.addProperty("function", serviceOption.getFunction());
    if (data instanceof ByteBuffer) {
      request.add("input", new JsonObject()); // empty input
    } else if (data instanceof Byte[]) request.add("input", new JsonObject());
    else {
      request.add("input", JsonUtils.toJsonElement(data));
    }
    if (param.getParameters() != null) {
      request.add("parameters", JsonUtils.parametersToJsonObject(param.getParameters()));
    }
    return request;
  }

  public JsonObject getStartTaskMessage() {
    JsonObject header = new JsonObject();
    header.addProperty("action", WebSocketEventType.RUN_TASK.getValue());
    header.addProperty("task_id", UUID.randomUUID().toString());
    header.addProperty("streaming", serviceOption.getStreamingMode().getValue());
    JsonObject wsMessage = new JsonObject();
    wsMessage.add("header", header);
    wsMessage.add("payload", getWebSocketPayload());
    return wsMessage;
  }

  public JsonObject getStartTaskMessage(Object payloadData) {
    JsonObject header = new JsonObject();
    header.addProperty("action", WebSocketEventType.RUN_TASK.getValue());
    header.addProperty("task_id", UUID.randomUUID().toString());
    header.addProperty("streaming", serviceOption.getStreamingMode().getValue());
    JsonObject wsMessage = new JsonObject();
    wsMessage.add("header", header);
    wsMessage.add("payload", getWebSocketPayload(payloadData));
    return wsMessage;
  }

  /**
   * Only for websocket.
   *
   * @return The stream data.
   */
  public Flowable<Object> getStreamingData() {
    return param.getStreamingData();
  }

  public JsonObject getContinueMessage() {
    JsonObject header = new JsonObject();
    header.addProperty("action", WebSocketEventType.CONTINUE_TASK.getValue());
    header.addProperty("task_id", UUID.randomUUID().toString());
    header.addProperty("streaming", serviceOption.getStreamingMode().getValue());
    // websocket package.
    JsonObject wsMessage = new JsonObject();
    wsMessage.add("header", header);
    wsMessage.add("payload", getWebSocketPayload());
    return wsMessage;
  }

  public JsonObject getContinueMessage(String data, String taskId) {
    JsonObject header = new JsonObject();
    header.addProperty("action", WebSocketEventType.CONTINUE_TASK.getValue());
    header.addProperty("task_id", taskId);
    header.addProperty("streaming", serviceOption.getStreamingMode().getValue());
    // websocket package.
    JsonObject wsMessage = new JsonObject();
    wsMessage.add("header", header);
    wsMessage.add("payload", getWebSocketPayload(data));
    return wsMessage;
  }

  public JsonObject getContinueMessage(Object data, String taskId) {
    JsonObject header = new JsonObject();
    header.addProperty("action", WebSocketEventType.CONTINUE_TASK.getValue());
    header.addProperty("task_id", taskId);
    header.addProperty("streaming", serviceOption.getStreamingMode().getValue());
    // websocket package.
    JsonObject wsMessage = new JsonObject();
    wsMessage.add("header", header);
    wsMessage.add("payload", getWebSocketPayload(data));
    return wsMessage;
  }

  public JsonObject getFinishedTaskMessage(String taskId) {
    JsonObject header = new JsonObject();
    header.addProperty("action", WebSocketEventType.FINISH_TASK.getValue());
    header.addProperty("task_id", taskId);
    header.addProperty("streaming", serviceOption.getStreamingMode().getValue());
    // websocket package.
    JsonObject wsMessage = new JsonObject();
    wsMessage.add("header", header);
    JsonObject payload = new JsonObject();
    payload.add("input", new JsonObject());
    wsMessage.add("payload", payload);
    return wsMessage;
  }
}
