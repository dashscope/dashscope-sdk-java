package com.alibaba.dashscope.audio.ttsv2;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisApiKeywords;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisTextType;
import com.alibaba.dashscope.base.FullDuplexServiceParam;
import io.reactivex.Flowable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

/** @author lengjiayi */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class SpeechSynthesisParam extends FullDuplexServiceParam {

  /** Input text type. */
  @Builder.Default private SpeechSynthesisTextType textType = SpeechSynthesisTextType.PLAIN_TEXT;
  /** voice name */
  @Builder.Default private String voice = "";

  /** synthesis audio format. */
  @Builder.Default private SpeechSynthesisAudioFormat format = SpeechSynthesisAudioFormat.DEFAULT;

  /** synthesis audio volume. */
  @Builder.Default private int volume = 50;

  /** synthesis audio speed. */
  @Builder.Default private float speechRate = 1.0f;

  /** synthesis audio pitch. */
  @Builder.Default private float pitchRate = 1.0f;

  /** enable word level timestamp. */
  @Builder.Default private boolean enableWordTimestamp = false;

  /** enable phoneme level timestamp. */
  @Builder.Default private boolean enablePhonemeTimestamp = false;

  @Builder.Default private long connectionTimeout = -1;
  @Builder.Default private long firstPackageTimeout = -1;

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put(SpeechSynthesisApiKeywords.VOICE, getVoice());
    params.put(SpeechSynthesisApiKeywords.TEXT_TYPE, getTextType().getValue());
    params.put(SpeechSynthesisApiKeywords.FORMAT, getFormat().getFormat());
    params.put(SpeechSynthesisApiKeywords.SAMPLE_RATE, getFormat().getSampleRate());
    params.put(SpeechSynthesisApiKeywords.VOLUME, getVolume());
    params.put(SpeechSynthesisApiKeywords.SPEECH_RATE, getSpeechRate());
    params.put(SpeechSynthesisApiKeywords.PITCH_RATE, getPitchRate());
    params.put(SpeechSynthesisApiKeywords.WORD_TIMESTAMP, isEnableWordTimestamp());
    params.put(SpeechSynthesisApiKeywords.PHONEME_TIMESTAMP, isEnablePhonemeTimestamp());
    if (getFormat().getFormat() == "opus") {
      params.put(SpeechSynthesisApiKeywords.BIT_RATE, getFormat().getBitRate());
    }
    params.putAll(parameters);
    return params;
  }

  @Override
  public Object getResources() {
    return null;
  }

  @Override
  public Flowable<Object> getStreamingData() {
    return null;
  }
}
