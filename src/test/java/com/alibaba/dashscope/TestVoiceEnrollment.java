package com.alibaba.dashscope;

import com.alibaba.dashscope.audio.ttsv2.enrollment.Voice;
import com.alibaba.dashscope.audio.ttsv2.enrollment.VoiceEnrollmentService;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestVoiceEnrollment {
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
  public void testVoiceEnrollment()
      throws InterruptedException, NoApiKeyException, InputRequiredException {
    // Test create voice
    JsonObject jsonObject =
        JsonUtils.parse(
            "{'output': {'voice_id': 'voice-nuomai-ef7bae7bef6f4baf97f13d99f9dcd8d6'}, 'usage': {}, 'request_id': '6996d372-b7a2-93ec-9c5d-c90904cbce2a'}");
    JsonObject rspObject = jsonObject.getAsJsonObject();
    MockResponse response = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(response);
    VoiceEnrollmentService service = new VoiceEnrollmentService("1234");
    Voice voice = service.createVoice("cosyvoice-v1", "nuomai", "https://xxxx");
    assert (voice.getVoiceId().equals("voice-nuomai-ef7bae7bef6f4baf97f13d99f9dcd8d6"));

    // Test list voice
    jsonObject =
        JsonUtils.parse(
            "{'output': {'voice_list': [{'gmt_create': '2024-08-20 10:49:08', 'voice_id': 'voice-nuomai-0b84d031ca224adca6354ad45c6efcd0', 'gmt_modified': '2024-08-20 10:49:08', 'status': 'OK'}, {'gmt_create': '2024-08-20 10:26:18', 'voice_id': 'voice-nuomai-2c40bd095ace495b92ca63482b28155e', 'gmt_modified': '2024-08-20 10:26:18', 'status': 'OK'}, {'gmt_create': '2024-08-19 16:23:54', 'voice_id': 'voice-nuomai-cef9dcfe86c04fa4863de6b041e669b8', 'gmt_modified': '2024-08-19 16:23:54', 'status': 'OK'}]}, 'usage': {}, 'request_id': '2828c61b-0c39-9238-b6e1-bd84e3ef9142'}");
    rspObject = jsonObject.getAsJsonObject();
    response = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(response);
    Voice[] voiceList = service.listVoice("nuomai");
    assert (voiceList[0].getVoiceId().equals("voice-nuomai-0b84d031ca224adca6354ad45c6efcd0"));

    // Test query voice
    jsonObject =
        JsonUtils.parse(
            " {'output': {'gmt_create': '2024-08-19 16:23:54', 'resource_link': 'http://xxxx', 'target_model': 'cosyvoice-v1', 'gmt_modified': '2024-08-19 16:23:54', 'status': 'OK'}, 'usage': {}, 'request_id': 'bc8c570e-310e-909a-9f06-93743d4ce87d'}");
    rspObject = jsonObject.getAsJsonObject();
    response = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(response);
    voice = service.queryVoice("voice-nuomai-0b84d031ca224adca6354ad45c6efcd0");
    assert (voice.getResourceLink().equals("http://xxxx"));
  }
}
