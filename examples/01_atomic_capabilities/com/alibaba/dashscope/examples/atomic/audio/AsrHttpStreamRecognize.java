// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.types.common.ResponseStream;
import com.alibaba.dashscope.v3.types.common.Usage;
import com.alibaba.dashscope.v3.types.multimodal.ContentPart;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChatChunk;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChatCreateParams;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChoice;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalMessage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * REST ASR: submit complete WAV audio once and receive cumulative transcripts over SSE. The Python
 * original POSTs {@code /services/aigc/multimodal-generation/generation} through the raw transport;
 * Java 3.0 has no raw transport API, so the SAME endpoint semantics are expressed as {@code
 * client.multimodal().chat().createStreaming(...)} with model {@code qwen-audio-3.1-asr-flash}, one
 * audio content part, and the ASR knobs ({@code format}, {@code sample_rate}) carried in the {@code
 * parameters} passthrough. A local file is uploaded by the SDK's OSS preprocessing (an {@code
 * oss://} URL plus the resolve header) instead of Python's base64 data URI; an {@code --url} value
 * is sent as-is. This is NOT realtime audio input: the service officially returns batched results
 * only for audio of at least 60 seconds, so the first SSE event of a long file may be slow. Usage:
 *
 * <pre>
 * AsrHttpStreamRecognize (--file &lt;speech.wav&gt; | --url &lt;https audio url&gt;) [--model M]
 * </pre>
 *
 * Translation of the Python blueprint example 01_atomic_capabilities/audio/asr_http_stream.py.
 */
public final class AsrHttpStreamRecognize {

  public static void main(String[] args) throws Exception {
    Path file = null;
    String url = null;
    String model = "qwen-audio-3.1-asr-flash";
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--file":
          file = Paths.get(value(args, ++i));
          break;
        case "--url":
          url = value(args, ++i);
          break;
        case "--model":
          model = value(args, ++i);
          break;
        default:
          usage();
          System.exit(2);
      }
    }
    if ((file == null) == (url == null)) {
      usage();
      System.exit(2);
    }

    String audioRef;
    if (file != null) {
      // This sample only sends 16 kHz mono 16-bit PCM WAV and never resamples; validate before
      // the (paid) request. The preprocessor uploads the local path to OSS.
      AudioCommon.readWav(file, 16000);
      audioRef = file.toString();
    } else {
      if (!url.startsWith("https://")) {
        System.err.println("error: --url must be a service-reachable HTTPS audio URL");
        System.exit(2);
      }
      audioRef = url;
    }

    Map<String, Object> asrParameters = new HashMap<>();
    asrParameters.put("format", "wav");
    asrParameters.put("sample_rate", "16000");
    MultimodalChatCreateParams params =
        MultimodalChatCreateParams.builder()
            .model(model)
            .messages(
                Collections.singletonList(
                    MultimodalMessage.user(Collections.singletonList(ContentPart.audio(audioRef)))))
            // Same wire body as the Python original: no incremental_output flag; this model
            // answers with cumulative output.text frames (each may revise an earlier partial).
            .parameters(asrParameters)
            .build();

    try (DashScopeClient client = ExampleSupport.client();
        ResponseStream<MultimodalChatChunk> stream =
            client.multimodal().chat().createStreaming(params)) {
      int count = 0;
      String requestId = null;
      Usage usage = null;
      for (MultimodalChatChunk chunk : stream) {
        if (chunk.getRequestId() != null) {
          requestId = chunk.getRequestId();
        }
        if (chunk.getUsage() != null) {
          usage = chunk.getUsage();
        }
        MultimodalChoice choice = chunk.firstChoice();
        String text =
            choice == null || choice.getMessage() == null ? "" : choice.getMessage().text();
        if (text.isEmpty()) {
          continue;
        }
        count++;
        System.out.println("[cumulative " + count + "] " + text);
      }
      if (count == 0) {
        throw new IllegalStateException("HTTP stream ended without any output text");
      }
      System.err.println(
          "request_id="
              + requestId
              + " text_events="
              + count
              + (usage == null
                  ? ""
                  : " usage(input/output/total)="
                      + usage.getInputTokens()
                      + "/"
                      + usage.getOutputTokens()
                      + "/"
                      + usage.getTotalTokens()
                      + usage.getExtra()));
    }
  }

  private static String value(String[] args, int i) {
    if (i >= args.length) {
      usage();
      System.exit(2);
    }
    return args[i];
  }

  private static void usage() {
    System.err.println(
        "usage: AsrHttpStreamRecognize (--file <speech.wav> | --url <https audio url>) [--model"
            + " M]");
  }
}
