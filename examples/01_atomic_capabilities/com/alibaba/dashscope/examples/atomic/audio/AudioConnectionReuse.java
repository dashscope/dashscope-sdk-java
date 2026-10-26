// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.resources.audio.AudioPacer;
import com.alibaba.dashscope.resources.audio.RecognitionSession;
import com.alibaba.dashscope.resources.audio.SynthesisSession;
import com.alibaba.dashscope.transport.ws.WsConnection;
import com.alibaba.dashscope.types.audio.AsrRecognizeParams;
import com.alibaba.dashscope.types.audio.AsrResult;
import com.alibaba.dashscope.types.audio.TtsConnectParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Run five sequential ASR or TTS tasks on one shared WebSocket connection. Translation of the
 * Python blueprint example 01_atomic_capabilities/audio/connection_reuse.py.
 *
 * <p>ASR and TTS run separately (subcommand {@code asr}/{@code tts}), each on its own connection;
 * no automatic reconnect. Per-round summary lines are printed as JSON with the request id.
 *
 * <pre>
 * mvn -q -f examples/pom.xml exec:java -Dexec.mainClass=com.alibaba.dashscope.examples.atomic.audio.AudioConnectionReuse -Dexec.args="asr speech.wav --output-dir reuse-asr"
 * </pre>
 */
public final class AudioConnectionReuse {
  private static final int RUNS = 5;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    if (args.length < 1 || (!args[0].equals("asr") && !args[0].equals("tts"))) {
      usage();
    }
    String kind = args[0];
    String model =
        "asr".equals(kind) ? "qwen-audio-3.1-asr-flash-streaming" : "qwen-audio-3.1-tts-flash";
    String voice = "longanhuan_v3.1";
    String text = "你好，这是连接复用测试。";
    Path wav = null;
    Path outputDir = null;
    for (int i = 1; i < args.length; i++) {
      switch (args[i]) {
        case "--model":
          model = args[++i];
          break;
        case "--voice":
          voice = args[++i];
          break;
        case "--text":
          text = args[++i];
          break;
        case "--output-dir":
          outputDir = Paths.get(args[++i]);
          break;
        default:
          if ("asr".equals(kind) && wav == null && !args[i].startsWith("--")) {
            wav = Paths.get(args[i]);
          } else {
            System.err.println("unknown argument: " + args[i]);
            usage();
          }
      }
    }
    if (outputDir == null || ("asr".equals(kind) && wav == null)) {
      usage();
    }
    if ("tts".equals(kind) && text.trim().isEmpty()) {
      System.err.println("error: --text 不能为空");
      System.exit(2);
    }
    // New output directory; never overwrite an existing one (Python mkdir() parity).
    Files.createDirectory(outputDir);

    AudioCommon.PcmAudio audio = "asr".equals(kind) ? AudioCommon.readWav(wav, 16000) : null;
    try (DashScopeClient client = ExampleSupport.client();
        WsConnection connection = client.wsConnection()) {
      for (int index = 1; index <= RUNS; index++) {
        Map<String, Object> result =
            "asr".equals(kind)
                ? recognizeOne(client, connection, audio, model, outputDir, index)
                : synthesizeOne(client, connection, model, voice, text, outputDir, index);
        result.put("index", index);
        result.put("connection_open_after_task", true);
        System.out.println(MAPPER.writeValueAsString(result));
      }
    }
    System.out.println("完成：1 条连接，" + RUNS + " 个独立任务；" + outputDir.toAbsolutePath());
  }

  private static Map<String, Object> recognizeOne(
      DashScopeClient client,
      WsConnection connection,
      AudioCommon.PcmAudio audio,
      String model,
      Path outputDir,
      int index)
      throws Exception {
    RecognitionSession session =
        client
            .audio()
            .asr()
            .recognize(
                AsrRecognizeParams.builder().model(model).format("pcm").sampleRate(16000).build(),
                connection);
    ExecutorService pool = Executors.newSingleThreadExecutor();
    StringBuilder transcript = new StringBuilder();
    try {
      session.start();
      System.out.println("ASR " + index + "/" + RUNS + ": task_id=" + session.getTaskId());
      Future<?> receiver =
          pool.submit(
              () -> {
                for (AsrResult result : session.results()) {
                  AsrResult.Sentence sentence = result.getSentence();
                  if (sentence == null || sentence.getText() == null) {
                    continue;
                  }
                  if (Boolean.TRUE.equals(sentence.getSentenceEnd())) {
                    synchronized (transcript) {
                      transcript.append(sentence.getText()).append('\n');
                    }
                  } else {
                    System.err.println("[partial] " + sentence.getText());
                  }
                }
              });
      long start = System.nanoTime();
      int chunkIndex = 0;
      int chunkBytes = AudioPacer.bytesPer100ms(16000);
      for (byte[] frame : AudioPacer.pcmChunks(audio.pcm, chunkBytes)) {
        AudioPacer.pace(start, chunkIndex++);
        session.sendAudioFrame(frame);
      }
      session.stop();
      receiver.get();
      Map<String, Object> result =
          completedTask(
              session.getTaskId(),
              session.getRequestId(),
              session.getLastResponse(),
              session.getFirstPackageDelay(),
              session.getUsage());
      result.put("last_package_delay_ms", millis(session.getLastPackageDelay()));
      String text;
      synchronized (transcript) {
        text = transcript.toString().trim();
      }
      if (text.isEmpty()) {
        throw new RuntimeException("没有最终转写，请使用包含清晰语音的 WAV");
      }
      String name = String.format("asr-%02d.txt", index);
      Files.write(
          outputDir.resolve(name),
          (text + "\n").getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE_NEW);
      result.put("transcript", name);
      result.put("text", text);
      return result;
    } finally {
      session.close();
      pool.shutdown();
    }
  }

  private static Map<String, Object> synthesizeOne(
      DashScopeClient client,
      WsConnection connection,
      String model,
      String voice,
      String text,
      Path outputDir,
      int index)
      throws Exception {
    String roundText = "第" + index + "次。" + text;
    SynthesisSession session =
        client
            .audio()
            .tts()
            .connect(
                TtsConnectParams.builder()
                    .model(model)
                    .voice(voice)
                    .format("pcm")
                    .sampleRate(24000)
                    .build(),
                connection);
    ExecutorService pool = Executors.newSingleThreadExecutor();
    ByteArrayOutputStream pcm = new ByteArrayOutputStream();
    try {
      session.start();
      System.out.println("TTS " + index + "/" + RUNS + ": task_id=" + session.getTaskId());
      Future<Long> receiver =
          pool.submit(
              () -> {
                long received = 0;
                for (byte[] chunk : session.audioChunks()) {
                  pcm.write(chunk, 0, chunk.length);
                  received += chunk.length;
                }
                return received;
              });
      session.sendText(roundText);
      session.stop();
      long size = receiver.get();
      Map<String, Object> result =
          completedTask(
              session.getTaskId(),
              session.getRequestId(),
              session.getLastResponse(),
              session.getFirstPackageDelay(),
              session.getUsage());
      if (size == 0 || size % 2 != 0) {
        throw new RuntimeException("返回的 PCM 音频为空或不完整（" + size + " 字节）");
      }
      String name = String.format("tts-%02d.wav", index);
      AudioCommon.writeWav(outputDir.resolve(name), pcm.toByteArray(), 24000);
      result.put("audio", name);
      result.put("input_text", roundText);
      result.put("audio_bytes", size);
      result.put("audio_seconds", size / 48000.0);
      return result;
    } finally {
      session.close();
      pool.shutdown();
    }
  }

  /** Python completed_task(): the last frame must be this task's task-finished. */
  private static Map<String, Object> completedTask(
      String taskId, String requestId, JsonNode lastResponse, Duration firstDelay, Object usage) {
    JsonNode header = lastResponse == null ? null : lastResponse.path("header");
    String event = header == null ? null : header.path("event").asText(null);
    if (!"task-finished".equals(event) || !taskId.equals(header.path("task_id").asText(null))) {
      throw new RuntimeException("任务未收到匹配的 task-finished，停止复用连接");
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("task_id", taskId);
    result.put("request_id", requestId);
    result.put("final_event", event);
    result.put("first_package_delay_ms", millis(firstDelay));
    result.put("usage", usage);
    return result;
  }

  private static String millis(Duration duration) {
    return duration == null ? "null" : Long.toString(duration.toMillis());
  }

  private static void usage() {
    System.err.println(
        "usage: AudioConnectionReuse asr <speech.wav> --output-dir <new-dir> [--model m]\n"
            + "       AudioConnectionReuse tts --output-dir <new-dir> [--model m] [--voice v]"
            + " [--text t]");
    System.exit(2);
  }
}
