// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.transcription;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class TranscriptionParam extends HalfDuplexServiceParam {

  @NonNull private List<String> fileUrls;

  @Builder.Default private List<Integer> channelId = Collections.singletonList(0);

  private Boolean diarizationEnabled;

  private Integer speakerCount;

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

    JsonObject jsonParameters = new JsonObject();
    jsonParameters.add(TranscriptionApiKeywords.CHANNEL_ID, jsonChannelId);
    if (diarizationEnabled != null) {
      jsonParameters.addProperty(TranscriptionApiKeywords.DIARIZATION_ENABLED, diarizationEnabled);
    }
    if (speakerCount != null) {
      jsonParameters.addProperty(TranscriptionApiKeywords.SPEAKER_COUNT, speakerCount);
    }
    body.add("parameters", jsonParameters);
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
