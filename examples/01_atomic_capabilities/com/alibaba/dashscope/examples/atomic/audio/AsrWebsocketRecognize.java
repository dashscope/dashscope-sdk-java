// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.resources.audio.AudioPacer;
import com.alibaba.dashscope.v3.resources.audio.RecognitionSession;
import com.alibaba.dashscope.v3.types.audio.AsrRecognizeParams;
import com.alibaba.dashscope.v3.types.audio.AsrResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * WebSocket duplex ASR (translation of the Python blueprint's {@code asr_websocket.py}): send PCM
 * from a local WAV while receiving sentences. Default model {@code
 * qwen-audio-3.1-asr-flash-streaming}; input must be mono 16-bit 16 kHz PCM WAV (no resampling).
 * Usage:
 *
 * <pre>
 * mvn -q -f examples/pom.xml exec:java -Dexec.mainClass=com.alibaba.dashscope.examples.atomic.audio.AsrWebsocketRecognize -Dexec.args="speech.wav"
 * </pre>
 */
public final class AsrWebsocketRecognize {

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("usage: AsrWebsocketRecognize <speech.wav> [model]");
      System.exit(2);
    }
    Path wav = Paths.get(args[0]);
    String model = args.length > 1 ? args[1] : "qwen-audio-3.1-asr-flash-streaming";

    AudioCommon.PcmAudio audio = AudioCommon.readWav(wav, 16000);
    int chunkBytes = AudioPacer.bytesPer100ms(audio.sampleRate);

    try (DashScopeClient client = ExampleSupport.client()) {
      RecognitionSession session =
          client
              .audio()
              .asr()
              .recognize(
                  AsrRecognizeParams.builder()
                      .model(model)
                      .format("pcm")
                      .sampleRate(audio.sampleRate)
                      .build());
      ExecutorService receiverPool = Executors.newSingleThreadExecutor();
      try {
        session.start();
        // Receive on a worker thread while the caller thread paces uplink frames.
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
        long start = System.nanoTime();
        int index = 0;
        for (byte[] frame : AudioPacer.pcmChunks(audio.pcm, chunkBytes)) {
          AudioPacer.pace(start, index++);
          session.sendAudioFrame(frame);
        }
        session.stop();
        receiver.get();
      } finally {
        session.close();
        receiverPool.shutdown();
      }
      System.err.println(
          "request_id="
              + session.getRequestId()
              + " first_package_delay_ms="
              + session.getFirstPackageDelay().toMillis()
              + " usage="
              + session.getUsage());
    }
  }
}
