// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizerV2;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestTtsV2SpeechSynthesizerV2 {
  private static ArrayList<Byte> audioBuffer;
  private static ResultCallback<SpeechSynthesisResult> callback =
      new ResultCallback<SpeechSynthesisResult>() {
        @Override
        public void onEvent(SpeechSynthesisResult message) {
          System.out.println("onEvent:" + message);
          if (message.getAudioFrame() != null) {
            for (byte b : message.getAudioFrame().array()) {
              audioBuffer.add(b);
            }
          }
        }

        @Override
        public void onComplete() {
          //            System.out.println("onComplete");
        }

        @Override
        public void onError(Exception e) {}
      };
  private static MockWebServer mockServer;

  @BeforeAll
  public static void before() throws IOException {
    audioBuffer = new ArrayList<>();
    mockServer = new MockWebServer();
    mockServer.start();
    MockResponse response =
        new MockResponse()
            .withWebSocketUpgrade(
                new WebSocketListener() {
                  String task_id = "";

                  @Override
                  public void onOpen(WebSocket webSocket, Response response) {
                    System.out.println("Mock Server onOpen");
                    System.out.println(
                        "Mock Server request header:" + response.request().headers());
                    System.out.println("Mock Server response header:" + response.headers());
                    System.out.println("Mock Server response:" + response);
                  }

                  @Override
                  public void onMessage(WebSocket webSocket, String string) {
                    System.out.println("mock server recv: " + string);
                    JsonObject req = JsonUtils.parse(string);
                    if (task_id == "") {
                      task_id = req.get("header").getAsJsonObject().get("task_id").getAsString();
                    }
                    if (string.contains("run-task")) {
                      try {
                        Thread.sleep(100);
                      } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                      }
                      webSocket.send(
                          "{'header': {'task_id': '"
                              + task_id
                              + "', 'event': 'task-started', 'attributes': {}}, 'payload': {}}");
                    } else if (string.contains("finish-task")) {
                      try {
                        Thread.sleep(100);
                      } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                      }
                      webSocket.send(
                          "{'header': {'task_id': '"
                              + task_id
                              + "', 'event': 'task-finished', 'attributes': {}}, 'payload': {'output': None, 'usage': {'characters': 7}}}");
                      webSocket.close(1000, "close by server");
                    } else if (string.contains("continue-task")) {
                      try {
                        Thread.sleep(100);
                      } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                      }
                      byte[] binary = new byte[] {0x01, 0x01, 0x01};
                      webSocket.send(new ByteString(binary));
                    }
                  }
                });
    mockServer.enqueue(response);
  }

  @AfterAll
  public static void after() throws IOException {
    System.out.println("Mock Server is closed");
    mockServer.close();
  }

  @Test
  public void testStreamingCall() {
    System.out.println("############ Start Test Streaming Call ############");
    int port = mockServer.getPort();
    Constants.baseWebsocketApiUrl = String.format("http://127.0.0.1:%s", port);

    // 获取 URL
    String url = mockServer.url("/binary").toString();

    // 在真实世界中，你会在这里做 HTTP 请求，并得到响应
    System.out.println("Mock Server is running at: " + url);
    SpeechSynthesisParam param =
        SpeechSynthesisParam.builder()
            .apiKey("1234")
            .model("cosyvoice-v1")
            .voice("longxiaochun")
            .format(SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS)
            .build();
    SpeechSynthesizerV2 synthesizer = new SpeechSynthesizerV2(param, callback);
    synthesizer.setStartedTimeout(1000);
    synthesizer.setFirstAudioTimeout(2000);
    for (int i = 0; i < 3; i++) {
      synthesizer.streamingCall("今天天气怎么样？");
    }
    try {
      synthesizer.streamingComplete();
      synthesizer.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertEquals(audioBuffer.size(), 9);
    for (int i = 0; i < 9; i++) {
      assertEquals((byte) audioBuffer.get(i), (byte) 0x01);
    }
    System.out.println("############ Start Test Streaming Call Done ############");
  }

  @Test
  public void testStreamingFlushMessageFormat() throws IOException {
    List<String> received = Collections.synchronizedList(new ArrayList<>());
    MockWebServer server = new MockWebServer();
    server.enqueue(
        new MockResponse()
            .withWebSocketUpgrade(
                new WebSocketListener() {
                  @Override
                  public void onMessage(WebSocket webSocket, String string) {
                    received.add(string);
                    JsonObject req = JsonUtils.parse(string);
                    String taskId = req.getAsJsonObject("header").get("task_id").getAsString();
                    if (string.contains("run-task")) {
                      webSocket.send(
                          "{\"header\": {\"task_id\": \""
                              + taskId
                              + "\", \"event\": \"task-started\"}, \"payload\": {}}");
                    } else if (string.contains("finish-task")) {
                      webSocket.send(
                          "{\"header\": {\"task_id\": \""
                              + taskId
                              + "\", \"event\": \"task-finished\"}, \"payload\": {}}");
                      // Release the connection, otherwise MockWebServer.close() times out.
                      webSocket.close(1000, "close by server");
                    }
                  }
                }));
    Constants.baseWebsocketApiUrl = String.format("http://127.0.0.1:%s", server.getPort());

    SpeechSynthesisParam param =
        SpeechSynthesisParam.builder()
            .apiKey("1234")
            .model("cosyvoice-v1")
            .voice("longxiaochun")
            .format(SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS)
            .build();
    // A dedicated callback: the shared one accumulates into the static audioBuffer asserted by the
    // other test.
    SpeechSynthesizerV2 synthesizer =
        new SpeechSynthesizerV2(
            param,
            new ResultCallback<SpeechSynthesisResult>() {
              @Override
              public void onEvent(SpeechSynthesisResult message) {}

              @Override
              public void onComplete() {}

              @Override
              public void onError(Exception e) {}
            });
    synthesizer.setStartedTimeout(2000);
    synthesizer.streamingCall("今天天气怎么样？");
    synthesizer.streamingFlush();
    JsonObject extra = new JsonObject();
    extra.addProperty("index", 1);
    synthesizer.streamingFlush(extra);
    synthesizer.streamingComplete();
    synthesizer.close();

    List<JsonObject> flushInputs = new ArrayList<>();
    for (String message : received) {
      JsonObject sent = JsonUtils.parse(message);
      JsonObject input = sent.getAsJsonObject("payload").getAsJsonObject("input");
      if (input != null && input.has("flush")) {
        // A flush travels as a continue-task, so the session stays open.
        assertEquals("continue-task", sent.getAsJsonObject("header").get("action").getAsString());
        flushInputs.add(input);
      }
    }
    assertEquals(2, flushInputs.size());
    // Plain flush carries the flag only.
    assertTrue(flushInputs.get(0).get("flush").getAsBoolean());
    assertEquals(1, flushInputs.get(0).entrySet().size());
    // Extra params travel next to the flag, which itself stays untouched.
    assertTrue(flushInputs.get(1).get("flush").getAsBoolean());
    assertEquals(1, flushInputs.get(1).get("index").getAsInt());
    server.close();
  }
}
