// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.transcription;

import com.alibaba.dashscope.audio.asr.phrase.AsrPhraseApiKeywords;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Slf4j
public class TranscriptionParam extends HalfDuplexServiceParam {

  @NonNull private List<String> fileUrls;

  private String phraseId;

  @Builder.Default private List<Integer> channelId = Collections.singletonList(0);

  private Boolean diarizationEnabled;

  private Integer speakerCount;

  private Boolean disfluencyRemovalEnabled;

  private Boolean timestampAlignmentEnabled;

  private String specialWordFilter;

  private Boolean audioEventDetectionEnabled;

  private String vocabularyId;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("model", getModel());
    JsonArray jsonFileUrls = new JsonArray();
    for (String fileUrl : fileUrls) {
      jsonFileUrls.add(fileUrl);
    }

    JsonArray jsonChannelId = new JsonArray();
    if (channelId == null) {
      channelId = Collections.singletonList(0);
    }
    for (Integer id : channelId) {
      jsonChannelId.add(id);
    }

    JsonObject jsonInput = new JsonObject();
    jsonInput.add(TranscriptionApiKeywords.FILE_URLS, jsonFileUrls);
    body.add("input", jsonInput);

    JsonObject jsonParameters = JsonUtils.parametersToJsonObject(getParameters());
    jsonParameters.add(TranscriptionApiKeywords.CHANNEL_ID, jsonChannelId);
    if (diarizationEnabled != null) {
      jsonParameters.addProperty(TranscriptionApiKeywords.DIARIZATION_ENABLED, diarizationEnabled);
    }
    if (speakerCount != null) {
      jsonParameters.addProperty(TranscriptionApiKeywords.SPEAKER_COUNT, speakerCount);
    }
    if (disfluencyRemovalEnabled != null) {
      jsonParameters.addProperty(
          TranscriptionApiKeywords.DISFLUENCY_REMOVAL_ENABLED, disfluencyRemovalEnabled);
    }
    if (timestampAlignmentEnabled != null) {
      jsonParameters.addProperty(
          TranscriptionApiKeywords.TIMESTAMP_ALIGNMENT_ENABLED, timestampAlignmentEnabled);
    }
    if (specialWordFilter != null) {
      jsonParameters.addProperty(TranscriptionApiKeywords.SPECIAL_WORD_FILTER, specialWordFilter);
    }
    if (audioEventDetectionEnabled != null) {
      jsonParameters.addProperty(
          TranscriptionApiKeywords.AUDIO_EVENT_DETECTION_ENABLED, audioEventDetectionEnabled);
    }
    if (vocabularyId != null) {
      jsonParameters.addProperty(TranscriptionApiKeywords.VOCABULARY_ID, vocabularyId);
    }
    body.add("parameters", jsonParameters);
    if (phraseId != null) {
      JsonElement jsonResources = new JsonArray();
      JsonObject jsonPhraseResource = new JsonObject();
      jsonPhraseResource.addProperty(ApiKeywords.RESOURCE_ID, phraseId);
      jsonPhraseResource.addProperty(
          ApiKeywords.RESOURCE_TYPE, AsrPhraseApiKeywords.RESOURCE_TYPE_PHRASE);
      jsonResources.getAsJsonArray().add(jsonPhraseResource);
      body.add(ApiKeywords.RESOURCES, jsonResources);
    }
    log.info("body=>{}", body);
    return body;
  }

  @Override
  public Object getInput() {
    JsonArray jsonFileUrls = new JsonArray();
    for (String fileUrl : fileUrls) {
      jsonFileUrls.add(fileUrl);
    }

    JsonObject jsonInput = new JsonObject();
    jsonInput.add(TranscriptionApiKeywords.FILE_URLS, jsonFileUrls);
    return jsonInput;
  }

  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  @Override
  public void validate() throws InputRequiredException {}
}
