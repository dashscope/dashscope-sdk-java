package com.alibaba.dashscope.audio.tts;

import static com.alibaba.dashscope.utils.ApiKeywords.TEXT;

import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.protocol.Request;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class SpeechSynthesisParam extends Param {

  /** The input text. */
  private String text;

  /** Input text type. */
  @Builder.Default private SpeechSynthesisTextType textType = SpeechSynthesisTextType.PLAIN_TEXT;

  /** synthesis audio format. */
  @Builder.Default private SpeechSynthesisAudioFormat format = SpeechSynthesisAudioFormat.WAV;

  /** synthesis audio sample rate. */
  @Builder.Default private int sampleRate = 16000;

  /** synthesis audio volume. */
  @Builder.Default private int volume = 50;

  /** synthesis audio speed. */
  @Builder.Default private float rate = 1.0f;

  /** synthesis audio pitch. */
  @Builder.Default private float pitch = 1.0f;

  /** enable word level timestamp. */
  @Builder.Default private boolean enableWordTimestamp = false;

  /** enable phoneme level timestamp. */
  @Builder.Default private boolean enablePhonemeTimestamp = false;

  private JsonObject toParameters() {
    JsonObject parameters = new JsonObject();
    parameters.addProperty(SpeechSynthesisApiKeywords.TEXT_TYPE, getTextType().getValue());
    parameters.addProperty(SpeechSynthesisApiKeywords.FORMAT, getFormat().getValue());
    parameters.addProperty(SpeechSynthesisApiKeywords.SAMPLE_RATE, getSampleRate());
    parameters.addProperty(SpeechSynthesisApiKeywords.VOLUME, getVolume());
    parameters.addProperty(SpeechSynthesisApiKeywords.SPEECH_RATE, getRate());
    parameters.addProperty(SpeechSynthesisApiKeywords.PITCH_RATE, getPitch());
    parameters.addProperty(SpeechSynthesisApiKeywords.WORD_TIMESTAMP, isEnableWordTimestamp());
    parameters.addProperty(
        SpeechSynthesisApiKeywords.PHONEME_TIMESTAMP, isEnablePhonemeTimestamp());
    return parameters;
  }

  @Override
  public String url() {
    return String.format(
        "/services/%s/%s/%s?request_id=%s",
        TaskGroup.AUDIO.getValue(),
        Task.TEXT_TO_SPEECH.getValue(),
        Function.SPEECH_SYNTHESIZER.getValue(),
        this.getRequestId());
  }

  @Override
  public Request toRequest(Protocol protocol) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(TEXT, getText());
    SpeechSynthesisMessagePayload payload =
        SpeechSynthesisMessagePayload.buildInputPayload(getModel(), jsonObject);
    payload.setParameters(toParameters());
    return Request.builder().message(JsonUtils.toJson(payload)).build();
  }

  @Override
  public Class<? extends Result> resultType() {
    return SpeechSynthesisResult.class;
  }
}
