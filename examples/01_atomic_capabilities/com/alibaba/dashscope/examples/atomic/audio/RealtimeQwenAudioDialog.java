// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.resources.audio.RealtimeSession;
import com.alibaba.dashscope.v3.types.audio.RealtimeParams;
import com.alibaba.dashscope.v3.types.audio.UpdateSessionParams;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * One Qwen-Audio realtime spoken turn from a WAV file or the microphone, saved as a 24 kHz WAV
 * reply. Translation of the Python blueprint example
 * 01_atomic_capabilities/audio/realtime_qwen_audio.py.
 *
 * <p>Fixed model {@code qwen-audio-3.0-realtime-plus}, default voice {@code longanqian}; manual
 * end-of-turn (commit + createResponse). Unlike the omni example this dialect uses the legacy flat
 * {@code input_audio_format}/{@code output_audio_format} session fields. WAV input must be mono
 * 16-bit 16 kHz, 0.1–20 s.
 *
 * <pre>
 * mvn -q -f examples/pom.xml exec:java -Dexec.mainClass=com.alibaba.dashscope.examples.atomic.audio.RealtimeQwenAudioDialog -Dexec.args="--wav input.wav --output reply.wav"
 * </pre>
 */
public final class RealtimeQwenAudioDialog {
  private static final String MODEL = "qwen-audio-3.0-realtime-plus";

  public static void main(String[] args) throws Exception {
    Path wav = null;
    Path output = null;
    boolean microphone = false;
    boolean noPlay = false;
    double seconds = 10.0;
    String voice = "longanqian";
    String instructions = "请用简洁自然的中文回答。";
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
        case "--voice":
          voice = args[++i];
          break;
        case "--instructions":
          instructions = args[++i];
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
    if (microphone && (seconds < 0.1 || seconds > 20)) {
      System.err.println("error: --seconds 应在 0.1～20 之间");
      System.exit(2);
    }
    AudioCommon.PcmAudio audio = null;
    if (wav != null) {
      audio = AudioCommon.readWav(wav, 16000);
      double duration = audio.pcm.length / 2.0 / 16000;
      if (duration < 0.1 || duration > 20) {
        System.err.println("error: 本示例的单轮 WAV 输入限制为 0.1～20 秒");
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
                .voice(voice)
                .inputAudioFormat("pcm")
                .outputAudioFormat("pcm")
                .enableTurnDetection(false)
                .parameters(Collections.singletonMap("instructions", instructions))
                .build());
        RealtimeOmniDialog.receiveUntil(session, "session.updated", null);
        Future<Integer> receiver =
            pool.submit(() -> RealtimeOmniDialog.receiveUntil(session, "response.done", pcm));
        if (audio != null) {
          RealtimeOmniDialog.sendWav(session, receiver, audio);
        } else {
          RealtimeOmniDialog.sendMicrophone(session, receiver, seconds);
        }
        session.commit();
        session.createResponse();
        size = receiver.get();
      } finally {
        session.close();
        pool.shutdown();
      }
    }
    if (size == 0 || size % 2 != 0) {
      throw new RuntimeException("回答音频为空或不完整（" + size + " 字节）");
    }
    AudioCommon.writeWav(output, pcm.toByteArray(), 24000);
    System.out.println(
        "已保存 " + output.toAbsolutePath() + "（" + String.format("%.2f", size / 48000.0) + " 秒）");
    if (!noPlay) {
      AudioCommon.play(output);
    }
  }

  private static void usage() {
    System.err.println(
        "usage: RealtimeQwenAudioDialog (--wav <in.wav> | --microphone [--seconds 10]) --output"
            + " <reply.wav> [--voice v] [--instructions s] [--no-play]");
    System.exit(2);
  }
}
