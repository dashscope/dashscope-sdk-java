// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerChat;
import com.alibaba.dashscope.audio.asr.translation.TranslationRecognizerParam;
import com.alibaba.dashscope.audio.asr.translation.results.TranslationRecognizerResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestTranslationRecognizerChat {

  private static MockWebServer mockServer;

  @BeforeAll
  public static void before() throws IOException {
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
                      String fakeStartedEvent =
                          "{'header': {'task_id': '"
                              + task_id
                              + "', 'event': 'task-started', 'attributes': {}}, 'payload': {}}";
                      System.out.println("MockServer >>> " + fakeStartedEvent);
                      webSocket.send(fakeStartedEvent);

                    } else if (string.contains("finish-task")) {
                      // 收到音频，模拟返回识别结果sentenceEnd
                      String fakeAsrResult =
                          "{\"header\":{\"task_id\":\""
                              + task_id
                              + "\",\"event\":\"result-generated\",\"attributes\":{}},\"payload\":{\"output\":{\"translations\":[{\"sentence_id\":0,\"begin_time\":0,\"end_time\":2900,\"text\":\"Hello, world.\",\"lang\":\"en\",\"vad_pre_end\":false,\"words\":[{\"begin_time\":0,\"end_time\":725,\"text\":\"Hello\",\"punctuation\":\"Hello\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":725,\"end_time\":1450,\"text\":\",\",\"punctuation\":\",\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":1450,\"end_time\":2175,\"text\":\" world\",\"punctuation\":\" world\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":2175,\"end_time\":2900,\"text\":\".\",\"punctuation\":\".\",\"fixed\":false,\"speaker_id\":null}],\"sentence_end\":true}],\"transcription\":{\"sentence_id\":0,\"begin_time\":0,\"end_time\":2900,\"text\":\"hello ,word,这里是。\",\"words\":[{\"begin_time\":0,\"end_time\":483,\"text\":\"hello\",\"punctuation\":\"hello\",\"fixed\":true,\"speaker_id\":null},{\"begin_time\":483,\"end_time\":966,\"text\":\" ,\",\"punctuation\":\" ,\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":966,\"end_time\":1450,\"text\":\"word\",\"punctuation\":\"word\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":1450,\"end_time\":1933,\"text\":\",\",\"punctuation\":\",\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":1933,\"end_time\":2416,\"text\":\"这里是\",\"punctuation\":\"这里是\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":2416,\"end_time\":2900,\"text\":\"。\",\"punctuation\":\"。\",\"fixed\":false,\"speaker_id\":null}],\"sentence_end\":true,\"vad_pre_end\":false}}}}";
                      webSocket.send(fakeAsrResult);
                      System.out.println("MockServer >>> " + fakeAsrResult);
                      String fakeFinishedEvent =
                          "{'header': {'task_id': '"
                              + task_id
                              + "', 'event': 'task-finished', 'attributes': {}}, 'payload': {'output': {}, 'usage': {'duration': 7}}}";
                      webSocket.send(fakeFinishedEvent);
                      System.out.println("MockServer >>> " + fakeFinishedEvent);
                      webSocket.close(1000, "close by server");
                    } else {
                      throw new RuntimeException("unexpected action");
                    }
                  }

                  @Override
                  public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                    // 收到音频，模拟返回识别结果
                    String fakeAsrResult =
                        "{\"header\":{\"task_id\":\""
                            + task_id
                            + "\",\"event\":\"result-generated\",\"attributes\":{}},\"payload\":{\"output\":{\"translations\":[{\"sentence_id\":0,\"begin_time\":0,\"end_time\":2900,\"text\":\"Hello, world.\",\"lang\":\"en\",\"vad_pre_end\":false,\"words\":[{\"begin_time\":0,\"end_time\":725,\"text\":\"Hello\",\"punctuation\":\"Hello\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":725,\"end_time\":1450,\"text\":\",\",\"punctuation\":\",\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":1450,\"end_time\":2175,\"text\":\" world\",\"punctuation\":\" world\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":2175,\"end_time\":2900,\"text\":\".\",\"punctuation\":\".\",\"fixed\":false,\"speaker_id\":null}],\"sentence_end\":false}],\"transcription\":{\"sentence_id\":0,\"begin_time\":0,\"end_time\":2900,\"text\":\"hello ,word,这里是。\",\"words\":[{\"begin_time\":0,\"end_time\":483,\"text\":\"hello\",\"punctuation\":\"hello\",\"fixed\":true,\"speaker_id\":null},{\"begin_time\":483,\"end_time\":966,\"text\":\" ,\",\"punctuation\":\" ,\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":966,\"end_time\":1450,\"text\":\"word\",\"punctuation\":\"word\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":1450,\"end_time\":1933,\"text\":\",\",\"punctuation\":\",\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":1933,\"end_time\":2416,\"text\":\"这里是\",\"punctuation\":\"这里是\",\"fixed\":false,\"speaker_id\":null},{\"begin_time\":2416,\"end_time\":2900,\"text\":\"。\",\"punctuation\":\"。\",\"fixed\":false,\"speaker_id\":null}],\"sentence_end\":false,\"vad_pre_end\":false}}}}";
                    webSocket.send(fakeAsrResult);
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
  public void testChatTask() throws InterruptedException {
    System.out.println("############ Start Test TranslationRecognizerChat Call ############");
    int port = mockServer.getPort();
    Constants.baseWebsocketApiUrl = String.format("http://127.0.0.1:%s", port);

    // 获取 URL
    String url = mockServer.url("/binary").toString();

    // 在真实世界中，你会在这里做 HTTP 请求，并得到响应
    System.out.println("Mock Server is running at: " + url);

    TranslationRecognizerParam param =
        TranslationRecognizerParam.builder()
            .model("gummy-chat-v1")
            .format("wav") // 'pcm'、'wav'、'opus'、'speex'、'aac'、'amr', you
            // can check the supported formats in the document
            .sampleRate(16000) // supported 8000、16000
            .apiKey("fake-apikey")
            .transcriptionEnabled(true)
            .translationEnabled(true)
            .translationLanguages(new String[] {"en"})
            .build();

    TranslationRecognizerChat translator = new TranslationRecognizerChat();
    CountDownLatch latch = new CountDownLatch(1);

    String threadName = Thread.currentThread().getName();

    ResultCallback<TranslationRecognizerResult> callback =
        new ResultCallback<TranslationRecognizerResult>() {
          @Override
          public void onEvent(TranslationRecognizerResult result) {
            System.out.println("RequestId: " + result.getRequestId());
            // 打印最终结果
            if (result.getTranscriptionResult() != null) {
              System.out.println("Transcription Result:" + result);
              if (result.isSentenceEnd()) {
                System.out.println("\tFix:" + result.getTranscriptionResult().getText());
                System.out.println("\tStash:" + result.getTranscriptionResult().getStash());
              } else {
                System.out.println("\tTemp Result:" + result.getTranscriptionResult().getText());
              }
            }
            if (result.getTranslationResult() != null) {
              System.out.println("English Translation Result:");
              if (result.isSentenceEnd()) {
                System.out.println(
                    "\tFix:" + result.getTranslationResult().getTranslation("en").getText());
                System.out.println(
                    "\tStash:" + result.getTranslationResult().getTranslation("en").getStash());
              } else {
                System.out.println(
                    "\tTemp Result:"
                        + result.getTranslationResult().getTranslation("en").getText());
              }
            }
          }

          @Override
          public void onComplete() {
            System.out.println("[" + threadName + "] Translation complete");
            latch.countDown();
          }

          @Override
          public void onError(Exception e) {
            e.printStackTrace();
            System.out.println("[" + threadName + "] TranslationCallback error: " + e.getMessage());
          }
        };
    // set param & callback
    translator.call(param, callback);

    for (int i = 0; i < 3; i++) {
      translator.sendAudioFrame(ByteBuffer.wrap(new byte[] {0x01, 0x01, 0x01}));
    }

    translator.stop();

    System.out.println("############ Start Test TranslationRecognizerChat Done ############");
  }
}
