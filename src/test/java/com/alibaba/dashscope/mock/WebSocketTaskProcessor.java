// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.mock;

import com.alibaba.dashscope.common.StreamingMode;
import com.alibaba.dashscope.conversation.ConversationMessagePayload;
import com.alibaba.dashscope.conversation.EventType;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.javalin.websocket.WsMessageContext;
import java.util.concurrent.CompletableFuture;

public class WebSocketTaskProcessor {
  private final WsMessageContext ws;
  private final String taskId;

  private final StreamingMode streamingMode;

  private final int code;

  public WebSocketTaskProcessor(
      WsMessageContext ws, String taskId, String streamingMode, int code) {
    this.ws = ws;
    this.taskId = taskId;
    this.streamingMode = StreamingMode.of(streamingMode);
    this.code = code;
  }

  public void asyncCall(String prompt) {
    CompletableFuture.runAsync(
        () -> {
          try {
            sendStartEvent();
            ConversationMessagePayload payload = new ConversationMessagePayload();
            JsonObject output = new JsonObject();
            output.add(ApiKeywords.TEXT, new JsonPrimitive(prompt));
            payload.setOutput(output);
            JsonObject usage = new JsonObject();
            usage.add(ApiKeywords.INPUT_TOKENS, new JsonPrimitive(100));
            usage.add(ApiKeywords.OUTPUT_TOKENS, new JsonPrimitive(200));
            payload.setUsage(usage);
            JsonObject payloadJson = JsonUtils.toJsonObject(payload);
            switch (streamingMode) {
              case NONE:
                {
                  if (code == 0) {
                    sendTaskFinished(payloadJson);
                  } else {
                    sendTaskFailed(payloadJson);
                  }
                  break;
                }
              case OUT:
                if (code == 0) {
                  sendResultGenerated(payloadJson);
                  sendTaskFinished(payloadJson);
                } else {
                  sendTaskFailed(payloadJson);
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
    headers.add(ApiKeywords.EVENT, new JsonPrimitive(EventType.TASK_STARTED.getValue()));
    JsonObject payload = new JsonObject();
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendResultGenerated(JsonObject payload) {
    JsonObject headers = new JsonObject();
    headers.add(ApiKeywords.TASKID, new JsonPrimitive(taskId));
    headers.add(ApiKeywords.EVENT, new JsonPrimitive(EventType.RESULT_GENERATED.getValue()));
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendTaskFinished(JsonObject payload) {
    JsonObject headers = new JsonObject();
    headers.add(ApiKeywords.TASKID, new JsonPrimitive(taskId));
    headers.add(ApiKeywords.EVENT, new JsonPrimitive(EventType.TASK_FINISHED.getValue()));
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendTaskFailed(JsonObject payload) {
    JsonObject headers = new JsonObject();
    headers.add(ApiKeywords.TASKID, new JsonPrimitive(taskId));
    headers.add(ApiKeywords.EVENT, new JsonPrimitive(EventType.TASK_FAILED.getValue()));
    headers.add(ApiKeywords.ERROR_NAME, new JsonPrimitive(String.valueOf(code)));
    headers.add(ApiKeywords.ERROR_MESSAGE, new JsonPrimitive(String.valueOf(code)));
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  private String buildUpMessage(JsonObject headers, JsonObject payload) {
    JsonObject message = new JsonObject();
    message.add(ApiKeywords.HEADER, headers);
    message.add(ApiKeywords.PAYLOAD, payload);
    return message.toString();
  }
}
