// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.http_tts;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Audio information containing URL and metadata for non-SSE synthesis results. When using non-SSE
 * synchronous call, the audio is returned as a URL instead of binary data.
 *
 * @author DashScope SDK Team
 */
@Data
@EqualsAndHashCode
public class AudioInfo {

  /** The audio URL for downloading the synthesized audio file. */
  private String url;

  /** The unique identifier for this audio file. */
  private String id;

  /** The expiration timestamp (Unix timestamp in seconds) for the URL. */
  private Long expiresAt;

  /** The audio data in base64 format (if available). */
  private String data;

  /**
   * Checks if this audio info has a valid URL.
   *
   * @return true if URL is available, false otherwise
   */
  public boolean hasUrl() {
    return url != null && !url.isEmpty();
  }

  /**
   * Checks if the URL has expired.
   *
   * @return true if expired, false if still valid or expiration unknown
   */
  public boolean isExpired() {
    if (expiresAt == null) {
      return false;
    }
    return System.currentTimeMillis() / 1000 > expiresAt;
  }

  /**
   * Gets the remaining time before URL expiration in seconds.
   *
   * @return remaining seconds, or -1 if expiration unknown or already expired
   */
  public long getRemainingSeconds() {
    if (expiresAt == null) {
      return -1;
    }
    long remaining = expiresAt - System.currentTimeMillis() / 1000;
    return remaining > 0 ? remaining : -1;
  }
}
