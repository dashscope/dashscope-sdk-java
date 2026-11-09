// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import static com.alibaba.dashscope.audio.live.QwenLiveConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.dashscope.audio.live.TestQwenLiveConversation.FakeFactory;
import com.alibaba.dashscope.audio.live.TestQwenLiveConversation.RecordingCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TestQwenLiveProtocol {
  private MockWebServer server;
  private QwenLiveConnectionParam param;
  private RecordingCallback callback;
  private QwenLiveConversation conversation;
  private final BlockingQueue<JsonObject> sent = new LinkedBlockingQueue<>();
  private final BlockingQueue<WebSocket> serverSocket = new LinkedBlockingQueue<>();

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    param =
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .apikey("test-key")
            .url(server.url("/realtime").toString())
            .build();
    callback = new RecordingCallback();
    server.enqueue(
        new MockResponse()
            .withWebSocketUpgrade(
                new WebSocketListener() {
                  @Override
                  public void onOpen(WebSocket socket, Response response) {
                    serverSocket.add(socket);
                  }

                  @Override
                  public void onMessage(WebSocket socket, String text) {
                    sent.add(JsonUtils.parse(text));
                  }

                  @Override
                  public void onClosing(WebSocket socket, int code, String reason) {
                    socket.close(code, reason);
                  }
                }));
  }

  @AfterEach
  void tearDown() throws Exception {
    if (conversation != null) conversation.cancel();
    server.shutdown();
  }

  private void connect() throws Exception {
    conversation = new QwenLiveConversation(param, callback);
    conversation.connect();
  }

  private String verify(Supplier<String> send, String expected) throws Exception {
    String id = send.get();
    JsonObject actual = sent.poll(5, TimeUnit.SECONDS);
    assertNotNull(actual);
    assertEquals(id, actual.remove(FIELD_EVENT_ID).getAsString());
    assertTrue(id.matches("event_[0-9a-f]{32}"));
    assertEquals(JsonUtils.parse(expected), actual);
    return id;
  }

  @Test
  void everyClientCommandHasExactPayloadAndUniqueEventId() throws Exception {
    connect();
    Set<String> ids = new HashSet<>();
    QwenLiveSessionConfig config =
        QwenLiveSessionConfig.builder()
            .model("front")
            .delegation(QwenLiveDelegation.builder().type(QwenLiveDelegationType.CLIENT).build())
            .build();
    ids.add(
        verify(
            () -> conversation.updateSession(config),
            "{\"type\":\"session.update\",\"session\":{\"model\":\"front\",\"delegation\":{\"type\":\"client\"}}}"));
    ids.add(
        verify(
            () -> conversation.appendAudio("AAAAAA=="),
            "{\"type\":\"session.input_audio.append\",\"audio\":\"AAAAAA==\"}"));
    ids.add(verify(() -> conversation.muteInputAudio(), "{\"type\":\"session.input_audio.mute\"}"));
    ids.add(
        verify(() -> conversation.unmuteInputAudio(), "{\"type\":\"session.input_audio.unmute\"}"));
    ids.add(
        verify(
            () -> conversation.appendInstruction("call_xxx", "提示词"),
            "{\"type\":\"session.instruction.append\",\"delegation_id\":\"call_xxx\",\"content\":\"提示词\"}"));
    ids.add(
        verify(
            () -> conversation.appendThinking("call_xxx", "思考"),
            "{\"type\":\"session.thinking.append\",\"delegation_id\":\"call_xxx\",\"content\":\"思考\"}"));
    ids.add(
        verify(
            () -> conversation.appendCommentary("call_xxx", "结果"),
            "{\"type\":\"session.commentary.append\",\"delegation_id\":\"call_xxx\",\"content\":\"结果\"}"));
    JsonObject item =
        JsonUtils.parse(
            "{\"type\":\"message\",\"role\":\"user\",\"content\":[{\"type\":\"input_text\",\"text\":\"你好\"}]}");
    JsonObject original = item.deepCopy();
    ids.add(
        verify(
            () -> conversation.createResponseItem(item),
            "{\"type\":\"response.item.create\",\"item\":" + item + "}"));
    assertEquals(original, item);
    ids.add(verify(() -> conversation.createResponse(), "{\"type\":\"response.create\"}"));
    ids.add(
        verify(
            () ->
                conversation.sendRaw(
                    "{\"type\":\"future.event\",\"event_id\":\"old\",\"data\":{\"value\":\"中文\",\"nullable\":null}}"),
            "{\"type\":\"future.event\",\"data\":{\"value\":\"中文\",\"nullable\":null}}"));
    assertEquals(10, ids.size());
  }

  @Test
  void emptyContentAndRawAudioAreAllowed() throws Exception {
    connect();
    verify(
        () -> conversation.appendCommentary("call", ""),
        "{\"type\":\"session.commentary.append\",\"delegation_id\":\"call\",\"content\":\"\"}");
    verify(
        () ->
            conversation.sendRaw(
                "{\"type\":\"session.input_audio.append\",\"audio\":\"AAAAAA==\"}"),
        "{\"type\":\"session.input_audio.append\",\"audio\":\"AAAAAA==\"}");
  }

  @Test
  void appendAudioBytesFullArrayHasExactPayload() throws Exception {
    connect();
    byte[] audio = {0, 1, 2, (byte) 0xfe, (byte) 0xff};
    verify(
        () -> conversation.appendAudioBytes(audio),
        "{\"type\":\"session.input_audio.append\",\"audio\":\"AAEC/v8=\"}");
    assertArrayEquals(new byte[] {0, 1, 2, (byte) 0xfe, (byte) 0xff}, audio);
  }

  @Test
  void appendAudioBytesSliceEncodesOnlySelectedBytes() throws Exception {
    connect();
    byte[] audio = {85, 0, 1, 2, (byte) 0xfe, (byte) 0xff, 102};
    verify(
        () -> conversation.appendAudioBytes(audio, 1, 5),
        "{\"type\":\"session.input_audio.append\",\"audio\":\"AAEC/v8=\"}");
    verify(
        () -> conversation.appendAudioBytes(audio, 4, 3),
        "{\"type\":\"session.input_audio.append\",\"audio\":\"/v9m\"}");
    assertArrayEquals(new byte[] {85, 0, 1, 2, (byte) 0xfe, (byte) 0xff, 102}, audio);
  }

  @Test
  void appendAudioBytesPayloadSurvivesArrayReuse() throws Exception {
    connect();
    byte[] audio = {0, 1, 2, (byte) 0xfe, (byte) 0xff};
    verify(
        () -> {
          String id = conversation.appendAudioBytes(audio);
          Arrays.fill(audio, (byte) 0);
          return id;
        },
        "{\"type\":\"session.input_audio.append\",\"audio\":\"AAEC/v8=\"}");
    byte[] slice = {85, 0, 1, 2, (byte) 0xfe, (byte) 0xff, 102};
    verify(
        () -> {
          String id = conversation.appendAudioBytes(slice, 1, 5);
          Arrays.fill(slice, (byte) 0);
          return id;
        },
        "{\"type\":\"session.input_audio.append\",\"audio\":\"AAEC/v8=\"}");
  }

  @Test
  void appendAudioBytesRejectsInvalidInputBeforeSend() throws Exception {
    connect();
    assertThrows(IllegalArgumentException.class, () -> conversation.appendAudioBytes(null));
    assertThrows(IllegalArgumentException.class, () -> conversation.appendAudioBytes(null, 0, 0));
    byte[] audio = {0, 1, 2};
    for (int[] range :
        new int[][] {
          {-1, 0},
          {0, -1},
          {-1, -1},
          {4, 0},
          {3, 1},
          {2, 2},
          {0, 4},
          {Integer.MAX_VALUE, 0},
          {Integer.MAX_VALUE, 1},
          {1, Integer.MAX_VALUE},
          {0, Integer.MAX_VALUE},
          {Integer.MAX_VALUE, Integer.MAX_VALUE}
        }) {
      assertThrows(
          IllegalArgumentException.class,
          () -> conversation.appendAudioBytes(audio, range[0], range[1]),
          "range: " + Arrays.toString(range));
    }
    assertThrows(
        IllegalArgumentException.class, () -> conversation.appendAudioBytes(new byte[0], 1, 0));
    verify(
        () -> conversation.appendAudio("AAA="),
        "{\"type\":\"session.input_audio.append\",\"audio\":\"AAA=\"}");
    assertNull(sent.poll(200, TimeUnit.MILLISECONDS));
  }

  @Test
  void appendAudioBytesDisconnectedNeverReturnsSuccessId() throws Exception {
    conversation = new QwenLiveConversation(param, callback);
    byte[] audio = {0, 1, 2};
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(audio));
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(audio, 1, 2));
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(new byte[0]));
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(audio, 3, 0));
    conversation.connect();
    conversation.close();
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(audio));
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(audio, 1, 2));
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(new byte[0]));
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(audio, 3, 0));
    assertNull(sent.poll(200, TimeUnit.MILLISECONDS));
  }

  @Test
  void appendAudioBytesFailedEnqueueNeverReturnsSuccessId() throws Exception {
    FakeFactory factory = new FakeFactory();
    factory.socket.sendResult = false;
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    byte[] audio = {0, 1, 2};
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(audio));
    assertThrows(IllegalStateException.class, () -> conversation.appendAudioBytes(audio, 1, 2));
    assertTrue(factory.socket.sent.isEmpty());
  }

  @Test
  void appendAudioBytesAllowsEmptyArraysAndValidEmptySlices() throws Exception {
    connect();
    String expected = "{\"type\":\"session.input_audio.append\",\"audio\":\"\"}";
    verify(() -> conversation.appendAudioBytes(new byte[0]), expected);
    verify(() -> conversation.appendAudioBytes(new byte[0], 0, 0), expected);
    byte[] audio = {0, 1, 2};
    verify(() -> conversation.appendAudioBytes(audio, 0, 0), expected);
    verify(() -> conversation.appendAudioBytes(audio, 1, 0), expected);
    verify(() -> conversation.appendAudioBytes(audio, audio.length, 0), expected);
    verify(() -> conversation.appendAudio(""), expected);
  }

  @Test
  void invalidPayloadIsRejectedBeforeSend() throws Exception {
    connect();
    assertThrows(IllegalArgumentException.class, () -> conversation.updateSession(null));
    assertThrows(IllegalArgumentException.class, () -> conversation.appendAudio(null));
    assertThrows(IllegalArgumentException.class, () -> conversation.createResponseItem(null));
    assertThrows(
        IllegalArgumentException.class, () -> conversation.appendInstruction("call", null));
    assertThrows(IllegalArgumentException.class, () -> conversation.appendThinking("call", null));
    assertThrows(IllegalArgumentException.class, () -> conversation.appendCommentary("call", null));
    for (String id : new String[] {null, "", " "}) {
      assertThrows(IllegalArgumentException.class, () -> conversation.appendInstruction(id, "x"));
      assertThrows(IllegalArgumentException.class, () -> conversation.appendThinking(id, "x"));
      assertThrows(IllegalArgumentException.class, () -> conversation.appendCommentary(id, "x"));
    }
    for (String raw :
        new String[] {
          null,
          "",
          "{",
          "[]",
          "null",
          "{}",
          "{\"type\":1}",
          "{\"type\":null}",
          "{\"type\":\" \"}",
          "{type:'legacy'}",
          "{\"type\":\"x\"} {}"
        }) {
      assertThrows(IllegalArgumentException.class, () -> conversation.sendRaw(raw), "raw: " + raw);
    }
    assertTrue(sent.isEmpty());
  }

  @Test
  void disconnectedSendNeverReturnsSuccessId() throws Exception {
    conversation = new QwenLiveConversation(param, callback);
    assertThrows(IllegalStateException.class, () -> conversation.appendAudio("AAA="));
    assertThrows(IllegalStateException.class, () -> conversation.sendRaw("{\"type\":\"x\"}"));
    conversation.connect();
    conversation.close();
    assertThrows(IllegalStateException.class, conversation::createResponse);
  }

  @Test
  void failedSocketEnqueueNeverReturnsSuccessId() throws Exception {
    FakeFactory factory = new FakeFactory();
    factory.socket.sendResult = false;
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    assertThrows(IllegalStateException.class, () -> conversation.appendAudio("AAA="));
    assertThrows(IllegalStateException.class, conversation::createResponse);
  }

  @Test
  void onOpenCanSendBeforeFactoryReturns() throws Exception {
    FakeFactory factory = new FakeFactory();
    AtomicReference<String> id = new AtomicReference<>();
    callback =
        new RecordingCallback() {
          @Override
          public void onOpen() {
            id.set(conversation.appendAudio("AAA="));
          }
        };
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    assertNotNull(id.get());
    assertEquals(
        id.get(),
        JsonUtils.parse(factory.socket.sent.poll(5, TimeUnit.SECONDS))
            .get("event_id")
            .getAsString());
  }

  static Stream<String> serverEvents() {
    return Stream.of(
        EVENT_ERROR,
        EVENT_SESSION_CREATED,
        EVENT_SESSION_UPDATED,
        EVENT_SESSION_INPUT_AUDIO_MUTED,
        EVENT_SESSION_INPUT_AUDIO_UNMUTED,
        EVENT_SESSION_OUTPUT_AUDIO_DELTA,
        EVENT_SESSION_INPUT_TRANSCRIPT_DELTA,
        EVENT_SESSION_OUTPUT_TRANSCRIPT_DELTA,
        EVENT_RESPONSE_EVENT,
        EVENT_SESSION_USAGE_UPDATED,
        EVENT_SESSION_CLOSED,
        EVENT_SESSION_DELEGATION_CREATED,
        EVENT_SESSION_INSTRUCTIONS_APPENDED,
        EVENT_SESSION_THINKING_APPENDED,
        EVENT_SESSION_COMMENTARY_APPENDED,
        "future.server.event");
  }

  @ParameterizedTest
  @MethodSource("serverEvents")
  void allServerEventsArePassedThroughWithoutLosingFields(String type) throws Exception {
    connect();
    JsonObject event =
        JsonUtils.parse(
            "{\"event_id\":\"server-id\",\"client_event_id\":\"client-id\","
                + "\"delegation_id\":\"call\",\"offset_ms\":3600,\"start_ms\":1000,\"end_ms\":1400,"
                + "\"arguments\":\"{\\\"city\\\":\\\"杭州\\\"}\",\"delta\":\"中文\","
                + "\"session\":{\"id\":\"sess-1\",\"status\":\"active\"},\"reason\":\"expired\","
                + "\"error\":{\"type\":\"server_error\",\"code\":\"test\"},"
                + "\"usage\":{\"seconds\":32.5},\"context_window\":{\"usage_ratio\":0.12},"
                + "\"event\":{\"type\":\"response.output_text.delta\",\"delta\":\"结果\",\"logprobs\":[]}}");
    event.addProperty("type", type);
    WebSocket socket = serverSocket.poll(5, TimeUnit.SECONDS);
    assertNotNull(socket);
    assertTrue(socket.send(event.toString()));
    JsonObject actual = callback.events.poll(5, TimeUnit.SECONDS);
    assertEquals(event, actual);
    if (EVENT_SESSION_CREATED.equals(type)) assertEquals("sess-1", conversation.getSessionId());
    if (EVENT_SESSION_CLOSED.equals(type)) {
      assertEquals("expired", conversation.getCloseReason());
      assertTrue(conversation.awaitSessionClosed(0));
      assertThrows(IllegalStateException.class, conversation::createResponse);
    } else {
      conversation.checkStatus();
      assertFalse(conversation.awaitSessionClosed(0));
    }
  }

  @Test
  void stateIsUpdatedBeforeCallbackEvenWhenCallbackThrows() throws Exception {
    FakeFactory factory = new FakeFactory();
    callback =
        new RecordingCallback() {
          @Override
          public void onEvent(JsonObject event) {
            if (EVENT_SESSION_CREATED.equals(event.get("type").getAsString())) {
              assertEquals("sess-2", conversation.getSessionId());
            } else {
              assertEquals("connection_lost", conversation.getCloseReason());
              assertThrows(IllegalStateException.class, conversation::checkStatus);
            }
            throw new IllegalStateException("event-callback");
          }
        };
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    factory.listener.onMessage(
        factory.socket, "{\"type\":\"session.created\",\"session\":{\"id\":\"sess-2\"}}");
    assertNotNull(callback.errors.poll(5, TimeUnit.SECONDS));
    factory.listener.onMessage(
        factory.socket, "{\"type\":\"session.closed\",\"reason\":\"connection_lost\"}");
    assertNotNull(callback.errors.poll(5, TimeUnit.SECONDS));
    assertTrue(conversation.awaitSessionClosed(0));
    conversation.close();
    assertTrue(factory.socket.cancelCalls.get() > 0);
  }

  @Test
  void malformedMessagesReportErrorButLeaveConnectionUsable() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    for (String bad : new String[] {"{", "[]", "null", "{type:'bad'}"}) {
      factory.listener.onMessage(factory.socket, bad);
      assertNotNull(callback.errors.poll(5, TimeUnit.SECONDS));
      conversation.checkStatus();
    }
    assertTrue(callback.events.isEmpty());
  }

  @Test
  void missingOrNonStringTypeDoesNotInventState() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    for (String json :
        new String[] {
          "{}",
          "{\"type\":null}",
          "{\"type\":1}",
          "{\"type\":\"session.created\",\"session\":null}",
          "{\"type\":\"session.created\",\"session\":{}}"
        }) {
      factory.listener.onMessage(factory.socket, json);
      assertEquals(JsonUtils.parse(json), callback.events.poll(5, TimeUnit.SECONDS));
      conversation.checkStatus();
      assertNull(conversation.getSessionId());
    }
    assertTrue(callback.errors.isEmpty());
  }

  @Test
  void sessionClosedUnblocksAnAlreadyWaitingThread() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<Boolean> result = executor.submit(() -> conversation.awaitSessionClosed(5));
      factory.listener.onMessage(
          factory.socket, "{\"type\":\"session.closed\",\"reason\":\"remote_hangup\"}");
      assertTrue(result.get(5, TimeUnit.SECONDS));
      assertTrue(conversation.awaitSessionClosed(0));
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void serverErrorReachesUnifiedErrorCallbackAndKeepsOriginalEvent() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    JsonObject event =
        JsonUtils.parse(
            "{\"type\":\"error\",\"event_id\":\"server-event\",\"client_event_id\":\"sent-event\","
                + "\"request_id\":\"request-1\",\"error\":{\"type\":\"invalid_request_error\","
                + "\"code\":\"invalid_format\",\"message\":\"Use pcm\",\"param\":\"input_audio_format\"}}");
    factory.listener.onMessage(factory.socket, event.toString());

    Throwable failure = callback.errors.poll(1, TimeUnit.SECONDS);
    assertInstanceOf(ApiException.class, failure);
    ApiException apiFailure = (ApiException) failure;
    assertEquals("invalid_format", apiFailure.getStatus().getCode());
    assertEquals("Use pcm", apiFailure.getStatus().getMessage());
    assertEquals("request-1", apiFailure.getStatus().getRequestId());
    assertEquals(event, callback.events.poll(1, TimeUnit.SECONDS));
    conversation.checkStatus();
  }

  @Test
  void throwingErrorCallbackDoesNotSuppressOriginalServerEvent() throws Exception {
    FakeFactory factory = new FakeFactory();
    callback =
        new RecordingCallback() {
          @Override
          public void onError(Throwable failure) {
            errors.add(failure);
            throw new IllegalStateException("error-handler-failed");
          }
        };
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    String event = "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\"}}";
    factory.listener.onMessage(factory.socket, event);
    assertInstanceOf(ApiException.class, callback.errors.poll(1, TimeUnit.SECONDS));
    assertEquals(JsonUtils.parse(event), callback.events.poll(1, TimeUnit.SECONDS));
    conversation.checkStatus();
  }

  @Test
  void invalidRequestErrorDoesNotTerminateSession() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    for (String errorType : new String[] {"invalid_request_error", "server_error"}) {
      String json = "{\"type\":\"error\",\"error\":{\"type\":\"" + errorType + "\"}}";
      factory.listener.onMessage(factory.socket, json);
      assertEquals(JsonUtils.parse(json), callback.events.poll(5, TimeUnit.SECONDS));
      Throwable failure = callback.errors.poll(1, TimeUnit.SECONDS);
      assertInstanceOf(ApiException.class, failure);
      assertEquals(errorType, ((ApiException) failure).getStatus().getCode());
      conversation.checkStatus();
    }
  }

  @ParameterizedTest
  @MethodSource("incompleteServerErrors")
  void incompleteServerErrorStillReachesBothCallbacks(String json) throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    factory.listener.onMessage(factory.socket, json);
    Throwable failure = callback.errors.poll(1, TimeUnit.SECONDS);
    assertInstanceOf(ApiException.class, failure);
    assertNotNull(((ApiException) failure).getStatus().getMessage());
    assertEquals(JsonUtils.parse(json), callback.events.poll(1, TimeUnit.SECONDS));
    conversation.checkStatus();
  }

  static Stream<String> incompleteServerErrors() {
    return Stream.of(
        "{\"type\":\"error\"}",
        "{\"type\":\"error\",\"error\":null}",
        "{\"type\":\"error\",\"error\":\"unexpected\"}",
        "{\"type\":\"error\",\"error\":{\"code\":[],\"message\":{}}}");
  }
}
