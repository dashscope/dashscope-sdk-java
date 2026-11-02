// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.audio.TtsSynthesizeParams;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * HTTP one-shot TTS: synthesize the whole text in one call, wrap the returned raw PCM into one WAV,
 * save it, then play it (unless {@code --no-play}). Default model {@code qwen-audio-3.1-tts-flash}
 * with voice {@code longanhuan_v3.1} at 24000 Hz. A real request may incur charges. The one-shot
 * aggregate holds the whole audio in memory; prefer the streaming variant for long text. Usage:
 *
 * <pre>TtsHttpSynthesize &lt;text&gt; &lt;output.wav&gt; [--model M] [--voice V] [--no-play]</pre>
 *
 * Translation of the Python blueprint example 01_atomic_capabilities/audio/tts_http.py.
 */
public final class TtsHttpSynthesize {

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

    byte[] pcm;
    try (DashScopeClient client = ExampleSupport.client()) {
      // The aggregated byte[] result carries no requestId/usage; only the streaming variants
      // expose per-frame envelopes.
      pcm =
          client
              .audio()
              .tts()
              .http()
              .synthesize(
                  TtsSynthesizeParams.builder()
                      .model(model)
                      .voice(voice)
                      .text(text)
                      // Request raw PCM: this example wraps it into a WAV itself below.
                      .format("pcm")
                      .sampleRate(SAMPLE_RATE)
                      .build());
    }
    if (pcm.length >= 12
        && pcm[0] == 'R'
        && pcm[1] == 'I'
        && pcm[2] == 'F'
        && pcm[3] == 'F'
        && pcm[8] == 'W'
        && pcm[9] == 'A'
        && pcm[10] == 'V'
        && pcm[11] == 'E') {
      // The service returned a RIFF/WAVE container instead of raw PCM; refuse to wrap it twice.
      throw new IllegalStateException("service returned a WAV container, not raw PCM");
    }
    if (pcm.length == 0 || pcm.length % 2 != 0) {
      throw new IllegalStateException(
          "returned PCM audio is empty or not an even number of bytes (" + pcm.length + ")");
    }
    // Never overwrites an existing file (AudioCommon.writeWav enforces this).
    AudioCommon.writeWav(output, pcm, SAMPLE_RATE);
    System.out.printf(
        "Saved %s (%d bytes PCM, %.2f seconds)%n",
        output.toAbsolutePath(), pcm.length, pcm.length / 2.0 / SAMPLE_RATE);
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
        "usage: TtsHttpSynthesize <text> <output.wav> [--model M] [--voice V] [--no-play]");
  }
}
