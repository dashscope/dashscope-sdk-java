// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.base.FullDuplexServiceParam;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public class FullDuplexRequest {
  FullDuplexServiceParam param;
  ServiceOption serviceOption;
  boolean isFlattenResult = false;

  public FullDuplexRequest(FullDuplexServiceParam param, ServiceOption option) {
    this.param = param;
    this.serviceOption = option;
  }

  public boolean getIsFlatten() {
    return serviceOption.getIsFlatten();
  }

  public String getBaseWebSocketUrl() {
    return serviceOption.getBaseWebSocketUrl();
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
    request.addProperty(ApiKeywords.MODEL, param.getModel());
    request.addProperty(ApiKeywords.TASK_GROUP, serviceOption.getTaskGroup());
    request.addProperty(ApiKeywords.TASK, serviceOption.getTask());
    request.addProperty(ApiKeywords.FUNCTION, serviceOption.getFunction());
    request.add(ApiKeywords.INPUT, new JsonObject());
    if (param.getParameters() != null) {
      request.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(param.getParameters()));
    }
    if (param.getResources() != null) {
      request.add(ApiKeywords.RESOURCES, (JsonElement) param.getResources());
    }
    return request;
  }

  public JsonObject getWebSocketPayload(Object data) {
    JsonObject request = new JsonObject();
    request.addProperty(ApiKeywords.MODEL, param.getModel());
    request.addProperty(ApiKeywords.TASK_GROUP, serviceOption.getTaskGroup());
    request.addProperty(ApiKeywords.TASK, serviceOption.getTask());
    request.addProperty(ApiKeywords.FUNCTION, serviceOption.getFunction());
    if (data instanceof ByteBuffer) {
      request.add(ApiKeywords.INPUT, new JsonObject()); // empty input
    } else if (data instanceof Byte[]) request.add("input", new JsonObject());
    else {
      request.add(ApiKeywords.INPUT, JsonUtils.toJsonElement(data));
    }
    if (param.getParameters() != null) {
      request.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(param.getParameters()));
    }
    if (param.getResources() != null) {
      request.add(ApiKeywords.RESOURCES, (JsonObject) param.getResources());
    }
    return request;
  }

  public JsonObject getStartTaskMessage() {
    JsonObject header = new JsonObject();
    header.addProperty(ApiKeywords.ACTION, WebSocketEventType.RUN_TASK.getValue());
    header.addProperty(ApiKeywords.TASKID, UUID.randomUUID().toString());
    header.addProperty(ApiKeywords.STREAMING, serviceOption.getStreamingMode().getValue());
    JsonObject wsMessage = new JsonObject();
    wsMessage.add(ApiKeywords.HEADER, header);
    wsMessage.add(ApiKeywords.PAYLOAD, getWebSocketPayload());
    return wsMessage;
  }

  public JsonObject getStartTaskMessage(Object payloadData) {
    JsonObject header = new JsonObject();
    header.addProperty(ApiKeywords.ACTION, WebSocketEventType.RUN_TASK.getValue());
    header.addProperty(ApiKeywords.TASKID, UUID.randomUUID().toString());
    header.addProperty(ApiKeywords.STREAMING, serviceOption.getStreamingMode().getValue());
    JsonObject wsMessage = new JsonObject();
    wsMessage.add(ApiKeywords.HEADER, header);
    wsMessage.add(ApiKeywords.PAYLOAD, getWebSocketPayload(payloadData));
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
    header.addProperty(ApiKeywords.ACTION, WebSocketEventType.CONTINUE_TASK.getValue());
    header.addProperty(ApiKeywords.TASKID, UUID.randomUUID().toString());
    header.addProperty(ApiKeywords.STREAMING, serviceOption.getStreamingMode().getValue());
    // websocket package.
    JsonObject wsMessage = new JsonObject();
    wsMessage.add(ApiKeywords.HEADER, header);
    wsMessage.add(ApiKeywords.PAYLOAD, getWebSocketPayload());
    return wsMessage;
  }

  public JsonObject getContinueMessage(String data, String taskId) {
    JsonObject header = new JsonObject();
    header.addProperty(ApiKeywords.ACTION, WebSocketEventType.CONTINUE_TASK.getValue());
    header.addProperty(ApiKeywords.TASKID, taskId);
    header.addProperty(ApiKeywords.STREAMING, serviceOption.getStreamingMode().getValue());
    // websocket package.
    JsonObject wsMessage = new JsonObject();
    wsMessage.add(ApiKeywords.HEADER, header);
    wsMessage.add(ApiKeywords.PAYLOAD, getWebSocketPayload(data));
    return wsMessage;
  }

  public JsonObject getContinueMessage(Object data, String taskId) {
    JsonObject header = new JsonObject();
    header.addProperty(ApiKeywords.ACTION, WebSocketEventType.CONTINUE_TASK.getValue());
    header.addProperty(ApiKeywords.TASKID, taskId);
    header.addProperty(ApiKeywords.STREAMING, serviceOption.getStreamingMode().getValue());
    // websocket package.
    JsonObject wsMessage = new JsonObject();
    wsMessage.add(ApiKeywords.HEADER, header);
    wsMessage.add(ApiKeywords.PAYLOAD, getWebSocketPayload(data));
    return wsMessage;
  }

  public JsonObject getFinishedTaskMessage(String taskId) {
    JsonObject header = new JsonObject();
    header.addProperty(ApiKeywords.ACTION, WebSocketEventType.FINISH_TASK.getValue());
    header.addProperty(ApiKeywords.TASKID, taskId);
    header.addProperty(ApiKeywords.STREAMING, serviceOption.getStreamingMode().getValue());
    // websocket package.
    JsonObject wsMessage = new JsonObject();
    wsMessage.add(ApiKeywords.HEADER, header);
    JsonObject payload = new JsonObject();
    payload.add("input", new JsonObject());
    wsMessage.add(ApiKeywords.PAYLOAD, payload);
    return wsMessage;
  }

  public Map<String, String> getHeaders() {
    return param.getHeaders();
  }

  public String getWorkspace() {
    return param.getWorkspace();
  }
}
