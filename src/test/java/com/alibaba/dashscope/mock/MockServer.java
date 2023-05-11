package com.alibaba.dashscope.mock;

import com.alibaba.dashscope.conversation.ConversationMessagePayload;
import com.alibaba.dashscope.conversation.EventType;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsMessageContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
public class MockServer {

  private static final Set<SseClient> sseClients = Collections.synchronizedSet(new HashSet<>());

  private Javalin app;

  private int code;

  private String apiKey;

  public void tearDown() {
    app.close();
  }

  public void start(int code, String apiKey) {
    this.code = code;
    this.apiKey = apiKey;
    app = Javalin.create().start(8080);

    app.post("/api/v1/services/aigc/generation", this::postEcho);
    app.post("/api/v1/services/aigc/text-generation/generation", this::postEcho);
    app.ws(
        "/api-ws/v1/inference",
        ws -> {
          ws.onConnect(ctx -> websocketHandler(ctx, false, false));
          ws.onMessage(ctx -> websocketMessageHandler(ctx, false, false));
          ws.onClose(ctx -> {});
          ws.onError(ctx -> {});
        });
  }

  public void postEcho(Context ctx) {
    String apiKey = ctx.header("Authorization");
    apiKey = apiKey == null ? "" : apiKey;
    if (!apiKey.contains(this.apiKey)) {
      ctx.res.setStatus(HttpStatus.SC_UNAUTHORIZED);
    } else if ("text/event-stream".equals(ctx.header("Accept"))) {
      // Start an async context
      javax.servlet.AsyncContext asyncContext = ctx.req.startAsync();

      // Add a listener to remove the client when the connection is closed
      asyncContext.addListener(
          new javax.servlet.AsyncListener() {
            @Override
            public void onComplete(javax.servlet.AsyncEvent asyncEvent) {}

            @Override
            public void onError(javax.servlet.AsyncEvent asyncEvent) {}

            @Override
            public void onStartAsync(javax.servlet.AsyncEvent asyncEvent) {}

            @Override
            public void onTimeout(javax.servlet.AsyncEvent asyncEvent) {
              SseClient sseClient =
                  (SseClient) asyncEvent.getAsyncContext().getRequest().getAttribute("sseClient");
              sseClients.remove(sseClient);
            }
          });

      SseClient sseClient = new SseClient(ctx);
      sseClients.add(sseClient);
      ctx.req.setAttribute("sseClient", sseClient);
      mockSse(sseClient, ctx.body());

      asyncContext.complete();
    } else {
      JsonObject body = new Gson().fromJson(ctx.body(), JsonObject.class);

      ConversationMessagePayload payload = new ConversationMessagePayload();
      JsonObject output = new JsonObject();
      output.add(
          ApiKeywords.TEXT,
          new JsonPrimitive(
              body.get(ApiKeywords.INPUT).getAsJsonObject().get(ApiKeywords.PROMPT).getAsString()));
      payload.setOutput(output);
      JsonObject usage = new JsonObject();
      usage.add(ApiKeywords.INPUT_TOKENS, new JsonPrimitive(100));
      usage.add(ApiKeywords.OUTPUT_TOKENS, new JsonPrimitive(200));
      payload.setUsage(usage);
      payload.setRequestId(ctx.req.getParameter(ApiKeywords.REQUEST_ID));
      ctx.contentType("application/json");
      ctx.result(JsonUtils.toJson(payload));
    }
  }

  public void websocketHandler(WsConnectContext ctx, boolean inFlag, boolean outFlag) {
    log.info(ctx.toString(), inFlag, outFlag);
    if (!this.apiKey.equals(ctx.header("Authorization"))) {
      ctx.closeSession(HttpStatus.SC_UNAUTHORIZED, "No apiKey");
    }
  }

  public void websocketMessageHandler(WsMessageContext ctx, boolean inFlag, boolean outFlag) {
    JsonObject req = new Gson().fromJson(ctx.message(), JsonObject.class);
    JsonObject header = req.getAsJsonObject(ApiKeywords.HEADER);
    if (header.get(ApiKeywords.ACTION).getAsString().equals(EventType.RUN_TASK.getValue())) {
      String taskId = header.get(ApiKeywords.TASKID).getAsString();
      String streamingMode = header.get(ApiKeywords.STREAMING).getAsString();
      JsonObject payload = req.getAsJsonObject(ApiKeywords.PAYLOAD);

      WebSocketTaskProcessor wsc = new WebSocketTaskProcessor(ctx, taskId, streamingMode, code);

      wsc.asyncCall(payload.toString());
    }
  }

  public void mockSse(SseClient client, String prompt) {
    client.ctx.contentType("application/json");
    if (code == 0) {
      for (int idx = 0; idx < 3; idx++) {
        ConversationMessagePayload payload = new ConversationMessagePayload();
        JsonObject output = new JsonObject();
        output.add(ApiKeywords.TEXT, new JsonPrimitive(prompt));
        payload.setOutput(output);
        JsonObject usage = new JsonObject();
        usage.add(ApiKeywords.INPUT_TOKENS, new JsonPrimitive(100));
        usage.add(ApiKeywords.OUTPUT_TOKENS, new JsonPrimitive(200));
        payload.setUsage(usage);
        payload.setRequestId(client.ctx.req.getParameter(ApiKeywords.REQUEST_ID));
        String responseStr = JsonUtils.toJson(payload);
        try {
          client.sendEvent(Integer.toString(idx), responseStr);
          TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      try {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(ApiKeywords.CODE, new JsonPrimitive(String.valueOf(code)));
        jsonObject.add(ApiKeywords.MESSAGE, new JsonPrimitive(String.valueOf(code)));
        client.sendEvent(ApiKeywords.ERROR, jsonObject.toString());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
