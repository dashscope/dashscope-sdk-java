// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.resources.audio.RealtimeSession;
import com.alibaba.dashscope.types.audio.RealtimeParams;
import com.alibaba.dashscope.types.audio.UpdateSessionParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Translate speech with Qwen3.8-LiveTranslate, saving the spoken translation as a 24 kHz WAV.
 * Translation of the Python blueprint example 01_atomic_capabilities/audio/realtime_translation.py.
 *
 * <p>Server-side turn detection stays enabled: no commit/createResponse; {@code endSession()} at
 * input end, and the receiver drains until {@code session.finished} (not the first {@code
 * response.done}) so every translation segment is collected. Default target language {@code en}.
 *
 * <pre>
 * mvn -q -f examples/pom.xml exec:java -Dexec.mainClass=com.alibaba.dashscope.examples.atomic.audio.RealtimeTranslation -Dexec.args="--wav input.wav --language en --output translation.wav"
 * </pre>
 */
public final class RealtimeTranslation {
  private static final String MODEL = "qwen3.8-livetranslate-flash-realtime";

  public static void main(String[] args) throws Exception {
    Path wav = null;
    Path output = null;
    boolean microphone = false;
    boolean noPlay = false;
    double seconds = 10.0;
    String language = "en";
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--wav":
          wav = Paths.get(args[++i]);
          break;
        case "--microphone":
          microphone = true;
          break;
        case "--seconds":
          seconds = Double.parseDouble(args[++i]);
          break;
        case "--language":
          language = args[++i];
          break;
        case "--output":
          output = Paths.get(args[++i]);
          break;
        case "--no-play":
          noPlay = true;
          break;
        default:
          System.err.println("unknown argument: " + args[i]);
          usage();
      }
    }
    if (output == null || (wav == null && !microphone) || (wav != null && microphone)) {
      usage();
    }
    if (language.trim().isEmpty()) {
      System.err.println("error: --language 需要非空目标语种代码");
      System.exit(2);
    }
    if (microphone && (seconds < 0.1 || seconds > 20)) {
      System.err.println("error: --seconds 应在 0.1～20 之间");
      System.exit(2);
    }
    AudioCommon.PcmAudio audio = null;
    if (wav != null) {
      audio = AudioCommon.readWav(wav, 16000);
      double duration = audio.pcm.length / 2.0 / 16000;
      if (duration < 0.1 || duration > 20) {
        System.err.println("error: 本示例的 WAV 输入限制为 0.1～20 秒");
        System.exit(2);
      }
    }

    ByteArrayOutputStream pcm = new ByteArrayOutputStream();
    int size;
    try (DashScopeClient client = ExampleSupport.client()) {
      RealtimeSession session =
          client.audio().realtime().connect(RealtimeParams.builder().model(MODEL).build());
      ExecutorService pool = Executors.newSingleThreadExecutor();
      try {
        session.connect();
        session.updateSession(
            UpdateSessionParams.builder()
                .modalities(Arrays.asList("text", "audio"))
                .translation(UpdateSessionParams.Translation.of(language))
                .build());
        // events() is a single-use stream, so ONE receiver thread drains it for the whole
        // session: it releases sessionUpdated once the server acknowledges the session update
        // (gating the input below) and finishes at session.finished — every translation segment
        // is collected, not just the first response.done.
        CountDownLatch sessionUpdated = new CountDownLatch(1);
        Future<Integer> receiver =
            pool.submit(
                () -> {
                  try {
                    return drainTranslation(session, pcm, sessionUpdated);
                  } finally {
                    sessionUpdated.countDown();
                  }
                });
        if (!sessionUpdated.await(30, TimeUnit.SECONDS)) {
          throw new RuntimeException("30 秒内未收到 session.updated 事件");
        }
        if (audio != null) {
          RealtimeOmniDialog.sendWav(session, receiver, audio);
        } else {
          RealtimeOmniDialog.sendMicrophone(session, receiver, seconds);
        }
        session.endSession();
        size = receiver.get();
      } finally {
        session.close();
        pool.shutdown();
      }
    }
    if (size == 0 || size % 2 != 0) {
      throw new RuntimeException("翻译音频为空或不完整（" + size + " 字节）");
    }
    AudioCommon.writeWav(output, pcm.toByteArray(), 24000);
    System.out.println(
        "已保存 " + output.toAbsolutePath() + "（" + String.format("%.2f", size / 48000.0) + " 秒）");
    if (!noPlay) {
      AudioCommon.play(output);
    }
  }

  /**
   * Single-pass consumer of the single-use event stream: translation audio deltas go to {@code
   * output}, {@code sessionUpdated} counts down at {@code session.updated}, and the decoded audio
   * byte count is returned at {@code session.finished} (after at least one completed response).
   * Prints the source transcription and translated text.
   */
  private static int drainTranslation(
      RealtimeSession session, ByteArrayOutputStream output, CountDownLatch sessionUpdated) {
    int size = 0;
    int responses = 0;
    for (JsonNode event : session.events()) {
      String kind = event.path("type").asText();
      switch (kind) {
        case "response.audio.delta":
          if (output != null) {
            byte[] audio = Base64.getDecoder().decode(event.path("delta").asText());
            output.write(audio, 0, audio.length);
            size += audio.length;
          }
          break;
        case "response.text.delta":
        case "response.audio_transcript.delta":
          System.out.print(event.path("delta").asText());
          System.out.flush();
          break;
        case "response.text.done":
        case "response.audio_transcript.done":
          System.out.println();
          System.out.println(
              "译文： " + event.path("text").asText(event.path("transcript").asText("")));
          break;
        case "conversation.item.input_audio_transcription.completed":
          System.out.println();
          System.out.println("原文： " + event.path("transcript").asText(""));
          break;
        case "response.done":
          JsonNode response = event.path("response");
          String status = response.path("status").asText(null);
          if (status != null && !"completed".equals(status)) {
            throw new RuntimeException("翻译未正常完成：" + status);
          }
          responses++;
          System.out.println();
          System.out.println("response_id: " + response.path("id").asText(null));
          System.out.println("usage: " + response.path("usage"));
          break;
        default:
          break;
      }
      if (kind.equals("session.updated")) {
        sessionUpdated.countDown();
      } else if (kind.equals("session.finished")) {
        if (responses == 0) {
          throw new RuntimeException("会话结束，但未收到翻译响应结束事件");
        }
        return size;
      }
    }
    throw new RuntimeException("连接已关闭，未收到 session.finished");
  }

  private static void usage() {
    System.err.println(
        "usage: RealtimeTranslation (--wav <in.wav> | --microphone [--seconds 10]) --output"
            + " <translation.wav> [--language en] [--no-play]");
    System.exit(2);
  }
}
