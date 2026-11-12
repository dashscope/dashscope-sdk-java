// Copyright (c) Alibaba, Inc. and its affiliates.
import static com.alibaba.dashscope.audio.live.QwenLiveConstants.*;

import com.alibaba.dashscope.audio.live.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class QwenLiveQuickStart {
  private static final int INPUT_BYTES_PER_SECOND = 16000 * 2;
  private static final int FRAME_BYTES = INPUT_BYTES_PER_SECOND / 10;

  public static void main(String[] args) throws Exception {
    if (args.length < 4 || args.length > 6) {
      System.out.println("Usage: QwenLiveQuickStart <workspaceId> <backendModel> <input.pcm> <output.pcm> [idleSeconds=3] [timeoutSeconds=90]");
      System.exit(2);
      return;
    }
    long idleMs = args.length > 4 ? Math.multiplyExact(Long.parseLong(args[4]), 1000) : 3000;
    long timeoutMs = args.length > 5 ? Math.multiplyExact(Long.parseLong(args[5]), 1000) : 90000;
    QwenLiveConnectionParam param = QwenLiveConnectionParam.builder().model("qwen-live").workspaceId(args[0]).build();
    try {
      run(param, args[1], Paths.get(args[2]), Paths.get(args[3]), idleMs, timeoutMs);
      System.out.println("Saved raw PCM: " + args[3] + " (stopped by audio idle window, not a server done event)");
    } catch (Exception failure) {
      failure.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }

  static void run(QwenLiveConnectionParam param, String backendModel, Path input, Path output,
      long idleMs, long timeoutMs) throws Exception {
    if (idleMs <= 0 || timeoutMs <= idleMs) throw new IllegalArgumentException("Require 0 < idle < timeout.");
    QwenLiveSessionConfig config = QwenLiveSessionConfig.builder()
        .model("qwen-audio-realtime").instructions("用简洁中文回答用户的语音问题")
        .inputAudioFormat("pcm").outputAudioFormat("pcm")
        .context(QwenLiveContext.builder().compaction(
            QwenLiveContextCompaction.builder().enabled(false).mode("local").build()).build())
        .delegation(QwenLiveDelegation.builder().type(QwenLiveDelegationType.RESPONSES)
            .responses(QwenLiveResponsesConfig.builder().model(backendModel)
                .instructions("直接回答用户问题，不使用外部工具").build()).build()).build();
    config.getConfig();
    CountDownLatch configured = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    AtomicLong lastAudio = new AtomicLong(Long.MIN_VALUE);
    AtomicBoolean accepting = new AtomicBoolean(true);
    try (OutputStream audioFile = new BufferedOutputStream(
        Files.newOutputStream(output, StandardOpenOption.CREATE_NEW))) {
      QwenLiveConversation conversation = new QwenLiveConversation(param, new QwenLiveCallback() {
        @Override public void onEvent(JsonObject event) {
          synchronized (audioFile) {
            if (!accepting.get()) return;
            try {
              String type = event.has(FIELD_TYPE) ? event.get(FIELD_TYPE).getAsString() : "";
              if (EVENT_SESSION_UPDATED.equals(type)) configured.countDown();
              if (EVENT_SESSION_CLOSED.equals(type)) {
                onError(new IOException("Session ended before audio idle window."));
              }
              if (EVENT_SESSION_OUTPUT_AUDIO_DELTA.equals(type)) {
                JsonElement audio = event.get(FIELD_AUDIO);
                if (audio == null || audio.isJsonNull()) audio = event.get(FIELD_DELTA);
                if (audio == null || audio.isJsonNull()) throw new IOException("Audio event has no audio/delta data.");
                byte[] bytes = Base64.getDecoder().decode(audio.getAsString());
                if (bytes.length > 0) {
                  audioFile.write(bytes);
                  lastAudio.set(System.nanoTime());
                }
              }
            } catch (IOException | RuntimeException e) { onError(e); }
            audioFile.notifyAll();
          }
        }

        @Override public void onClose(int code, String reason) {
          onError(new IOException("WebSocket closed before output settled: " + code));
        }

        @Override public void onError(Throwable cause) {
          synchronized (audioFile) {
            if (!accepting.get()) return;
            failure.compareAndSet(null, cause);
            configured.countDown();
            audioFile.notifyAll();
          }
        }
      });
      try {
        conversation.connect();
        conversation.updateSession(config);
        if (!configured.await(30, TimeUnit.SECONDS)) throw new TimeoutException("No session.updated confirmation.");
        checkFailure(failure);
        streamAudio(conversation, input, failure);
        long inputFinished = System.nanoTime();
        synchronized (audioFile) {
          while (true) {
            checkFailure(failure);
            long now = System.nanoTime();
            boolean receivedAudio = lastAudio.get() != Long.MIN_VALUE;
            long idleLeft = TimeUnit.MILLISECONDS.toNanos(idleMs)
                - (receivedAudio ? Math.min(now - lastAudio.get(), now - inputFinished) : 0);
            long timeoutLeft = TimeUnit.MILLISECONDS.toNanos(timeoutMs) - (now - inputFinished);
            if (receivedAudio && idleLeft <= 0) { accepting.set(false); break; }
            if (timeoutLeft <= 0) throw new TimeoutException("Audio output did not settle; saved PCM may be incomplete.");
            TimeUnit.NANOSECONDS.timedWait(audioFile, receivedAudio ? Math.min(idleLeft, timeoutLeft) : timeoutLeft);
          }
        }
        conversation.close();
      } finally {
        synchronized (audioFile) { accepting.set(false); }
        conversation.cancel();
      }
    }
  }

  private static void streamAudio(QwenLiveConversation conversation, Path input, AtomicReference<Throwable> failure)
      throws Exception {
    byte[] frame = new byte[FRAME_BYTES];
    long started = System.nanoTime();
    long sentBytes = 0;
    try (InputStream in = Files.newInputStream(input)) {
      int read;
      while ((read = in.read(frame)) != -1) {
        checkFailure(failure);
        conversation.appendAudioBytes(frame, 0, read);
        sentBytes += read;
        pace(started, sentBytes);
      }
    }
    byte[] silence = new byte[FRAME_BYTES];
    for (int i = 0; i < 10; i++) {
      checkFailure(failure);
      conversation.appendAudioBytes(silence);
      sentBytes += silence.length;
      pace(started, sentBytes);
    }
  }

  private static void pace(long started, long sentBytes) throws InterruptedException {
    long remaining = sentBytes * 1000000000L / INPUT_BYTES_PER_SECOND - (System.nanoTime() - started);
    if (remaining > 0) TimeUnit.NANOSECONDS.sleep(remaining);
  }

  private static void checkFailure(AtomicReference<Throwable> failure) {
    Throwable cause = failure.get();
    if (cause != null) throw new IllegalStateException("Audio request failed; saved PCM may be incomplete.", cause);
  }
}
