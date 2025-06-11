// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import com.alibaba.dashscope.multimodal.MultiModalDialog;
import com.alibaba.dashscope.multimodal.MultiModalDialogCallback;
import com.alibaba.dashscope.multimodal.MultiModalRequestParam;
import com.alibaba.dashscope.multimodal.State;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertThrows;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestMultimodalDialog {
  private final String workSpaceId = "";
  private final String appId = "";
  private final String modelName = "multimodal-dialog";
  private static MultiModalDialog multiModalDialog;
  private static MockWebServer mockServer;
  private static State.DialogState currentState;
    static int enterListeningTimes = 0;

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
                      webSocket.send(
                          "{'header': {'task_id': '"
                              + task_id
                              + "', 'event': 'task-started', 'attributes': {}}, 'payload': {'output':{'event':'Started','dialog_id':'bfce4b14-32d8-48f1-b30d-c98789f319be'}}}");
                        webSocket.send(
                                "{'header': {'event': 'result-generated', 'task_id': '"
                                        + task_id
                                        + "', 'attributes': {}}, 'payload': {'output':{'event':'DialogStateChanged','state':'Listening','dialog_id':'bfce4b14-32d8-48f1-b30d-c98789f319be'}}}");

                    } else if (string.contains("finish-task")) {
                      webSocket.send(
                          "{'header': {'task_id': '"
                              + task_id
                              + "', 'event': 'task-finished', 'attributes': {}}, 'payload': {'output': {'event':'stopped'}, 'usage': {'characters': 7}}}");
                      webSocket.close(1000, "close by server");
                    } else if (string.contains("continue-task")) {
                        if (string.contains("prompt")) {
                            webSocket.send(
                                    "{'header': {'event': 'result-generated','task_id': '"
                                            + task_id
                                            + "', 'attributes': {}}, 'payload': {'output':{'event':'DialogStateChanged','state':'Thinking','dialog_id':'bfce4b14-32d8-48f1-b30d-c98789f319be'}}}");

                            webSocket.send(
                                    "{'header': {'event': 'result-generated','task_id': '"
                                            + task_id
                                            + "', 'attributes': {}}, 'payload': {'output':{'event':'DialogStateChanged','state':'Responding','dialog_id':'bfce4b14-32d8-48f1-b30d-c98789f319be'}}}");

                            webSocket.send(
                                    "{'header': {'event': 'result-generated','task_id': '"
                                            + task_id
                                            + "', 'attributes': {}}, 'payload': {'output':{'event':'RespondingStarted','dialog_id':'bfce4b14-32d8-48f1-b30d-c98789f319be'}}}");


                        }

                        if (string.contains("LocalRespondingStarted")) {
                            byte[] binary = new byte[] {0x01, 0x01, 0x01};
                            webSocket.send(new ByteString(binary));

                            webSocket.send(
                                    "{'header': {'event': 'result-generated','task_id': '"
                                            + task_id
                                            + "', 'attributes': {}}, 'payload': {'output':{'event':'RespondingEnded','dialog_id':'bfce4b14-32d8-48f1-b30d-c98789f319be'}}}");
                        }


                        if (string.contains("LocalRespondingEnded")) {
                            //重新切换到Listening状态
                            webSocket.send(
                                    "{'header': {'event': 'result-generated','task_id': '"
                                            + task_id
                                            + "', 'attributes': {}}, 'payload': {'output':{'event':'DialogStateChanged','state':'Listening','dialog_id':'bfce4b14-32d8-48f1-b30d-c98789f319be'}}}");

                        }

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
  public void testDialog() throws InterruptedException {
    System.out.println("############ Start Test Dialog ############");
    int port = mockServer.getPort();
    Constants.baseWebsocketApiUrl = String.format("http://127.0.0.1:%s", port);

    // 获取 URL
    String url = mockServer.url("/api-ws/v1/inference").toString();

    // 在真实世界中，你会在这里做 HTTP 请求，并得到响应
    System.out.println("Mock Server is running at: " + url);
      MultiModalRequestParam params =
              MultiModalRequestParam.builder()
                      .customInput(
                              MultiModalRequestParam.CustomInput.builder()
                                      .workspaceId(workSpaceId)
                                      .appId(appId)
                                      .build())
                      .upStream(
                              MultiModalRequestParam.UpStream.builder()
                                      .mode("push2talk")
                                      .audioFormat("pcm")
                                      .build())
                      .downStream(
                              MultiModalRequestParam.DownStream.builder()
                                      .voice("longxiaochun_v2")
                                      .sampleRate(48000)
                                      .build())
                      .clientInfo(
                              MultiModalRequestParam.ClientInfo.builder()
                                      .userId("1234")
                                      .device(MultiModalRequestParam.ClientInfo.Device.builder().uuid("device_1234").build())
                                      .build())
                      .model(modelName)
                      .apiKey("api_key")
                      .build();
      multiModalDialog = new MultiModalDialog(params, getCallback());
      multiModalDialog.start();

      while (currentState != State.DialogState.LISTENING) {
          try {
              sleep(100);
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          }
      }
      // 模拟语音请求
      multiModalDialog.requestToRespond("prompt","讲个故事",null);
      // 增加交互流程等待
      while (enterListeningTimes < 2) {
          try {
              sleep(2000);
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          }
      }
      multiModalDialog.stop();
      try {
          sleep(1000);
      } catch (InterruptedException e) {
          throw new RuntimeException(e);
      }

    System.out.println("############ Start Test Dialog Done ############");
  }


    public static MultiModalDialogCallback getCallback() {
        return new MultiModalDialogCallbackImpl();
    }
    public static class MultiModalDialogCallbackImpl extends MultiModalDialogCallback {
        @Override
        public void onConnected() {}
        @Override
        public void onStarted(String dialogId) {
            log.info("onStarted: {}", dialogId);
        }
        @Override
        public void onStopped(String dialogId) {
            log.info("onStopped: {}", dialogId);
        }
        @Override
        public void onSpeechStarted(String dialogId) {
            log.info("onSpeechStarted: {}", dialogId);
        }
        @Override
        public void onSpeechEnded(String dialogId) {
            log.info("onSpeechEnded: {}", dialogId);
        }
        @Override
        public void onError(String dialogId, String errorCode, String errorMsg) {
            log.error("onError: {}, {}, {}", dialogId, errorCode, errorMsg);
            assertThrows(RuntimeException.class, () -> {
                throw new RuntimeException(errorMsg);
            });
        }
        @Override
        public void onStateChanged(State.DialogState state) {
            currentState = state;
            log.info("onStateChanged: {}", state);
            if (currentState == State.DialogState.LISTENING) {
                enterListeningTimes++;
                log.info("enterListeningTimes: {}", enterListeningTimes);
            }

        }
        @Override
        public void onSpeechAudioData(ByteBuffer audioData) {
            //write audio data to file
            //or redirect to audio player
        }
        @Override
        public void onRespondingStarted(String dialogId) {
            log.info("onRespondingStarted: {}", dialogId);
            multiModalDialog.localRespondingStarted();
        }

        @Override
        public void onRespondingEnded(String dialogId, JsonObject content) {
            log.info("onRespondingEnded: {}", dialogId);
            multiModalDialog.localRespondingEnded();
        }


        @Override
        public void onRespondingContent(String dialogId, JsonObject content) {
            log.info("onRespondingContent: {}, {}", dialogId, content);
            if (content.has("extra_info")) {
                JsonObject extraInfo = content.getAsJsonObject("extra_info");
                if (extraInfo.has("commands")) {
                    String commandsStr = extraInfo.get("commands").getAsString();
                    log.info("commandsStr: {}", commandsStr);
                    //"[{\"name\":\"visual_qa\",\"params\":[{\"name\":\"shot\",\"value\":\"拍照看看\",\"normValue\":\"True\"}]}]"
                    JsonArray commands = new Gson().fromJson(commandsStr, JsonArray.class);
                }
            }
        }
        @Override
        public void onSpeechContent(String dialogId, JsonObject content) {
            log.info("onSpeechContent: {}, {}", dialogId, content);
        }
        @Override
        public void onRequestAccepted(String dialogId) {
            log.info("onRequestAccepted: {}", dialogId);
        }
        @Override
        public void onClosed() {
            log.info("onClosed");
        }
    }
}
