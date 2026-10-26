// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.sse.SseStream;
import com.alibaba.dashscope.v3.types.audio.TtsSynthesizeParams;
import com.alibaba.dashscope.v3.types.common.ResponseStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Explicit HTTP TTS (audio design §8): POST SSE, audio frames as base64 at {@code output.audio}
 * (string or {@code .data}) or {@code output.binary}. Path routed by model name; all speech knobs
 * live inside {@code input} (2.x parity).
 */
public final class TtsHttpService {
  static final String MODULE = "audio";

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(TtsHttpService.class);

  private final HttpTransport transport;

  public TtsHttpService(HttpTransport transport) {
    this.transport = transport;
  }

  /**
   * One-shot synthesis: streams internally and aggregates. Peak memory = the whole audio; prefer
   * {@link #stream} for long text.
   */
  public byte[] synthesize(TtsSynthesizeParams params) {
    return synthesize(params, RequestOptions.NONE);
  }

  public byte[] synthesize(TtsSynthesizeParams params, RequestOptions options) {
    try (ResponseStream<byte[]> stream = stream(params, options)) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      for (byte[] chunk : stream) {
        out.write(chunk, 0, chunk.length);
      }
      return out.toByteArray();
    }
  }

  /** Streaming synthesis; frames carrying no audio (usage-only tails) are skipped. */
  public ResponseStream<byte[]> stream(TtsSynthesizeParams params) {
    return stream(params, RequestOptions.NONE);
  }

  public ResponseStream<byte[]> stream(TtsSynthesizeParams params, RequestOptions options) {
    SseStream sse =
        transport.executeStream(
            AudioModelRouter.httpPath(params.getModel()), buildBody(params), MODULE, options);
    return new ResponseStream<>(
        sse, TtsHttpService::extractAudio, transport.config().isRaiseOnError());
  }

  static ObjectNode buildBody(TtsSynthesizeParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    ObjectNode input = root.putObject("input");
    input.put("text", params.getText());
    if (params.getVoice() != null) {
      input.put("voice", params.getVoice());
    }
    if (params.getFormat() != null) {
      input.put("format", params.getFormat());
    }
    if (params.getSampleRate() != null) {
      input.put("sample_rate", params.getSampleRate());
    }
    if (params.getVolume() != null) {
      input.put("volume", params.getVolume());
    }
    if (params.getRate() != null) {
      input.put("rate", params.getRate());
    }
    if (params.getPitch() != null) {
      input.put("pitch", params.getPitch());
    }
    if (params.getParameters() != null) {
      params.getParameters().forEach((k, v) -> input.set(k, mapper.valueToTree(v)));
    }
    return root;
  }

  /** Base64 audio at output.audio (string or object.data) or output.binary; null when absent. */
  static byte[] extractAudio(JsonNode frame) {
    JsonNode output = frame == null ? null : frame.get("output");
    if (output == null || !output.isObject()) {
      return null;
    }
    JsonNode audio = output.get("audio");
    if (audio != null) {
      if (audio.isTextual()) {
        return decode(audio.asText());
      }
      if (audio.isObject()) {
        JsonNode data = audio.get("data");
        if (data != null && data.isTextual() && !data.asText().isEmpty()) {
          return decode(data.asText());
        }
      }
    }
    JsonNode binary = output.get("binary");
    if (binary != null && binary.isTextual() && !binary.asText().isEmpty()) {
      return decode(binary.asText());
    }
    return null;
  }

  private static byte[] decode(String base64) {
    try {
      return Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      // Usage-only frames legitimately carry no audio, but an undecodable payload is corrupted
      // data — never drop it silently.
      log.warn("dropping tts frame with undecodable base64 audio (length {})", base64.length());
      return null;
    }
  }
}
