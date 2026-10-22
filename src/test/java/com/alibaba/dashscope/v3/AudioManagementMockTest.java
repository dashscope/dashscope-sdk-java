// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.types.audio.DesignedVoice;
import com.alibaba.dashscope.types.audio.DesignedVoiceList;
import com.alibaba.dashscope.types.audio.Phrase;
import com.alibaba.dashscope.types.audio.PhraseCreateParams;
import com.alibaba.dashscope.types.audio.PhraseList;
import com.alibaba.dashscope.types.audio.Vocabulary;
import com.alibaba.dashscope.types.audio.VocabularyCreateParams;
import com.alibaba.dashscope.types.audio.VocabularyList;
import com.alibaba.dashscope.types.audio.Voice;
import com.alibaba.dashscope.types.audio.VoiceCreateParams;
import com.alibaba.dashscope.types.audio.VoiceDesignCreateParams;
import com.alibaba.dashscope.types.audio.VoiceList;
import com.alibaba.dashscope.types.common.JobStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer tests for the audio management plane (audio design §1.4). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class AudioManagementMockTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MockWebServer server;
  private DashScopeClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    client =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.shutdown();
  }

  private void enqueueJson(String body) {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(body));
  }

  private static JsonNode bodyOf(RecordedRequest request) throws IOException {
    return MAPPER.readTree(request.getBody().readUtf8());
  }

  private static Map<String, Object> word(String text, double weight) {
    Map<String, Object> w = new LinkedHashMap<>();
    w.put("text", text);
    w.put("weight", weight);
    return w;
  }

  @Test
  void vocabulariesCrud() throws Exception {
    enqueueJson("{\"output\":{\"vocabulary_id\":\"vocab-1\"},\"request_id\":\"r1\"}");
    Vocabulary created =
        client.audio().vocabularies().create(
            VocabularyCreateParams.builder()
                .targetModel("paraformer-v2")
                .prefix("pre")
                .vocabulary(Arrays.asList(word("阿里巴巴", 1.0)))
                .build());
    assertEquals("vocab-1", created.getVocabularyId());
    assertEquals("r1", created.getRequestId());

    RecordedRequest create = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/audio/asr/customization", create.getPath());
    JsonNode createBody = bodyOf(create);
    assertEquals("speech-biasing", createBody.get("model").asText());
    JsonNode createInput = createBody.get("input");
    assertEquals("create_vocabulary", createInput.get("action").asText());
    assertEquals("paraformer-v2", createInput.get("target_model").asText());
    assertEquals("pre", createInput.get("prefix").asText());
    assertEquals("阿里巴巴", createInput.get("vocabulary").get(0).get("text").asText());
    assertFalse(createBody.has("parameters"));

    // list: page_index/page_size + optional prefix; items keep their wire shape.
    enqueueJson(
        "{\"output\":{\"vocabulary_list\":[{\"vocabulary_id\":\"v1\",\"prefix\":\"pre\"}]},"
          + "\"request_id\":\"r2\"}");
    VocabularyList list = client.audio().vocabularies().list("pre", 0, 10);
    assertEquals(1, list.getVocabularies().size());
    assertEquals("v1", list.getVocabularies().get(0).get("vocabulary_id"));
    JsonNode listInput = bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input");
    assertEquals("list_vocabulary", listInput.get("action").asText());
    assertEquals(0, listInput.get("page_index").asInt());
    assertEquals(10, listInput.get("page_size").asInt());
    assertEquals("pre", listInput.get("prefix").asText());

    // get / update / delete.
    enqueueJson(
        "{\"output\":{\"vocabulary_id\":\"vocab-1\",\"vocabulary\":[{\"text\":\"阿里\"}]}}");
    Vocabulary got = client.audio().vocabularies().get("vocab-1");
    assertEquals("阿里", got.getVocabulary().get(0).get("text"));
    JsonNode getInput = bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input");
    assertEquals("query_vocabulary", getInput.get("action").asText());
    assertEquals("vocab-1", getInput.get("vocabulary_id").asText());

    enqueueJson("{\"output\":{\"status\":\"SUCCEEDED\"}}");
    JobStatus updated =
        client.audio().vocabularies().update("vocab-1", Arrays.asList(word("阿里云", 2.0)));
    assertEquals("SUCCEEDED", updated.getStatus());
    JsonNode updateInput = bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input");
    assertEquals("update_vocabulary", updateInput.get("action").asText());
    assertEquals("阿里云", updateInput.get("vocabulary").get(0).get("text").asText());

    enqueueJson("{\"output\":{\"status\":\"SUCCEEDED\"}}");
    JobStatus deleted = client.audio().vocabularies().delete("vocab-1");
    assertEquals("SUCCEEDED", deleted.getStatus());
    JsonNode deleteInput = bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input");
    assertEquals("delete_vocabulary", deleteInput.get("action").asText());
  }

  @Test
  void vocabulariesValidationRejectsBadInput() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VocabularyCreateParams.builder()
                .targetModel("paraformer-v2")
                .prefix("pre")
                .build());
    assertThrows(
        IllegalArgumentException.class, () -> client.audio().vocabularies().get("  "));
    assertThrows(
        IllegalArgumentException.class, () -> client.audio().vocabularies().delete(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.audio().vocabularies().update("v1", Collections.emptyList()));
    assertEquals(0, server.getRequestCount());
  }

  @Test
  void voicesCreateNamedWinOverExtraInput() throws Exception {
    enqueueJson(
        "{\"output\":{\"voice_id\":\"voice-1\",\"status\":\"OK\","
          + "\"preview_audio\":{\"url\":\"http://x/p.wav\"}},\"request_id\":\"r3\"}");
    Map<String, Object> extra = new LinkedHashMap<>();
    extra.put("url", "http://hatch/loses.wav");
    extra.put("custom_field", "kept");
    Voice voice =
        client.audio().voices().create(
            VoiceCreateParams.builder()
                .targetModel("cosyvoice-v1")
                .prefix("pre")
                .url("http://files/sample.wav")
                .languageHints(Arrays.asList("zh", "en"))
                .maxPromptAudioLength(15.0)
                .extraInput(extra)
                .build());
    assertEquals("voice-1", voice.getVoiceId());
    assertEquals("OK", voice.getStatus());
    assertEquals("http://x/p.wav", voice.getPreviewAudio().get("url"));

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/audio/tts/customization", request.getPath());
    JsonNode body = bodyOf(request);
    assertEquals("voice-enrollment", body.get("model").asText());
    JsonNode input = body.get("input");
    assertEquals("create_voice", input.get("action").asText());
    // Named url wins over the extraInput hatch; unknown keys pass through.
    assertEquals("http://files/sample.wav", input.get("url").asText());
    assertEquals("kept", input.get("custom_field").asText());
    assertEquals("zh", input.get("language_hints").get(0).asText());
    assertEquals(15.0, input.get("max_prompt_audio_length").asDouble());

    // get / update / delete ride the *_voice actions.
    enqueueJson("{\"output\":{\"voice_id\":\"voice-1\",\"status\":\"OK\"}}");
    assertEquals("voice-1", client.audio().voices().get("voice-1").getVoiceId());
    assertEquals(
        "query_voice",
        bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input").get("action").asText());

    enqueueJson("{\"output\":{\"status\":\"SUCCEEDED\"}}");
    assertEquals(
        "SUCCEEDED",
        client.audio().voices().update("voice-1", "http://files/new.wav").getStatus());
    JsonNode updateInput = bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input");
    assertEquals("update_voice", updateInput.get("action").asText());
    assertEquals("http://files/new.wav", updateInput.get("url").asText());

    enqueueJson("{\"output\":{\"status\":\"SUCCEEDED\"}}");
    assertEquals("SUCCEEDED", client.audio().voices().delete("voice-1").getStatus());
    assertEquals(
        "delete_voice",
        bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input").get("action").asText());
  }

  @Test
  void voicesListAndVoiceDesignCrud() throws Exception {
    // voices().list narrows by prefix.
    enqueueJson("{\"output\":{\"voice_list\":[{\"voice_id\":\"v1\"}]}}");
    VoiceList voices = client.audio().voices().list("pre", 1, 5);
    assertEquals(1, voices.getVoices().size());
    JsonNode listInput = bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input");
    assertEquals("list_voice", listInput.get("action").asText());
    assertEquals(1, listInput.get("page_index").asInt());
    assertEquals("pre", listInput.get("prefix").asText());

    // voiceDesign: shorter action words, id field "voice".
    enqueueJson(
        "{\"output\":{\"voice\":\"vd-1\",\"language\":\"zh\",\"target_model\":\"qwen3-tts\","
          + "\"preview_audio\":{\"url\":\"http://x/v.wav\"}}}");
    DesignedVoice designed =
        client.audio().voiceDesign().create(
            VoiceDesignCreateParams.builder()
                .targetModel("qwen3-tts")
                .voicePrompt("沉稳的男声")
                .previewText("你好，世界")
                .preferredName("narrator")
                .language("zh")
                .build());
    assertEquals("vd-1", designed.getVoice());
    assertEquals("zh", designed.getLanguage());
    assertEquals("qwen3-tts", designed.getTargetModel());
    JsonNode designBody = bodyOf(server.takeRequest(5, TimeUnit.SECONDS));
    assertEquals("qwen-voice-design", designBody.get("model").asText());
    JsonNode designInput = designBody.get("input");
    assertEquals("create", designInput.get("action").asText());
    assertEquals("沉稳的男声", designInput.get("voice_prompt").asText());
    assertEquals("narrator", designInput.get("preferred_name").asText());

    enqueueJson("{\"output\":{\"voice\":\"vd-1\"}}");
    assertEquals("vd-1", client.audio().voiceDesign().get("vd-1").getVoice());
    JsonNode queryInput = bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input");
    assertEquals("query", queryInput.get("action").asText());
    assertEquals("vd-1", queryInput.get("voice").asText());

    enqueueJson("{\"output\":{\"voice_list\":[{\"voice\":\"vd-1\"}]}}");
    DesignedVoiceList designedList = client.audio().voiceDesign().list(0, 10);
    assertEquals(1, designedList.getVoices().size());
    assertEquals(
        "list",
        bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input").get("action").asText());

    enqueueJson("{\"output\":{\"status\":\"SUCCEEDED\"}}");
    assertEquals("SUCCEEDED", client.audio().voiceDesign().delete("vd-1").getStatus());
    assertEquals(
        "delete",
        bodyOf(server.takeRequest(5, TimeUnit.SECONDS)).get("input").get("action").asText());
  }

  @Test
  void phrasesCreatePostsCompileJob() throws Exception {
    enqueueJson("{\"output\":{\"finetuned_output\":\"phrase-1\",\"status\":\"PENDING\"}}");
    Map<String, Object> phrases = new LinkedHashMap<>();
    phrases.put("阿里巴巴", 3);
    Phrase phrase =
        client.audio().phrases().create(
            PhraseCreateParams.builder().model("paraformer-v2").phrases(phrases).build());
    // finetuned_output is the compiled resource id (phraseId alias).
    assertEquals("phrase-1", phrase.getPhraseId());
    assertEquals("PENDING", phrase.getStatus());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", request.getMethod());
    assertEquals("/fine-tunes", request.getPath());
    JsonNode body = bodyOf(request);
    assertEquals("paraformer-v2", body.get("model").asText());
    assertEquals("compile_asr_phrase", body.get("training_type").asText());
    assertTrue(body.get("training_file_ids").isArray());
    assertEquals(0, body.get("training_file_ids").size());
    assertTrue(body.get("validation_file_ids").isArray());
    assertEquals(3, body.get("hyper_parameters").get("phrase_list").get("阿里巴巴").asInt());
    assertFalse(body.has("finetuned_output"));
  }

  @Test
  void phrasesUpdateGetListDelete() throws Exception {
    // update carries finetuned_output = phraseId.
    enqueueJson("{\"output\":{\"finetuned_output\":\"phrase-1\",\"status\":\"RUNNING\"}}");
    Phrase updated =
        client.audio().phrases().update(
            "phrase-1",
            PhraseCreateParams.builder()
                .model("paraformer-v2")
                .phrases(Collections.singletonMap("阿里云", 5))
                .build());
    assertEquals("phrase-1", updated.getPhraseId());
    JsonNode updateBody = bodyOf(server.takeRequest(5, TimeUnit.SECONDS));
    assertEquals("phrase-1", updateBody.get("finetuned_output").asText());
    assertEquals(
        5, updateBody.get("hyper_parameters").get("phrase_list").get("阿里云").asInt());

    // get rides /fine-tunes/outputs/{id}; job_id is the legacy id alias.
    enqueueJson("{\"output\":{\"job_id\":\"phrase-1\",\"status\":\"SUCCEEDED\"}}");
    Phrase got = client.audio().phrases().get("phrase-1");
    assertEquals("phrase-1", got.getPhraseId());
    RecordedRequest get = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("GET", get.getMethod());
    assertEquals("/fine-tunes/outputs/phrase-1", get.getPath());

    // list: page_no/page_size query; jobs + total aliases.
    enqueueJson(
        "{\"output\":{\"jobs\":[{\"job_id\":\"p1\"},{\"job_id\":\"p2\"}],"
          + "\"page_no\":2,\"page_size\":5,\"total\":7}}");
    PhraseList list = client.audio().phrases().list(2, 5);
    assertEquals(2, list.getPageNo());
    assertEquals(5, list.getPageSize());
    assertEquals(7, list.getTotalCount());
    assertEquals(2, list.getPhrases().size());
    assertEquals("p1", list.getPhrases().get(0).getPhraseId());
    RecordedRequest listReq = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/fine-tunes/outputs?page_no=2&page_size=5", listReq.getPath());

    // delete returns JobStatus.
    enqueueJson("{\"output\":{\"status\":\"SUCCEEDED\"}}");
    JobStatus deleted = client.audio().phrases().delete("phrase-1");
    assertEquals("SUCCEEDED", deleted.getStatus());
    RecordedRequest delete = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("DELETE", delete.getMethod());
    assertEquals("/fine-tunes/outputs/phrase-1", delete.getPath());
  }

  @Test
  void phrasesValidationRejectsBadInput() {
    assertThrows(
        IllegalArgumentException.class, () -> PhraseCreateParams.builder().model("m").build());
    assertThrows(IllegalArgumentException.class, () -> client.audio().phrases().get(""));
    assertThrows(IllegalArgumentException.class, () -> client.audio().phrases().delete(null));
    assertEquals(0, server.getRequestCount());
  }
}
