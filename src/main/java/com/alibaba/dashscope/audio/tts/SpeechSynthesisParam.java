// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.tts;

import static com.alibaba.dashscope.utils.ApiKeywords.TEXT;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class SpeechSynthesisParam extends HalfDuplexServiceParam {

  /** The input text. */
  @NonNull private String text;

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

  @Override
  public JsonObject getHttpBody() {
    /* not support http */
    throw new UnsupportedOperationException("Not support http");
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put(SpeechSynthesisApiKeywords.TEXT_TYPE, getTextType().getValue());
    params.put(SpeechSynthesisApiKeywords.FORMAT, getFormat().getValue());
    params.put(SpeechSynthesisApiKeywords.SAMPLE_RATE, getSampleRate());
    params.put(SpeechSynthesisApiKeywords.VOLUME, getVolume());
    params.put(SpeechSynthesisApiKeywords.SPEECH_RATE, getRate());
    params.put(SpeechSynthesisApiKeywords.PITCH_RATE, getPitch());
    params.put(SpeechSynthesisApiKeywords.WORD_TIMESTAMP, isEnableWordTimestamp());
    params.put(SpeechSynthesisApiKeywords.PHONEME_TIMESTAMP, isEnablePhonemeTimestamp());
    params.putAll(parameters);
    return params;
  }

  @Override
  public Object getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(TEXT, getText());
    return jsonObject;
  }

  @Override
  public ByteBuffer getBinaryData() {
    return null;
  }

  @Override
  public void validate() throws InputRequiredException {}
}
