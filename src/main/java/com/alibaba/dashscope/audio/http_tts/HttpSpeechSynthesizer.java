// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.http_tts;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisUsage;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP-based Speech Synthesizer using Server-Sent Events (SSE). This class provides a simple
 * interface for text-to-speech synthesis via HTTP SSE protocol.
 *
 * <p>Supports models like CosyVoice (cosyvoice-v3-flash, etc.) that use HTTP SSE for streaming
 * synthesis.
 *
 * @author songsong.sss
 */
@Slf4j
public class HttpSpeechSynthesizer {

  /** Creates a new HttpSpeechSynthesizer instance with default settings. */
  public HttpSpeechSynthesizer() {}
  /** Creates a per-request ApiServiceOption with the specified SSE setting. */
  private SynchronizeHalfDuplexApi<HttpSpeechSynthesisParam> createApi(boolean isSSE) {
    ApiServiceOption serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.OUT)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.TEXT_TO_SPEECH.getValue())
            .function(Function.SPEECH_SYNTHESIZER.getValue())
            .isSSE(isSSE)
            .build();
    return new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  /**
   * Synchronous call without SSE - returns a result containing the audio URL. This is a simpler,
   * faster call that returns a download URL instead of streaming audio data.
   *
   * <p>Use this method when you want to get the audio URL and download it later.
   *
   * <p>Example response:
   *
   * <pre>{@code
   * {
   *   "request_id": "xxx",
   *   "output": {
   *     "finish_reason": "stop",
   *     "audio": {
   *       "url": "http://dashscope-result-bj.oss-cn-beijing.aliyuncs.com/...",
   *       "id": "audio_xxx",
   *       "expires_at": 1772697707
   *     }
   *   },
   *   "usage": { "characters": 15 }
   * }
   * }</pre>
   *
   * @param param The synthesis parameters
   * @return HttpSpeechSynthesisResult containing audio URL and metadata
   * @throws ApiException If the API call fails
   * @throws NoApiKeyException If the API key is not configured
   * @throws InputRequiredException If required parameters are missing
   */
  public HttpSpeechSynthesisResult call(HttpSpeechSynthesisParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    SynchronizeHalfDuplexApi<HttpSpeechSynthesisParam> api = createApi(false);
    try {
      DashScopeResult result = api.call(param);
      return convertNonSSEResult(result);
    } catch (Exception e) {
      log.error("Synchronous speech synthesis failed", e);
      throw new ApiException(e);
    }
  }

  /**
   * Streaming call with callback interface. Results are delivered through the callback as they
   * arrive.
   *
   * @param param The synthesis parameters
   * @param callback The callback to receive synthesis results
   * @throws ApiException If the API call fails
   * @throws NoApiKeyException If the API key is not configured
   * @throws InputRequiredException If required parameters are missing
   */
  public void streamCall(
      HttpSpeechSynthesisParam param, ResultCallback<HttpSpeechSynthesisResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    SynchronizeHalfDuplexApi<HttpSpeechSynthesisParam> api = createApi(true);

    try {
      api.streamCall(
          param,
          new ResultCallback<DashScopeResult>() {
            @Override
            public void onEvent(DashScopeResult message) {
              try {
                HttpSpeechSynthesisResult result = convertResult(message);
                if (callback != null) {
                  callback.onEvent(result);
                }
              } catch (Exception e) {
                log.error("Failed to process audio result", e);
                if (callback != null) {
                  callback.onError(e);
                }
              }
            }

            @Override
            public void onComplete() {
              if (callback != null) {
                callback.onComplete();
              }
            }

            @Override
            public void onError(Exception e) {
              if (callback != null) {
                callback.onError(e);
              }
            }
          });
    } catch (Exception e) {
      log.error("Streaming call failed", e);
      throw new ApiException(e);
    }
  }

  /**
   * Streaming call that returns audio data as a ByteBuffer. This method blocks until the audio data
   * is fully received.
   *
   * @param param The synthesis parameters
   * @return The accumulated audio data as ByteBuffer
   * @throws ApiException If the API call fails
   * @throws NoApiKeyException If the API key is not configured
   * @throws InputRequiredException If required parameters are missing
   */
  public ByteBuffer callAndReturnAudio(HttpSpeechSynthesisParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {

    ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Exception> callbackError = new AtomicReference<>();

    streamCall(
        param,
        new ResultCallback<HttpSpeechSynthesisResult>() {
          @Override
          public void onEvent(HttpSpeechSynthesisResult result) {
            try {
              if (result.getAudioData() != null) {
                audioBuffer.write(result.getAudioData());
              }
            } catch (Exception e) {
              callbackError.compareAndSet(null, e);
              latch.countDown();
            }
          }

          @Override
          public void onComplete() {
            latch.countDown();
          }

          @Override
          public void onError(Exception e) {
            callbackError.compareAndSet(null, e);
            latch.countDown();
          }
        });

    try {
      boolean completed = latch.await(60 * 10, TimeUnit.SECONDS);
      if (!completed) {
        Status timeoutStatus =
            Status.builder()
                .statusCode(408)
                .code("RequestTimeOut")
                .message("Timeout waiting for audio data from server.")
                .build();
        throw new ApiException(timeoutStatus);
      }

      Exception error = callbackError.get();
      if (error != null) {
        throw (error instanceof ApiException) ? (ApiException) error : new ApiException(error);
      }

      return ByteBuffer.wrap(audioBuffer.toByteArray());
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to get audio data", e);
      throw new ApiException(e);
    }
  }

  /** Converts DashScopeResult to HttpSpeechSynthesisResult. */
  private HttpSpeechSynthesisResult convertResult(DashScopeResult dashScopeResult) {
    // Check for API error response
    if (dashScopeResult.getCode() != null && !dashScopeResult.getCode().isEmpty()) {
      String errorMsg =
          dashScopeResult.getMessage() != null ? dashScopeResult.getMessage() : "Unknown error";
      Status status =
          Status.builder()
              .statusCode(
                  dashScopeResult.getStatusCode() != null ? dashScopeResult.getStatusCode() : 400)
              .code(dashScopeResult.getCode())
              .message(errorMsg)
              .requestId(dashScopeResult.getRequestId())
              .build();
      throw new ApiException(status);
    }

    HttpSpeechSynthesisResult result = new HttpSpeechSynthesisResult();

    if (dashScopeResult.getRequestId() != null) {
      result.setRequestId(dashScopeResult.getRequestId());
    }

    byte[] audioData = extractAudioData(dashScopeResult);
    if (audioData != null) {
      result.setAudioData(audioData);
    }

    if (dashScopeResult.getUsage() != null) {
      try {
        SpeechSynthesisUsage usage =
            JsonUtils.fromJsonObject(
                dashScopeResult.getUsage().getAsJsonObject(), SpeechSynthesisUsage.class);
        result.setUsage(usage);
      } catch (Exception e) {
        log.debug("Failed to parse usage information", e);
      }
    }

    if (dashScopeResult.getOutput() != null && dashScopeResult.getOutput() instanceof JsonObject) {
      result.setOutput((JsonObject) dashScopeResult.getOutput());
    }

    return result;
  }

  /**
   * Extracts audio data from DashScope API response. The audio data is typically Base64-encoded in
   * the response.
   */
  private byte[] extractAudioData(DashScopeResult result) {
    if (result == null) {
      return null;
    }

    // Try to get audio from output (Base64 encoded)
    if (result.getOutput() != null && result.getOutput() instanceof JsonObject) {
      JsonObject output = (JsonObject) result.getOutput();

      // Try common audio field names
      if (output.has("audio")) {
        JsonElement audioElement = output.get("audio");
        // audio could be a Base64 string or a JSON object with data field
        if (audioElement.isJsonPrimitive()) {
          String audioBase64 = audioElement.getAsString();
          if (audioBase64 != null && !audioBase64.isEmpty()) {
            try {
              return Base64.getDecoder().decode(audioBase64);
            } catch (IllegalArgumentException e) {
              log.warn("Failed to decode Base64 audio data", e);
            }
          }
        } else if (audioElement.isJsonObject()) {
          // audio is an object with fields like url, id, data
          JsonObject audioObj = audioElement.getAsJsonObject();
          if (audioObj.has("data") && !audioObj.get("data").isJsonNull()) {
            String audioBase64 = audioObj.get("data").getAsString();
            if (audioBase64 != null && !audioBase64.isEmpty()) {
              try {
                return Base64.getDecoder().decode(audioBase64);
              } catch (IllegalArgumentException e) {
                log.warn("Failed to decode Base64 audio data from audio.data", e);
              }
            }
          }
        }
      }

      // Some APIs may return audio in binary field
      if (output.has("binary")) {
        String binaryBase64 = output.get("binary").getAsString();
        if (binaryBase64 != null && !binaryBase64.isEmpty()) {
          try {
            return Base64.getDecoder().decode(binaryBase64);
          } catch (IllegalArgumentException e) {
            log.warn("Failed to decode Base64 binary data", e);
          }
        }
      }
    }

    // Check if output is ByteBuffer (WebSocket-style)
    if (result.getOutput() instanceof ByteBuffer) {
      ByteBuffer buffer = (ByteBuffer) result.getOutput();
      byte[] data = new byte[buffer.remaining()];
      buffer.get(data);
      return data;
    }

    return null;
  }

  /**
   * Converts DashScopeResult from non-SSE call to HttpSpeechSynthesisResult. Non-SSE call returns
   * audio URL instead of binary data.
   */
  private HttpSpeechSynthesisResult convertNonSSEResult(DashScopeResult dashScopeResult) {
    if (dashScopeResult.getCode() != null && !dashScopeResult.getCode().isEmpty()) {
      String errorMsg =
          dashScopeResult.getMessage() != null ? dashScopeResult.getMessage() : "Unknown error";
      Status status =
          Status.builder()
              .statusCode(
                  dashScopeResult.getStatusCode() != null ? dashScopeResult.getStatusCode() : 400)
              .code(dashScopeResult.getCode())
              .message(errorMsg)
              .requestId(dashScopeResult.getRequestId())
              .build();
      throw new ApiException(status);
    }
    HttpSpeechSynthesisResult result = new HttpSpeechSynthesisResult();

    if (dashScopeResult.getRequestId() != null) {
      result.setRequestId(dashScopeResult.getRequestId());
    }

    // Parse output for audio URL information
    if (dashScopeResult.getOutput() != null && dashScopeResult.getOutput() instanceof JsonObject) {
      JsonObject output = (JsonObject) dashScopeResult.getOutput();
      result.setOutput(output);

      // Parse finish_reason
      if (output.has("finish_reason")) {
        result.setFinishReason(output.get("finish_reason").getAsString());
      }

      // Parse audio object (contains url, id, expires_at)
      if (output.has("audio") && output.get("audio").isJsonObject()) {
        JsonObject audio = output.getAsJsonObject("audio");
        AudioInfo audioInfo = new AudioInfo();

        if (audio.has("url")) {
          audioInfo.setUrl(audio.get("url").getAsString());
        }
        if (audio.has("id")) {
          audioInfo.setId(audio.get("id").getAsString());
        }
        if (audio.has("expires_at")) {
          audioInfo.setExpiresAt(audio.get("expires_at").getAsLong());
        }
        if (audio.has("data") && !audio.get("data").isJsonNull()) {
          audioInfo.setData(audio.get("data").getAsString());
        }

        result.setAudioInfo(audioInfo);
      }
    }

    // Parse usage
    if (dashScopeResult.getUsage() != null) {
      try {
        SpeechSynthesisUsage usage =
            JsonUtils.fromJsonObject(
                dashScopeResult.getUsage().getAsJsonObject(), SpeechSynthesisUsage.class);
        result.setUsage(usage);
      } catch (Exception e) {
        log.debug("Failed to parse usage information", e);
      }
    }

    return result;
  }
}
