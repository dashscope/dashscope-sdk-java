package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.protocol.Request;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class RecognitionRequestParam extends RecognitionBaseParam {

  @NonNull private Integer sampleRate;

  @NonNull private String format;

  @Builder.Default private boolean disfluencyRemovalEnabled = false;

  @Override
  public Request toRequest(Protocol protocol) {
    JsonObject jsonParam = new JsonObject();
    jsonParam.addProperty(RecognitionApiKeywords.SAMPLE_RATE, sampleRate);
    jsonParam.addProperty(RecognitionApiKeywords.FORMAT, format);
    jsonParam.addProperty(
        RecognitionApiKeywords.DISFLUENCY_REMOVAL_ENABLED, disfluencyRemovalEnabled);
    RecognitionMessagePayload payload =
        RecognitionMessagePayload.buildInputPayload(getModel(), jsonParam);
    return Request.builder().message(JsonUtils.toJson(payload)).build();
  }
}
