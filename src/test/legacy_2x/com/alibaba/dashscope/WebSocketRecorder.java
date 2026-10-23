// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

// Modified from:
// https://github.com/square/okhttp/blob/master/okhttp/src/jvmTest/java/okhttp3/internal/ws/WebSocketRecorder.java
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.alibaba.dashscope.base.FullDuplexServiceParam;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.internal.platform.Platform;
import okhttp3.internal.ws.WebSocketReader;
import okio.ByteString;

public final class WebSocketRecorder extends WebSocketListener {
  private final String name;
  private final BlockingQueue<Object> events = new LinkedBlockingQueue<>();
  private final BlockingQueue<String> textMessages = new LinkedBlockingQueue<>();
  private final BlockingQueue<ByteBuffer> binaryMessages = new LinkedBlockingQueue<>();
  private WebSocketListener delegate;
  private Map<String, List<String>> headers;

  public WebSocketRecorder(String name) {
    this.name = name;
  }

  /** Sets a delegate for handling the next callback to this listener. Cleared after invoked. */
  public void setNextEventDelegate(WebSocketListener delegate) {
    this.delegate = delegate;
  }

  @Override
  public void onOpen(WebSocket webSocket, Response response) {
    Platform.get().log("[WS " + name + "] onOpen", Platform.INFO, null);
    headers = response.request().headers().toMultimap();
    WebSocketListener delegate = this.delegate;
    if (delegate != null) {
      this.delegate = null;
      delegate.onOpen(webSocket, response);
    } else {
      events.add(new Open(webSocket, response));
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, ByteString bytes) {
    Platform.get().log("[WS " + name + "] onMessage", Platform.INFO, null);

    WebSocketListener delegate = this.delegate;
    if (delegate != null) {
      this.delegate = null;
      delegate.onMessage(webSocket, bytes);
    } else {
      Message event = new Message(bytes);
      events.add(event);
    }
    binaryMessages.add(bytes.asByteBuffer());
  }

  @Override
  public void onMessage(WebSocket webSocket, String text) {
    Platform.get().log("[WS " + name + "] onMessage", Platform.INFO, null);

    WebSocketListener delegate = this.delegate;
    if (delegate != null) {
      this.delegate = null;
      delegate.onMessage(webSocket, text);
    } else {
      Message event = new Message(text);
      events.add(event);
    }
    textMessages.add(text);
  }

  @Override
  public void onClosing(WebSocket webSocket, int code, String reason) {
    Platform.get().log("[WS " + name + "] onClosing " + code, Platform.INFO, null);

    WebSocketListener delegate = this.delegate;
    if (delegate != null) {
      this.delegate = null;
      delegate.onClosing(webSocket, code, reason);
    } else {
      events.add(new Closing(code, reason));
    }
  }

  @Override
  public void onClosed(WebSocket webSocket, int code, String reason) {
    Platform.get().log("[WS " + name + "] onClosed " + code, Platform.INFO, null);

    WebSocketListener delegate = this.delegate;
    if (delegate != null) {
      this.delegate = null;
      delegate.onClosed(webSocket, code, reason);
    } else {
      events.add(new Closed(code, reason));
    }
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    Platform.get().log("[WS " + name + "] onFailure", Platform.INFO, t);

    WebSocketListener delegate = this.delegate;
    if (delegate != null) {
      this.delegate = null;
      delegate.onFailure(webSocket, t, response);
    } else {
      events.add(new Failure(t, response));
    }
  }

  public WebSocket assertOpen() {
    Object event = nextEvent();
    if (!(event instanceof Open)) {
      throw new AssertionError("Expected Open but was " + event);
    }
    return ((Open) event).webSocket;
  }

  public void assertFailure(Throwable t) {
    Object event = nextEvent();
    if (!(event instanceof Failure)) {
      throw new AssertionError("Expected Failure but was " + event);
    }
    Failure failure = (Failure) event;
    assertNull(failure.response);
    assertEquals(failure.t, t);
  }

  private Object nextEvent() {
    try {
      Object event = events.poll(10, TimeUnit.SECONDS);
      if (event == null) {
        throw new AssertionError("Timed out waiting for event.");
      }
      return event;
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  public <RequestParam extends HalfDuplexServiceParam> void assertHalfDuplexRequest(
      RequestParam req, String stream) throws InterruptedException {
    String textMessage = textMessages.take();
    JsonObject obj = JsonUtils.parse(textMessage);
    JsonObject header = obj.get("header").getAsJsonObject();
    assertEquals(header.get("action").getAsString(), "run-task");
    assertEquals(header.get("streaming").getAsString(), stream);
    JsonObject payload = obj.get("payload").getAsJsonObject();
    assertEquals(payload.get("model").getAsString(), req.getModel());
    ByteBuffer reqBytes = req.getBinaryData();
    if (reqBytes != null) {
      ByteBuffer binaryMessage = binaryMessages.take();
      assertEquals(binaryMessage, reqBytes.position(0));
    } else {
      assertEquals(payload.get("input").getAsJsonObject(), (JsonObject) req.getInput());
    }
    assertEquals(
        payload.get("parameters").getAsJsonObject(),
        JsonUtils.parametersToJsonObject(req.getParameters()));
  }

  public void assertHeaders(Map<String, String> customHeaders) {
    for (Entry<String, String> entry : customHeaders.entrySet()) {
      assertTrue(headers.containsKey(entry.getKey()));
      assertTrue(headers.get(entry.getKey()).get(0).equals(entry.getValue()));
    }
  }

  public <RequestParam extends HalfDuplexServiceParam> void assertResources(
      JsonObject sendResources) throws InterruptedException {
    String textMessage = textMessages.take();
    JsonObject obj = JsonUtils.parse(textMessage);
    assertEquals(sendResources, obj.get("payload").getAsJsonObject().get(ApiKeywords.RESOURCES));
  }

  public <RequestParam extends FullDuplexServiceParam> void assertFullDuplexRequest(
      RequestParam req, String streamMode) throws InterruptedException {
    String textMessage = textMessages.take();
    JsonObject obj = JsonUtils.parse(textMessage);
    JsonObject header = obj.get("header").getAsJsonObject();
    assertEquals(header.get("action").getAsString(), "run-task");
    assertEquals(header.get("streaming").getAsString(), streamMode);
    JsonObject payload = obj.get("payload").getAsJsonObject();
    assertEquals(payload.get("model").getAsString(), req.getModel());
    Flowable<Object> data = req.getStreamingData();
    if (data != null) {
      data.blockingForEach(
          msg -> {
            if (msg instanceof String) {
              String inputMessage = textMessages.take();
              System.out.println(inputMessage);
              System.out.println(msg);
              JsonObject reqObject = JsonUtils.parse(inputMessage);
              JsonObject reqHeader = reqObject.get("header").getAsJsonObject();
              assertEquals(reqHeader.get("action").getAsString(), "continue-task");
              JsonObject reqPayload = reqObject.get("payload").getAsJsonObject();
              assertEquals(reqPayload.get("input").getAsString(), msg);
            } else if (msg instanceof ByteBuffer) {
              ByteBuffer b = binaryMessages.take();
              assertEquals(((ByteBuffer) msg).position(0), b);
            } else if (msg instanceof byte[]) {
              ByteBuffer b = binaryMessages.take();
              assertEquals(ByteBuffer.wrap((byte[]) msg), b);
            }
          });
    }
  }

  public <RequestParam extends FullDuplexServiceParam> void assertFullDuplexRequestWithResources(
      RequestParam req, String streamMode, JsonObject resources) throws InterruptedException {
    String textMessage = textMessages.take();
    JsonObject obj = JsonUtils.parse(textMessage);
    JsonObject header = obj.get("header").getAsJsonObject();
    assertEquals(header.get("action").getAsString(), "run-task");
    assertEquals(header.get("streaming").getAsString(), streamMode);
    JsonObject payload = obj.get("payload").getAsJsonObject();
    assertEquals(payload.get("model").getAsString(), req.getModel());
    assertEquals(payload.get(ApiKeywords.RESOURCES), resources);
    Flowable<Object> data = req.getStreamingData();
    if (data != null) {
      data.blockingForEach(
          msg -> {
            if (msg instanceof String) {
              String inputMessage = textMessages.take();
              System.out.println(inputMessage);
              System.out.println(msg);
              JsonObject reqObject = JsonUtils.parse(inputMessage);
              JsonObject reqHeader = reqObject.get("header").getAsJsonObject();
              assertEquals(reqHeader.get("action").getAsString(), "continue-task");
              JsonObject reqPayload = reqObject.get("payload").getAsJsonObject();
              assertEquals(reqPayload.get("input").getAsString(), msg);
            } else if (msg instanceof ByteBuffer) {
              ByteBuffer b = binaryMessages.take();
              assertEquals(((ByteBuffer) msg).position(0), b);
            } else if (msg instanceof byte[]) {
              ByteBuffer b = binaryMessages.take();
              assertEquals(ByteBuffer.wrap((byte[]) msg), b);
            }
          });
    }
  }

  /** Expose this recorder as a frame callback and shim in "ping" events. */
  public WebSocketReader.FrameCallback asFrameCallback() {
    return new WebSocketReader.FrameCallback() {
      @Override
      public void onReadMessage(String text) throws IOException {
        onMessage(null, text);
      }

      @Override
      public void onReadMessage(ByteString bytes) throws IOException {
        onMessage(null, bytes);
      }

      @Override
      public void onReadPing(ByteString payload) {
        events.add(new Ping(payload));
      }

      @Override
      public void onReadPong(ByteString payload) {
        events.add(new Pong(payload));
      }

      @Override
      public void onReadClose(int code, String reason) {
        onClosing(null, code, reason);
      }
    };
  }

  static final class Open {
    final WebSocket webSocket;
    final Response response;

    Open(WebSocket webSocket, Response response) {
      this.webSocket = webSocket;
      this.response = response;
    }

    @Override
    public String toString() {
      return "Open[" + response + "]";
    }
  }

  static final class Failure {
    final Throwable t;
    final Response response;
    final String responseBody;

    Failure(Throwable t, Response response) {
      this.t = t;
      this.response = response;
      String responseBody = null;
      if (response != null && response.code() != 101) {
        try {
          responseBody = response.body().string();
        } catch (IOException ignored) {
        }
      }
      this.responseBody = responseBody;
    }

    @Override
    public String toString() {
      if (response == null) {
        return "Failure[" + t + "]";
      }
      return "Failure[" + response + "]";
    }
  }

  static final class Message {
    public final ByteString bytes;
    public final String string;

    public Message(ByteString bytes) {
      this.bytes = bytes;
      this.string = null;
    }

    public Message(String string) {
      this.bytes = null;
      this.string = string;
    }

    @Override
    public String toString() {
      return "Message[" + (bytes != null ? bytes : string) + "]";
    }

    @Override
    public int hashCode() {
      return (bytes != null ? bytes : string).hashCode();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Message
          && Objects.equals(((Message) other).bytes, bytes)
          && Objects.equals(((Message) other).string, string);
    }
  }

  static final class Ping {
    public final ByteString payload;

    public Ping(ByteString payload) {
      this.payload = payload;
    }

    @Override
    public String toString() {
      return "Ping[" + payload + "]";
    }

    @Override
    public int hashCode() {
      return payload.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Ping && ((Ping) other).payload.equals(payload);
    }
  }

  static final class Pong {
    public final ByteString payload;

    public Pong(ByteString payload) {
      this.payload = payload;
    }

    @Override
    public String toString() {
      return "Pong[" + payload + "]";
    }

    @Override
    public int hashCode() {
      return payload.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Pong && ((Pong) other).payload.equals(payload);
    }
  }

  static final class Closing {
    public final int code;
    public final String reason;

    Closing(int code, String reason) {
      this.code = code;
      this.reason = reason;
    }

    @Override
    public String toString() {
      return "Closing[" + code + " " + reason + "]";
    }

    @Override
    public int hashCode() {
      return code * 37 + reason.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Closing
          && ((Closing) other).code == code
          && ((Closing) other).reason.equals(reason);
    }
  }

  static final class Closed {
    public final int code;
    public final String reason;

    Closed(int code, String reason) {
      this.code = code;
      this.reason = reason;
    }

    @Override
    public String toString() {
      return "Closed[" + code + " " + reason + "]";
    }

    @Override
    public int hashCode() {
      return code * 37 + reason.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Closed
          && ((Closed) other).code == code
          && ((Closed) other).reason.equals(reason);
    }
  }
}
