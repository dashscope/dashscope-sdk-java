// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.resources.audio.AudioPacer;
import com.alibaba.dashscope.resources.audio.RealtimeSession;
import com.alibaba.dashscope.types.audio.RealtimeParams;
import com.alibaba.dashscope.types.audio.UpdateSessionParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.TargetDataLine;

/**
 * One Omni 3.8 spoken turn from a WAV file or the microphone, saved as a 24 kHz WAV reply.
 * Translation of the Python blueprint example 01_atomic_capabilities/audio/realtime_omni.py.
 *
 * <p>Manual end-of-turn (commit + createResponse), no server VAD; default voice {@code
 * longanlingxin}. WAV input must be mono 16-bit 16 kHz, 0.1–20 s.
 *
 * <pre>
 * mvn -q -f examples/pom.xml exec:java -Dexec.mainClass=com.alibaba.dashscope.examples.atomic.audio.RealtimeOmniDialog -Dexec.args="--wav input.wav --output reply.wav"
 * </pre>
 */
public final class RealtimeOmniDialog {
  private static final String MODEL = "qwen3.8-omni-flash-realtime";

  public static void main(String[] args) throws Exception {
    Path wav = null;
    Path output = null;
    boolean microphone = false;
    boolean noPlay = false;
    double seconds = 10.0;
    String voice = "longanlingxin";
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
      System.err.println("error: --seconds must be between 0.1 and 20");
      System.exit(2);
    }
    AudioCommon.PcmAudio audio = null;
    if (wav != null) {
      audio = AudioCommon.readWav(wav, 16000);
      double duration = audio.pcm.length / 2.0 / 16000;
      if (duration < 0.1 || duration > 20) {
        System.err.println("error: this example accepts single-turn WAV input of 0.1 to 20 seconds");
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
                .inputAudio(UpdateSessionParams.AudioFormat.of("pcm", 16000))
                .outputAudio(UpdateSessionParams.AudioFormat.of("pcm", 24000))
                .enableInputAudioTranscription(false)
                .enableTurnDetection(false)
                .parameters(Collections.singletonMap("instructions", instructions))
                .build());
        // events() is a single-use stream, so ONE receiver thread consumes it for the whole
        // session: it releases sessionUpdated once the server acknowledges the session update
        // (gating the input below) and finishes at response.done.
        CountDownLatch sessionUpdated = new CountDownLatch(1);
        Future<Integer> receiver =
            pool.submit(
                () -> {
                  try {
                    return receiveReply(session, pcm, sessionUpdated);
                  } finally {
                    // Also release a failed receiver so the sender notices via checkReceiver.
                    sessionUpdated.countDown();
                  }
                });
        if (!sessionUpdated.await(30, TimeUnit.SECONDS)) {
          throw new RuntimeException("No session.updated event received within 30 seconds");
        }
        if (audio != null) {
          sendWav(session, receiver, audio);
        } else {
          sendMicrophone(session, receiver, seconds);
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
      throw new RuntimeException("Response audio is empty or incomplete (" + size + " bytes)");
    }
    AudioCommon.writeWav(output, pcm.toByteArray(), 24000);
    System.out.println(
        "Saved "
            + output.toAbsolutePath()
            + " ("
            + String.format("%.2f", size / 48000.0)
            + " seconds)");
    if (!noPlay) {
      AudioCommon.play(output);
    }
  }

  /**
   * Single-pass consumer of the single-use event stream: audio deltas go to {@code output}, text
   * deltas print inline, {@code sessionUpdated} counts down at {@code session.updated}, and the
   * decoded audio byte count is returned at {@code response.done}.
   */
  static int receiveReply(
      RealtimeSession session, ByteArrayOutputStream output, CountDownLatch sessionUpdated) {
    int size = 0;
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
              "Full response: " + event.path("text").asText(event.path("transcript").asText("")));
          break;
        case "conversation.item.input_audio_transcription.completed":
          System.out.println();
          System.out.println("Input transcript: " + event.path("transcript").asText(""));
          break;
        case "response.done":
          JsonNode response = event.path("response");
          String status = response.path("status").asText(null);
          if (!"completed".equals(status)) {
            throw new RuntimeException("Response did not complete normally: " + status);
          }
          System.out.println();
          System.out.println("response_id: " + response.path("id").asText(null));
          System.out.println("usage: " + response.path("usage"));
          break;
        default:
          break;
      }
      if (kind.equals("session.updated")) {
        sessionUpdated.countDown();
      } else if (kind.equals("response.done")) {
        return size;
      }
    }
    throw new RuntimeException("Connection closed without response.done");
  }

  /** Pace a 16 kHz WAV into the input buffer, one 100 ms chunk at a time. */
  static void sendWav(RealtimeSession session, Future<?> receiver, AudioCommon.PcmAudio audio)
      throws Exception {
    int chunkBytes = AudioPacer.bytesPer100ms(16000);
    long start = System.nanoTime();
    int index = 0;
    int sent = 0;
    for (byte[] frame : AudioPacer.pcmChunks(audio.pcm, chunkBytes)) {
      checkReceiver(receiver);
      AudioPacer.pace(start, index++);
      session.appendAudio(frame);
      sent += frame.length;
    }
    if (sent < 3200) {
      throw new IllegalArgumentException("This turn requires at least 0.1 seconds of audio");
    }
  }

  /** Capture the microphone for {@code seconds} and append each 100 ms block. */
  static void sendMicrophone(RealtimeSession session, Future<?> receiver, double seconds)
      throws Exception {
    TargetDataLine line = AudioCommon.microphone(16000);
    int sent = 0;
    try {
      System.out.println(
          "Recording for " + seconds + " seconds; press Ctrl+C to end this turn early.");
      long remaining = Math.round(seconds * 16000);
      while (remaining > 0) {
        checkReceiver(receiver);
        int count = (int) Math.min(1600, remaining);
        byte[] frame = new byte[count * 2];
        int offset = 0;
        while (offset < frame.length) {
          int n = line.read(frame, offset, frame.length - offset);
          if (n <= 0) {
            break;
          }
          offset += n;
        }
        if (offset != frame.length) {
          throw new RuntimeException("Microphone returned an incomplete PCM frame");
        }
        session.appendAudio(frame);
        sent += frame.length;
        remaining -= count;
      }
    } finally {
      line.stop();
      line.close();
    }
    if (sent < 3200) {
      throw new IllegalArgumentException("This turn requires at least 0.1 seconds of audio");
    }
  }

  /** Surface a receiver failure as soon as the send side notices it exited early. */
  static void checkReceiver(Future<?> receiver) throws Exception {
    if (receiver.isDone()) {
      receiver.get();
      throw new RuntimeException("Receiver thread exited before input ended");
    }
  }

  private static void usage() {
    System.err.println(
        "usage: RealtimeOmniDialog (--wav <in.wav> | --microphone [--seconds 10]) --output"
            + " <reply.wav> [--voice v] [--instructions s] [--no-play]");
    System.exit(2);
  }
}
