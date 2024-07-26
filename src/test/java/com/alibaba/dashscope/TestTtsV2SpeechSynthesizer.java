// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
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
public class TestTtsV2SpeechSynthesizer {
  private static ArrayList<Byte> audioBuffer;
  private static ResultCallback<SpeechSynthesisResult> callback =
      new ResultCallback<SpeechSynthesisResult>() {
        @Override
        public void onEvent(SpeechSynthesisResult message) {
          //            System.out.println("onEvent:" + message);
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
                    JsonObject req = JsonUtils.parse(string);
                    if (task_id == "") {
                      task_id = req.get("header").getAsJsonObject().get("task_id").getAsString();
                    }
                    if (string.contains("run-task")) {
                      webSocket.send(
                          "{'header': {'task_id': '"
                              + task_id
                              + "', 'event': 'task-started', 'attributes': {}}, 'payload': {}}");
                    } else if (string.contains("finish-task")) {
                      webSocket.send(
                          "{'header': {'task_id': '"
                              + task_id
                              + "', 'event': 'task-finished', 'attributes': {}}, 'payload': {'output': None, 'usage': {'characters': 7}}}");
                      webSocket.close(1000, "close by server");
                    } else if (string.contains("continue-task")) {
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
  public void testStreamingCall() throws InterruptedException {
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
    SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);
    for (int i = 0; i < 3; i++) {
      synthesizer.streamingCall("今天天气怎么样？");
    }
    try {
      synthesizer.streamingComplete();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    assertEquals(audioBuffer.size(), 9);
    for (int i = 0; i < 9; i++) {
      assertEquals((byte) audioBuffer.get(i), (byte) 0x01);
    }
    System.out.println("############ Start Test Streaming Call Done ############");
  }
}
