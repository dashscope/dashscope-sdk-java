// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestTranscription {
  private static MockWebServer mockServer;

  @BeforeAll
  public static void before() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s/api/v1/", mockServer.getPort());
    Constants.apiKey = "1234";
  }

  @AfterAll
  public static void after() throws IOException {
    System.out.println("Mock Server is closed");
    mockServer.close();
  }

  @Test
  public void testTranscription() throws InterruptedException {
    JsonObject jsonObject =
        JsonUtils.parse(
            "{'output': {'task_status': 'PENDING', 'task_id': '0123456789abcd'}, 'request_id': '22427afc-6fde-9aa4-97c1-577a36803168'}");
    JsonObject rspObject = jsonObject.getAsJsonObject();
    MockResponse response = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(response);
    TranscriptionParam param =
        TranscriptionParam.builder()
            .model("paraformer-v1")
            .fileUrls(
                Arrays.asList(
                    "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav"))
            .parameter("language_hints", new String[] {"zh", "ja"})
            .build();
    Transcription transcription = new Transcription();
    TranscriptionResult result = transcription.asyncCall(param);
    RecordedRequest request = mockServer.takeRequest();
    JsonObject requestBody = JsonUtils.parse(request.getBody().readUtf8());
    JsonObject parameters = requestBody.getAsJsonObject("parameters");
    JsonElement languageHints = parameters.get("language_hints");
    assert (languageHints != null);
    System.out.println(languageHints);
    jsonObject =
        JsonUtils.parse(
            "{\"status_code\": 200, \"request_id\": \"3e5d1ec1-883d-9050-ba08-68d0261b403a\", \"code\": null, \"message\": \"\", \"output\": {\"task_id\": \"a4dbb067-ee19-4c1b-990e-d36e9162d3f4\", \"task_status\": \"SUCCEEDED\", \"submit_time\": \"2024-07-24 11:02:14.751\", \"scheduled_time\": \"2024-07-24 11:02:14.772\", \"task_metrics\": {\"TOTAL\": 1, \"SUCCEEDED\": 1, \"FAILED\": 0}}, \"usage\": null}");
    rspObject = jsonObject.getAsJsonObject();
    response = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(response);
    result =
        transcription.wait(
            TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
  }
}
