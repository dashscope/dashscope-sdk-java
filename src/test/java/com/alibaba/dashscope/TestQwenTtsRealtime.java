package com.alibaba.dashscope;

import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtime;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeCallback;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeConfig;
import com.alibaba.dashscope.audio.qwen_tts_realtime.QwenTtsRealtimeParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestQwenTtsRealtime {
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
                                        webSocket.send(
                                                "{\"event_id\":\"event_RiXR49wQsjYDGIdFZZvwV\",\"type\":\"session.created\",\"session\":{\"object\":\"realtime.session\",\"mode\":\"server_commit\",\"model\":\"qwen-tts-realtime\",\"voice\":\"Cherry\",\"response_format\":\"pcm\",\"sample_rate\":24000,\"id\":\"sess_Wf61YVPaIA3d1bPQTyWqE\"}}");
                                    }

                                    @Override
                                    public void onMessage(WebSocket webSocket, String string) {
                                        System.out.println("mock server recv: " + string);
                                        JsonObject req = JsonUtils.parse(string);
                                        if (string.contains("input_text_buffer.commit")) {
                                            webSocket.send(
                                                    "{\"event_id\":\"event_B7p1sjr6AY4OTH2dVhr43\",\"type\":\"input_text_buffer.committed\",\"item_id\":\"\"}");
                                            webSocket.send(
                                                    "{\"event_id\":\"event_EnRZHRRqOOpNZnj56onql\",\"type\":\"response.audio.delta\",\"response_id\":\"resp_Hk9RCaeoY9bEA9aHoenUD\",\"item_id\":\"item_QlDl3OxhiOOv5ZGVRcL5K\",\"output_index\":0,\"content_index\":0,\"delta\":\"xxxx\"}");
                                        }
                                    }

                                    @Override
                                    public void onFailure(
                                            @NotNull WebSocket webSocket,
                                            @NotNull Throwable t,
                                            @Nullable Response response) {
                                        super.onFailure(webSocket, t, response);
                                        t.printStackTrace();
                                        System.out.println("Mock Server onFailure" + t.getMessage());
                                    }
                                });
        mockServer.enqueue(response);
    }

    @AfterAll
    public static void after() throws IOException {
        System.out.println("Mock Server is closed");
    }

    @Test
    public void testQwenTtsRealtime() throws NoApiKeyException, InterruptedException, IOException {
        System.out.println("############ Start Test Qwen Tts Realtime ############");
        int port = mockServer.getPort();
        // 获取 URL
        String url = mockServer.url("/binary").toString();

        // 在真实世界中，你会在这里做 HTTP 请求，并得到响应
        System.out.println("Mock Server is running at: " + url);
        QwenTtsRealtimeParam param =
                QwenTtsRealtimeParam.builder()
                        .model("qwen-tts-realtime")
                        .apikey("1234")
                        .url(String.format("http://127.0.0.1:%s", port))
                        .build();
        final String[] audio = {"wrong text"};

        AtomicReference<CountDownLatch> textLatch = new AtomicReference<>(null);
        textLatch.set(new CountDownLatch(1));

        QwenTtsRealtime ttsRealtime =
                new QwenTtsRealtime(
                        param,
                        new QwenTtsRealtimeCallback() {
                            @Override
                            public void onOpen() {
                                super.onOpen();
                            }

                            @Override
                            public void onEvent(JsonObject message) {
                                System.out.println("onEvent:" + message);
                                if (message.get("type").getAsString().equals("response.audio.delta")) {
                                    audio[0] = message.get("delta").getAsString();
                                    textLatch.get().countDown();
                                }
                            }

                            @Override
                            public void onClose(int code, String reason) {
                                System.out.println("onClose:" + code + ", " + reason);
                            }
                        });

        ttsRealtime.connect();
        QwenTtsRealtimeConfig config =
                QwenTtsRealtimeConfig.builder()
                        .voice("Chelsie")
                        .mode("commit")
                        .build();
        ttsRealtime.updateSession(config);
        ttsRealtime.appendText("你好");
        ttsRealtime.commit();
        textLatch.get().await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals("xxxx", audio[0]);
        ttsRealtime.close();
    }
}
