// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.audio.TtsSynthesizeParams;
import com.alibaba.dashscope.types.common.ResponseStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * HTTP SSE TTS: stream decoded PCM chunks, accumulate them into one WAV at 24000 Hz, save after the
 * stream completes, then play (unless {@code --no-play}). Default model {@code
 * qwen-audio-3.1-tts-flash} with voice {@code longanhuan_v3.1}. A real request may incur charges.
 * The SDK's {@code byte[]} stream skips frames that carry no audio (usage-only tails), so the
 * finish flag and requestId/usage of the Python original are not observable here; an in-stream
 * business error still surfaces as an exception. Usage:
 *
 * <pre>TtsHttpStreamSynthesize &lt;text&gt; &lt;output.wav&gt; [--model M] [--voice V] [--no-play]
 * </pre>
 *
 * Translation of the Python blueprint example 01_atomic_capabilities/audio/tts_http_stream.py.
 */
public final class TtsHttpStreamSynthesize {

  private static final int SAMPLE_RATE = 24000;

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      usage();
      System.exit(2);
    }
    String text = args[0];
    Path output = Paths.get(args[1]);
    String model = "qwen-audio-3.1-tts-flash";
    String voice = "longanhuan_v3.1";
    boolean play = true;
    for (int i = 2; i < args.length; i++) {
      switch (args[i]) {
        case "--model":
          model = value(args, ++i);
          break;
        case "--voice":
          voice = value(args, ++i);
          break;
        case "--no-play":
          play = false;
          break;
        default:
          usage();
          System.exit(2);
      }
    }
    if (text.trim().isEmpty()) {
      System.err.println("error: text must not be empty or blank");
      System.exit(2);
    }

    ByteArrayOutputStream pcm = new ByteArrayOutputStream();
    try (DashScopeClient client = ExampleSupport.client();
        ResponseStream<byte[]> stream =
            client.audio().tts().http().stream(
                TtsSynthesizeParams.builder()
                    .model(model)
                    .voice(voice)
                    .text(text)
                    .format("wav")
                    .sampleRate(SAMPLE_RATE)
                    .build())) {
      for (byte[] chunk : stream) {
        pcm.write(chunk, 0, chunk.length);
      }
    }
    byte[] audio = pcm.toByteArray();
    if (audio.length >= 12
        && audio[0] == 'R'
        && audio[1] == 'I'
        && audio[2] == 'F'
        && audio[3] == 'F'
        && audio[8] == 'W'
        && audio[9] == 'A'
        && audio[10] == 'V'
        && audio[11] == 'E') {
      // The service returned a RIFF/WAVE container instead of raw PCM; refuse to wrap it twice.
      throw new IllegalStateException("service returned a WAV container, not raw PCM");
    }
    if (audio.length == 0 || audio.length % 2 != 0) {
      throw new IllegalStateException(
          "returned PCM audio is empty or not an even number of bytes (" + audio.length + ")");
    }
    // Never overwrites an existing file (AudioCommon.writeWav enforces this).
    AudioCommon.writeWav(output, audio, SAMPLE_RATE);
    System.out.printf(
        "Saved %s (%d bytes PCM, %.2f seconds)%n",
        output.toAbsolutePath(), audio.length, audio.length / 2.0 / SAMPLE_RATE);
    if (play) {
      AudioCommon.play(output);
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
        "usage: TtsHttpStreamSynthesize <text> <output.wav> [--model M] [--voice V] [--no-play]");
  }
}
