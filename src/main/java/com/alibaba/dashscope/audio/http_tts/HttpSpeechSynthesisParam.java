// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.http_tts;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * HTTP TTS (Text-to-Speech) synthesis parameter class. Supports HTTP SSE-based speech synthesis API
 * calls for models like CosyVoice.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * HttpSpeechSynthesisParam param = HttpSpeechSynthesisParam.builder()
 *     .model("cosyvoice-v3-flash")
 *     .text("你好，欢迎使用语音合成服务。")
 *     .voice("longanyang")
 *     .format("wav")
 *     .sampleRate(24000)
 *     .build();
 * }</pre>
 *
 * @author DashScope SDK Team
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class HttpSpeechSynthesisParam extends HalfDuplexServiceParam {

  /** The text to be synthesized into speech. */
  @NonNull private String text;

  /** The voice name for synthesis (e.g., "longanyang", "longxiaochun"). */
  private String voice;

  /** The audio format (e.g., "wav", "mp3", "pcm"). */
  @Builder.Default private String format = "wav";

  /** The sample rate in Hz (e.g., 8000, 16000, 24000, 48000). */
  @Builder.Default private Integer sampleRate = 16000;

  /** The audio volume (0-100). */
  @Builder.Default private Integer volume = 50;

  /** The speech rate (0.5-2.0). */
  @Builder.Default private Float rate = 1.0f;

  /** The pitch rate (0.5-2.0). */
  @Builder.Default private Float pitch = 1.0f;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("model", getModel());

    // Build input object
    JsonObject input = new JsonObject();
    input.addProperty("text", text);

    if (voice != null && !voice.isEmpty()) {
      input.addProperty("voice", voice);
    }
    if (format != null && !format.isEmpty()) {
      input.addProperty("format", format);
    }
    if (sampleRate != null) {
      input.addProperty("sample_rate", sampleRate);
    }
    if (volume != null) {
      input.addProperty("volume", volume);
    }
    if (rate != null) {
      input.addProperty("rate", rate);
    }
    if (pitch != null) {
      input.addProperty("pitch", pitch);
    }
    if (parameters != null && !parameters.isEmpty()) {
      JsonUtils.merge(input, JsonUtils.parametersToJsonObject(parameters));
    }

    body.add("input", input);

    return body;
  }

  @Override
  public Object getInput() {
    JsonObject input = new JsonObject();
    input.addProperty("text", text);
    return input;
  }

  @Override
  public ByteBuffer getBinaryData() {
    return null;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (text == null || text.trim().isEmpty()) {
      throw new InputRequiredException("text is required and cannot be empty");
    }
    if (getModel() == null || getModel().trim().isEmpty()) {
      throw new InputRequiredException("model is required");
    }
  }
}
