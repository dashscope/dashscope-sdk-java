package com.alibaba.dashscope;

import com.alibaba.dashscope.audio.asr.vocabulary.Vocabulary;
import com.alibaba.dashscope.audio.asr.vocabulary.VocabularyService;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestVocabulary {
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
            "{'output': {'vocabulary_id': 'vocab-nuomai-42cde24f833a46cda2e766b219582dc4'}, 'usage': {}, 'request_id': '4023f263-584d-9f99-be54-5eeb1678395b'} ");
    JsonObject rspObject = jsonObject.getAsJsonObject();
    MockResponse response = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(response);
    VocabularyService service = new VocabularyService("1234");
    Vocabulary voc = service.createVocabulary("paraformer-v1", "nuomai", new JsonArray());
    assert (voc.getVocabularyId().equals("vocab-nuomai-42cde24f833a46cda2e766b219582dc4"));

    // Test list voice
    jsonObject =
        JsonUtils.parse(
            "{'output': {'vocabulary_list': [{'gmt_create': '2024-08-20 10:49:08', 'vocabulary_id': 'vocab-nuomai-0b84d031ca224adca6354ad45c6efcd0', 'gmt_modified': '2024-08-20 10:49:08', 'status': 'OK'}, {'gmt_create': '2024-08-20 10:26:18', 'vocabulary_id': 'vocab-nuomai-2c40bd095ace495b92ca63482b28155e', 'gmt_modified': '2024-08-20 10:26:18', 'status': 'OK'}, {'gmt_create': '2024-08-19 16:23:54', 'vocabulary_id': 'vocab-nuomai-cef9dcfe86c04fa4863de6b041e669b8', 'gmt_modified': '2024-08-19 16:23:54', 'status': 'OK'}]}, 'usage': {}, 'request_id': '2828c61b-0c39-9238-b6e1-bd84e3ef9142'}");
    rspObject = jsonObject.getAsJsonObject();
    response = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(response);
    Vocabulary[] vocabularyList = service.listVocabulary("nuomai");
    assert (vocabularyList[0]
        .getVocabularyId()
        .equals("vocab-nuomai-0b84d031ca224adca6354ad45c6efcd0"));

    // Test query voice
    jsonObject =
        JsonUtils.parse(
            " {'output': {'gmt_create': '2024-08-21 16:12:04', 'vocabulary': [{'weight': 5, 'text': '阿里巴巴', 'lang': 'zh'}, {'weight': 5, 'text': 'speech recognition', 'lang': 'en'}], 'target_model': 'paraformer-realtime-v2', 'gmt_modified': '2024-08-21 16:12:04', 'status': 'OK'}, 'usage': {}, 'request_id': 'bc4f6918-18f7-9ea4-97cb-14dad79efdfe'}");
    rspObject = jsonObject.getAsJsonObject();
    response = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(response);
    voc = service.queryVocabulary("vocab-nuomai-0b84d031ca224adca6354ad45c6efcd0");
    assert (voc.getVocabulary().get(0).getAsJsonObject().get("text").getAsString().equals("阿里巴巴"));
  }
}
