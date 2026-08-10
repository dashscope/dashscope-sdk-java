// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;

/**
 * New-style upstream/downstream audio format configuration, used to build the nested {@code
 * audio.input.format} / {@code audio.output.format} structure in the session.update request, e.g.
 *
 * <pre>{@code
 * "session": {
 *   "audio": {
 *     "input": { "format": { "type": "pcm", "sample_rate": 16000 } },
 *     "output": { "format": { "type": "pcm", "sample_rate": 24000 } }
 *   }
 * }
 * }</pre>
 */
@Data
public class OmniRealtimeAudioFormatConfig {
  private static final Set<Integer> SUPPORTED_SAMPLE_RATES =
      new HashSet<>(Arrays.asList(8000, 16000, 24000, 48000));

  /** audio format type, supports "pcm" and "wav", defaults to "pcm". */
  private OmniRealtimeAudioCodec type = OmniRealtimeAudioCodec.PCM;

  /** sample rate in Hz, supports 8000/16000/24000/48000, defaults to 16000. */
  private int sampleRate = 16000;

  /**
   * Extra format parameters for future extension (e.g. speech rate). These entries are merged into
   * the {@code format} node alongside {@code type}/{@code sample_rate} when serialized, so new
   * server-side parameters can be passed through without changing this SDK. Reserved keys ({@code
   * type}/{@code sample_rate}) set here are ignored to avoid overriding the typed fields.
   */
  private Map<String, Object> parameters;

  public OmniRealtimeAudioFormatConfig() {}

  /**
   * Creates a new audio format config.
   *
   * @param type audio format type
   * @param sampleRate sample rate in Hz, must be one of 8000/16000/24000/48000
   */
  public OmniRealtimeAudioFormatConfig(OmniRealtimeAudioCodec type, int sampleRate) {
    setType(type);
    setSampleRate(sampleRate);
  }

  /**
   * Creates a new audio format config.
   *
   * @param type "pcm" or "wav"
   * @param sampleRate sample rate in Hz, must be one of 8000/16000/24000/48000
   */
  public OmniRealtimeAudioFormatConfig(String type, int sampleRate) {
    this(OmniRealtimeAudioCodec.fromValue(type), sampleRate);
  }

  public void setType(OmniRealtimeAudioCodec type) {
    this.type = (type == null) ? OmniRealtimeAudioCodec.PCM : type;
  }

  public void setSampleRate(int sampleRate) {
    if (!SUPPORTED_SAMPLE_RATES.contains(sampleRate)) {
      throw new IllegalArgumentException(
          "Unsupported sample rate: "
              + sampleRate
              + ", supported values are: 8000, 16000, 24000, 48000");
    }
    this.sampleRate = sampleRate;
  }

  /**
   * Creates a new audio format config, convenience factory method equivalent to {@code new
   * OmniRealtimeAudioFormatConfig(type, sampleRate)}.
   *
   * @param type "pcm" or "wav"
   * @param sampleRate sample rate in Hz, must be one of 8000/16000/24000/48000
   * @return the created config
   */
  public static OmniRealtimeAudioFormatConfig of(String type, int sampleRate) {
    return new OmniRealtimeAudioFormatConfig(type, sampleRate);
  }

  /**
   * Adds a single extra format parameter for future extension (e.g. {@code addParameter("rate",
   * 1.2)} for speech rate). Merged into the {@code format} node when serialized.
   *
   * @param key parameter name
   * @param value parameter value
   * @return this config for chaining
   */
  public OmniRealtimeAudioFormatConfig addParameter(String key, Object value) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, value);
    return this;
  }
}
