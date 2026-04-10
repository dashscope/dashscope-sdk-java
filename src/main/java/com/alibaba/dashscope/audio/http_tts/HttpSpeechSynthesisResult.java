// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.http_tts;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisUsage;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Result class for HTTP TTS synthesis. Contains the synthesized audio data and related metadata.
 *
 * <p>For SSE streaming calls, the result contains binary audio data in {@link #audioData}. For
 * non-SSE synchronous calls, the result contains an audio URL in {@link #audioInfo}.
 *
 * @author DashScope SDK Team
 */
@Data
@EqualsAndHashCode
public class HttpSpeechSynthesisResult {

  /** The request ID for tracking. */
  private String requestId;

  /** The audio data in binary format (for SSE streaming calls). */
  private byte[] audioData;

  /** The audio URL and metadata (for non-SSE synchronous calls). */
  private AudioInfo audioInfo;

  /** The usage statistics (if available). */
  private SpeechSynthesisUsage usage;

  /** The raw output from the API (may contain additional metadata). */
  private JsonObject output;

  /** The finish reason (e.g., "stop"). */
  private String finishReason;

  /**
   * Checks if audio data is present in this result (SSE mode).
   *
   * @return true if audio data is available, false otherwise
   */
  public boolean hasAudioData() {
    return audioData != null && audioData.length > 0;
  }

  /**
   * Gets the size of the audio data in bytes.
   *
   * @return the size in bytes, or 0 if no audio data is present
   */
  public int getAudioDataSize() {
    return audioData != null ? audioData.length : 0;
  }

  /**
   * Checks if audio URL is present in this result (non-SSE mode).
   *
   * @return true if audio URL is available, false otherwise
   */
  public boolean hasAudioUrl() {
    return audioInfo != null && audioInfo.hasUrl();
  }

  /**
   * Gets the audio URL.
   *
   * @return the audio URL, or null if not available
   */
  public String getAudioUrl() {
    return audioInfo != null ? audioInfo.getUrl() : null;
  }
}
