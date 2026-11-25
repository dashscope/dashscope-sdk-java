// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.resources.audio.SynthesisSession;
import com.alibaba.dashscope.types.audio.TtsConnectParams;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.alibaba.dashscope.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.completions.Delta;
import com.alibaba.dashscope.types.messages.Message;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * WebSocket duplex TTS: send text while receiving audio into one WAV, then play it. Translation of
 * the Python blueprint example 01_atomic_capabilities/audio/tts_websocket.py.
 *
 * <p>Default model {@code qwen-audio-3.1-tts-flash}, voice {@code longanhuan_v3.1}. With {@code
 * --prompt}, an LLM answer is streamed first ({@code chat.completions.createStreaming}) and every
 * delta text is fed straight into the synthesizer.
 *
 * <pre>
 * mvn -q -f examples/pom.xml exec:java -Dexec.mainClass=com.alibaba.dashscope.examples.atomic.audio.TtsWebsocketConnect -Dexec.args="--text 'Hello, welcome to speech synthesis.' --output hello.wav"
 * </pre>
 */
public final class TtsWebsocketConnect {
  private static final Map<String, String> DEFAULT_VOICES = new HashMap<>();

  static {
    DEFAULT_VOICES.put("qwen-audio-3.1-tts-flash", "longanhuan_v3.1");
    DEFAULT_VOICES.put("qwen-audio-3.0-tts-flash", "longanhuan_v3.6");
    DEFAULT_VOICES.put("cosyvoice-v3-flash", "longanhuan");
    DEFAULT_VOICES.put("cosyvoice-v3-plus", "longanhuan");
  }

  public static void main(String[] args) throws Exception {
    List<String> texts = new ArrayList<>();
    String prompt = null;
    String llmModel = "qwen-plus";
    String model = "qwen-audio-3.1-tts-flash";
    String voice = null;
    String instruction = null;
    int sampleRate = 24000;
    Path output = null;
    boolean noPlay = false;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--text":
          texts.add(args[++i]);
          break;
        case "--prompt":
          prompt = args[++i];
          break;
        case "--llm-model":
          llmModel = args[++i];
          break;
        case "--model":
          model = args[++i];
          break;
        case "--voice":
          voice = args[++i];
          break;
        case "--instruction":
          instruction = args[++i];
          break;
        case "--sample-rate":
          sampleRate = Integer.parseInt(args[++i]);
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
    if (output == null
        || (texts.isEmpty() && prompt == null)
        || (prompt != null && !texts.isEmpty())) {
      usage();
    }
    if (prompt != null && prompt.trim().isEmpty()) {
      System.err.println("error: --prompt 需要非空提示词");
      System.exit(2);
    }
    if (prompt == null && texts.stream().noneMatch(t -> !t.trim().isEmpty())) {
      System.err.println("error: 文本不能为空");
      System.exit(2);
    }
    if (voice == null) {
      voice = DEFAULT_VOICES.getOrDefault(model, "longanhuan_v3.1");
    }

    try (DashScopeClient client = ExampleSupport.client()) {
      TtsConnectParams.Builder params =
          TtsConnectParams.builder().model(model).voice(voice).format("pcm").sampleRate(sampleRate);
      if (instruction != null) {
        params.parameters(Collections.singletonMap("instruction", instruction));
      }
      SynthesisSession session = client.audio().tts().connect(params.build());
      ExecutorService receiverPool = Executors.newSingleThreadExecutor();
      ByteArrayOutputStream pcm = new ByteArrayOutputStream();
      long size;
      try {
        session.start();
        // Receive audio chunks on a worker thread while the caller thread sends text.
        Future<Long> receiver =
            receiverPool.submit(
                () -> {
                  long received = 0;
                  for (byte[] chunk : session.audioChunks()) {
                    pcm.write(chunk, 0, chunk.length);
                    received += chunk.length;
                  }
                  return received;
                });
        try {
          if (prompt != null) {
            streamPrompt(client, llmModel, prompt, session, receiver);
          } else {
            for (String part : texts) {
              checkReceiver(receiver);
              if (!part.isEmpty()) {
                session.sendText(part);
              }
            }
          }
          session.stop();
          size = receiver.get();
        } finally {
          // Close before joining the pool to unblock a socket reader (Python parity).
          session.close();
        }
        if (size == 0 || size % 2 != 0) {
          throw new RuntimeException("返回的 PCM 音频为空或不完整（" + size + " 字节）");
        }
        System.err.println(
            "request_id="
                + session.getRequestId()
                + " first_package_delay_ms="
                + millis(session.getFirstPackageDelay())
                + " usage="
                + session.getUsage());
        AudioCommon.writeWav(output, pcm.toByteArray(), sampleRate);
      } finally {
        receiverPool.shutdown();
      }
      System.out.println(
          "已保存 "
              + output.toAbsolutePath()
              + "（"
              + String.format("%.2f", size / 2.0 / sampleRate)
              + " 秒）");
      if (!noPlay) {
        AudioCommon.play(output);
      }
    }
  }

  /** Stream an LLM answer and feed every delta text into the synthesizer (Python --prompt flow). */
  private static void streamPrompt(
      DashScopeClient client,
      String llmModel,
      String prompt,
      SynthesisSession session,
      Future<Long> receiver)
      throws Exception {
    try (ResponseStream<ChatCompletionChunk> stream =
        client
            .chat()
            .completions()
            .createStreaming(
                ChatCompletionCreateParams.builder()
                    .model(llmModel)
                    .messages(Collections.singletonList(Message.user(prompt)))
                    .incrementalOutput(true)
                    .build())) {
      for (ChatCompletionChunk chunk : stream) {
        Delta delta = chunk.delta();
        String text = delta == null ? null : delta.getContent();
        if (text != null && !text.isEmpty()) {
          checkReceiver(receiver);
          session.sendText(text);
        }
      }
    }
  }

  /** Surface a receiver failure as soon as the send side notices it exited early. */
  private static void checkReceiver(Future<?> receiver) throws Exception {
    if (receiver.isDone()) {
      receiver.get();
      throw new RuntimeException("文本尚未发送完，合成会话已结束");
    }
  }

  private static String millis(Duration duration) {
    return duration == null ? "null" : Long.toString(duration.toMillis());
  }

  private static void usage() {
    System.err.println(
        "usage: TtsWebsocketConnect (--text <t> [--text <t>...] | --prompt <p>) --output <out.wav>"
            + " [--model m] [--voice v] [--llm-model m] [--instruction s] [--sample-rate 24000]"
            + " [--no-play]");
    System.exit(2);
  }
}
