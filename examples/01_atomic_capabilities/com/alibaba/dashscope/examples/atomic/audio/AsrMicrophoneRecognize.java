// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.resources.audio.RecognitionSession;
import com.alibaba.dashscope.v3.types.audio.AsrRecognizeParams;
import com.alibaba.dashscope.v3.types.audio.AsrResult;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sound.sampled.TargetDataLine;

/**
 * Capture the system microphone and stream PCM to streaming ASR in the caller thread. Translation
 * of the Python blueprint example 01_atomic_capabilities/audio/asr_microphone.py.
 *
 * <p>Capture uses blocking 100 ms mono 16-bit 16 kHz reads; a receiver thread prints partial
 * sentences to stderr and final sentences to stdout. {@code --seconds N} bounds the capture window
 * (default 5); {@code --recording take.wav} additionally saves the captured PCM.
 *
 * <pre>mvn -q -f examples/pom.xml exec:java -Dexec.mainClass=com.alibaba.dashscope.examples.atomic.audio.AsrMicrophoneRecognize -Dexec.args="--seconds 10 --recording take.wav"</pre>
 */
public final class AsrMicrophoneRecognize {

  public static void main(String[] args) throws Exception {
    double seconds = 5.0;
    String model = "qwen-audio-3.1-asr-flash-streaming";
    Path recording = null;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--seconds":
          seconds = Double.parseDouble(args[++i]);
          break;
        case "--model":
          model = args[++i];
          break;
        case "--recording":
          recording = Path.of(args[++i]);
          break;
        default:
          System.err.println("unknown argument: " + args[i]);
          usage();
      }
    }
    if (seconds <= 0) {
      System.err.println("error: --seconds must be positive");
      System.exit(2);
    }
    int sampleRate = 16000;
    TargetDataLine line = AudioCommon.microphone(sampleRate);

    try (DashScopeClient client = ExampleSupport.client()) {
      RecognitionSession session =
          client
              .audio()
              .asr()
              .recognize(
                  AsrRecognizeParams.builder()
                      .model(model)
                      .format("pcm")
                      .sampleRate(sampleRate)
                      .build());
      ExecutorService receiverPool = Executors.newSingleThreadExecutor();
      ByteArrayOutputStream captured = new ByteArrayOutputStream();
      try {
        session.start();
        // Receive on a worker thread while the caller thread captures and sends mic frames.
        Future<?> receiver =
            receiverPool.submit(
                () -> {
                  for (AsrResult result : session.results()) {
                    AsrResult.Sentence sentence = result.getSentence();
                    if (sentence == null || sentence.getText() == null) {
                      continue;
                    }
                    if (Boolean.TRUE.equals(sentence.getSentenceEnd())) {
                      System.out.println(sentence.getText());
                    } else {
                      System.err.println("[partial] " + sentence.getText());
                    }
                  }
                });
        System.err.println(
            "Recording up to " + seconds + "s; Ctrl+C stops capture and drains ASR.");
        long remaining = Math.round(seconds * sampleRate);
        while (remaining > 0) {
          checkReceiver(receiver);
          int count = (int) Math.min(sampleRate / 10, remaining);
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
            throw new RuntimeException("Microphone returned an incomplete PCM frame.");
          }
          if (recording != null) {
            captured.write(frame, 0, frame.length);
          }
          session.sendAudioFrame(frame);
          remaining -= count;
        }
        session.stop();
        receiver.get();
      } finally {
        session.close();
        receiverPool.shutdown();
        line.stop();
        line.close();
      }
      System.err.println(
          "request_id="
              + session.getRequestId()
              + " first_package_delay_ms="
              + millis(session.getFirstPackageDelay())
              + " usage="
              + session.getUsage());
      if (recording != null) {
        AudioCommon.writeWav(recording, captured.toByteArray(), sampleRate);
        System.err.println("recording saved: " + recording.toAbsolutePath());
      }
    }
  }

  /** Surface a receiver failure as soon as the capture loop notices it exited early. */
  private static void checkReceiver(Future<?> receiver) throws Exception {
    if (receiver.isDone()) {
      receiver.get();
      throw new RuntimeException("Capture not finished but the receiver thread exited.");
    }
  }

  private static String millis(Duration duration) {
    return duration == null ? "null" : Long.toString(duration.toMillis());
  }

  private static void usage() {
    System.err.println(
        "usage: AsrMicrophoneRecognize [--seconds 5] [--model m] [--recording <take.wav>]");
    System.exit(2);
  }
}
