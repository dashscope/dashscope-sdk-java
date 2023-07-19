package com.alibaba.dashscope.audio.asr.transcription;

import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.protocol.Request;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
public class TranscriptionParam extends Param {

  @NonNull private List<String> fileUrls;

  @Builder.Default private List<Integer> channelId = Collections.singletonList(0);

  @Override
  public String url() {
    return String.format(
        "/services/%s/%s/%s",
        TaskGroup.AUDIO.getValue(), Task.ASR.getValue(), Function.TRANSCRIPTION.getValue());
  }

  @Override
  public Request toRequest(Protocol protocol) {
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

    JsonObject jsonParameters = new JsonObject();
    jsonParameters.add(TranscriptionApiKeywords.CHANNEL_ID, jsonChannelId);

    TranscriptionMessagePayload payload =
        TranscriptionMessagePayload.buildInputPayload(getModel(), jsonInput, jsonParameters);
    return Request.builder().message(JsonUtils.toJson(payload)).build();
  }

  @Override
  public Class<? extends Result> resultType() {
    return TranscriptionResult.class;
  }
}
