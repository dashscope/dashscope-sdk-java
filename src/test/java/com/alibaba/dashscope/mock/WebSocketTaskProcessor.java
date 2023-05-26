// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.mock;

import com.alibaba.dashscope.aigc.conversation.ConversationMessagePayload;
import com.alibaba.dashscope.common.StreamingMode;
import com.alibaba.dashscope.common.WebSocketEventType;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsMessageContext;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class WebSocketTaskProcessor {
  private final WsMessageContext ws;

  private final WsBinaryMessageContext wsb;

  private final String taskId;

  private final StreamingMode streamingMode;

  private final int code;

  public WebSocketTaskProcessor(
      WsMessageContext ws, String taskId, String streamingMode, int code) {
    this.ws = ws;
    this.wsb = null;
    this.taskId = taskId;
    this.streamingMode = StreamingMode.of(streamingMode);
    this.code = code;
  }

  public WebSocketTaskProcessor(WsBinaryMessageContext wsb, String streamingMode, int code) {
    this.ws = null;
    this.wsb = wsb;
    this.taskId = null;
    this.streamingMode = StreamingMode.of(streamingMode);
    this.code = code;
  }

  private static JsonObject getTestMessage(String prompt) {
    ConversationMessagePayload payload = new ConversationMessagePayload();
    JsonObject output = new JsonObject();
    output.add(ApiKeywords.TEXT, new JsonPrimitive(prompt));
    payload.setOutput(output);
    JsonObject usage = new JsonObject();
    usage.add(ApiKeywords.INPUT_TOKENS, new JsonPrimitive(100));
    usage.add(ApiKeywords.OUTPUT_TOKENS, new JsonPrimitive(200));
    payload.setUsage(usage);
    return JsonUtils.toJsonObject(payload);
  }

  public void asyncCall(String action, String prompt) {
    CompletableFuture.runAsync(
        () -> {
          try {
            switch (streamingMode) {
              case NONE:
                {
                  JsonObject message = getTestMessage(prompt);
                  sendStartEvent();
                  if (code == 0) {
                    sendTaskFinished(message);
                  } else {
                    sendTaskFailed(message);
                  }
                  break;
                }
              case OUT:
                JsonObject message = getTestMessage(prompt);
                sendStartEvent();
                if (code == 0) {
                  sendResultGenerated(message);
                  sendTaskFinished(message);
                } else {
                  sendTaskFailed(message);
                }
                break;
              case DUPLEX:
                if (wsb != null) {
                  // Returns original binary to test binary message.
                  sendBinary(ByteBuffer.wrap(wsb.data()));
                } else {
                  if (action.equals(WebSocketEventType.RUN_TASK.getValue())) {
                    sendStartEvent();
                  } else if (action.equals(WebSocketEventType.FINISH_TASK.getValue())) {
                    sendTaskFinished(new JsonObject());
                  } else {
                    throw new RuntimeException("Unexpected action:" + action);
                  }
                }
                break;
              default:
                throw new UnsupportedOperationException();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
  }

  public void sendStartEvent() {
    JsonObject headers = new JsonObject();
    headers.add(ApiKeywords.TASKID, new JsonPrimitive(taskId));
    headers.add(ApiKeywords.EVENT, new JsonPrimitive(WebSocketEventType.TASK_STARTED.getValue()));
    JsonObject payload = new JsonObject();
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendResultGenerated(JsonObject payload) {
    JsonObject headers = new JsonObject();
    headers.add(ApiKeywords.TASKID, new JsonPrimitive(taskId));
    headers.add(
        ApiKeywords.EVENT, new JsonPrimitive(WebSocketEventType.RESULT_GENERATED.getValue()));
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendTaskFinished(JsonObject payload) {
    JsonObject headers = new JsonObject();
    headers.add(ApiKeywords.TASKID, new JsonPrimitive(taskId));
    headers.add(ApiKeywords.EVENT, new JsonPrimitive(WebSocketEventType.TASK_FINISHED.getValue()));
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendTaskFailed(JsonObject payload) {
    JsonObject headers = new JsonObject();
    headers.add(ApiKeywords.TASKID, new JsonPrimitive(taskId));
    headers.add(ApiKeywords.EVENT, new JsonPrimitive(WebSocketEventType.TASK_FAILED.getValue()));
    headers.add(ApiKeywords.ERROR_NAME, new JsonPrimitive(String.valueOf(code)));
    headers.add(ApiKeywords.ERROR_MESSAGE, new JsonPrimitive(String.valueOf(code)));
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendBinary(ByteBuffer buffer) {
    wsb.send(buffer);
  }

  private String buildUpMessage(JsonObject headers, JsonObject payload) {
    JsonObject message = new JsonObject();
    message.add(ApiKeywords.HEADER, headers);
    message.add(ApiKeywords.PAYLOAD, payload);
    return message.toString();
  }
}
