package com.dashscope.mock;

import static com.dashscope.utils.ApiKeywords.*;

import com.dashscope.common.StreamingMode;
import com.dashscope.conversation.ConversationMessagePayload;
import com.dashscope.conversation.EventType;
import com.dashscope.utils.JsonUtils;
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
            output.add(TEXT, new JsonPrimitive(prompt));
            payload.setOutput(output);
            JsonObject usage = new JsonObject();
            usage.add(INPUT_TOKENS, new JsonPrimitive(100));
            usage.add(OUTPUT_TOKENS, new JsonPrimitive(200));
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
    headers.add(TASKID, new JsonPrimitive(taskId));
    headers.add(EVENT, new JsonPrimitive(EventType.TASK_STARTED.getValue()));
    JsonObject payload = new JsonObject();
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendResultGenerated(JsonObject payload) {
    JsonObject headers = new JsonObject();
    headers.add(TASKID, new JsonPrimitive(taskId));
    headers.add(EVENT, new JsonPrimitive(EventType.RESULT_GENERATED.getValue()));
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendTaskFinished(JsonObject payload) {
    JsonObject headers = new JsonObject();
    headers.add(TASKID, new JsonPrimitive(taskId));
    headers.add(EVENT, new JsonPrimitive(EventType.TASK_FINISHED.getValue()));
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  public void sendTaskFailed(JsonObject payload) {
    JsonObject headers = new JsonObject();
    headers.add(TASKID, new JsonPrimitive(taskId));
    headers.add(EVENT, new JsonPrimitive(EventType.TASK_FAILED.getValue()));
    headers.add(ERROR_NAME, new JsonPrimitive(String.valueOf(code)));
    headers.add(ERROR_MESSAGE, new JsonPrimitive(String.valueOf(code)));
    String message = buildUpMessage(headers, payload);
    ws.send(message);
  }

  private String buildUpMessage(JsonObject headers, JsonObject payload) {
    JsonObject message = new JsonObject();
    message.add(HEADER, headers);
    message.add(PAYLOAD, payload);
    return message.toString();
  }
}
