package com.alibaba.dashscope.audio.ttsv2;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisApiKeywords;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisTextType;
import com.alibaba.dashscope.base.FullDuplexServiceParam;
import io.reactivex.Flowable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

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

  /**
   * the following parameters take effect only in CosyVoice V3 and later versions. instruction for
   * synthesis.
   */
  @Builder.Default private String instruction = null;
  /** random seed. */
  @Builder.Default private int seed = 0;
  /** language hints. */
  @Builder.Default private List<String> languageHints = null;
  /** synthesis style */
  @Builder.Default private int style = 0;
  /** Hot fix configuration for pronunciation and replace rules. */
  @Builder.Default private ParamHotFix hotFix = null;

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
    if (getInstruction() != null) {
      params.put(SpeechSynthesisApiKeywords.INSTRUCTION, getInstruction());
    }
    if (getSeed() != 0) {
      params.put(SpeechSynthesisApiKeywords.SEED, getSeed());
    }
    if (getLanguageHints() != null) {
      params.put(SpeechSynthesisApiKeywords.LANGUAGE_HINTS, getLanguageHints());
    }
    if (getStyle() != 0) {
      params.put(SpeechSynthesisApiKeywords.STYLE, getStyle());
    }
    // Add hot fix parameters if present
    if (getHotFix() != null) {
      Map<String, Object> hotFixParams = new HashMap<>();
      if (getHotFix().getPronunciation() != null && !getHotFix().getPronunciation().isEmpty()) {
        hotFixParams.put(SpeechSynthesisApiKeywords.PRONUNCIATION, getHotFix().getPronunciation());
      }
      if (getHotFix().getReplace() != null && !getHotFix().getReplace().isEmpty()) {
        hotFixParams.put(SpeechSynthesisApiKeywords.REPLACE, getHotFix().getReplace());
      }
      if (!hotFixParams.isEmpty()) {
        params.put(SpeechSynthesisApiKeywords.HOT_FIX, hotFixParams);
      }
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
