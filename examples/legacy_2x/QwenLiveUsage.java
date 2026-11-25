// Copyright (c) Alibaba, Inc. and its affiliates.

import static com.alibaba.dashscope.audio.live.QwenLiveConstants.*;

import com.alibaba.dashscope.audio.live.QwenLiveCallback;
import com.alibaba.dashscope.audio.live.QwenLiveSessionConfig;
import com.alibaba.dashscope.audio.live.QwenLiveContext;
import com.alibaba.dashscope.audio.live.QwenLiveContextCompaction;
import com.alibaba.dashscope.audio.live.QwenLiveConversation;
import com.alibaba.dashscope.audio.live.QwenLiveDelegation;
import com.alibaba.dashscope.audio.live.QwenLiveDelegationType;
import com.alibaba.dashscope.audio.live.QwenLiveConnectionParam;
import com.alibaba.dashscope.audio.live.QwenLiveResponsesConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class QwenLiveUsage {
  private static final int FRAME_BYTES = 3200;

  private static final long FRAME_INTERVAL_MS = 100;
  private static final int AWAIT_SECONDS = 30;

  private static final BlockingQueue<JsonObject> delegations = new ArrayBlockingQueue<>(16);
  private static final CountDownLatch sessionSettled = new CountDownLatch(1);
  private static final AtomicBoolean sessionUpdated = new AtomicBoolean(false);
  private static final AtomicBoolean running = new AtomicBoolean(true);
  private static final AtomicLong outputAudioBytes = new AtomicLong();
  private static final AtomicReference<String> usageSeconds = new AtomicReference<>();
  private static final StringBuilder inputTranscript = new StringBuilder();
  private static final StringBuilder outputTranscript = new StringBuilder();
  private static volatile QwenLiveConversation conversation;

  public static void main(String[] args) throws Exception {
    if (args.length < 3 || args.length > 4) {
      System.out.println(
          "Usage: QwenLiveUsage <workspaceId> <client|responses> <pcmFile> [backendModel]");
      return;
    }
    String workspaceId = args[0];
    String mode = args[1];
    Path pcm = Paths.get(args[2]);
    String backendModel = args.length == 4 ? args[3] : null;
    if (!"client".equals(mode) && !"responses".equals(mode)) {
      System.out.println("mode must be client or responses, got: " + mode);
      return;
    }
    if ("responses".equals(mode) && (backendModel == null || backendModel.trim().isEmpty())) {
      System.out.println("responses mode requires an explicit backendModel argument.");
      return;
    }
    if (!Files.isRegularFile(pcm) || Files.size(pcm) == 0) {
      System.out.println("pcmFile must be a non-empty regular file: " + pcm);
      return;
    }
    if (hasWavHeader(pcm)) {
      System.out.println(
          "pcmFile looks like a WAV container; strip the header and pass raw PCM16.");
      return;
    }

    QwenLiveDelegation delegation;
    if ("client".equals(mode)) {
      delegation = QwenLiveDelegation.builder().type(QwenLiveDelegationType.CLIENT).build();
    } else {
      delegation =
          QwenLiveDelegation.builder()
              .type(QwenLiveDelegationType.RESPONSES)
              .responses(
                  QwenLiveResponsesConfig.builder()
                      .model(backendModel)
                      .instructions("根据任务调用已配置工具并返回清晰结果")
                      .tools(Collections.emptyList())
                      .build())
              .build();
    }
    QwenLiveSessionConfig config =
        QwenLiveSessionConfig.builder()
            .model("qwen-audio-realtime")
            .instructions("用简洁中文与用户交流")
            .inputAudioFormat("pcm")
            .outputAudioFormat("pcm")
            .context(
                QwenLiveContext.builder()
                    .compaction(
                        QwenLiveContextCompaction.builder().enabled(false).mode("local").build())
                    .build())
            .delegation(delegation)
            .build();

    ExecutorService agentWorker = Executors.newSingleThreadExecutor();
    ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    try {
      QwenLiveConnectionParam param =
          QwenLiveConnectionParam.builder()
              .model("qwen-live")
              .workspaceId(workspaceId)
              .callbackExecutor(callbackExecutor)
              .callbackQueueCapacity(256)
              .build();
      conversation = new QwenLiveConversation(param, new SampleCallback());
      conversation.connect();
      System.out.println("connected to " + param.getUrl());
      conversation.updateSession(config);
      if (!sessionSettled.await(AWAIT_SECONDS, TimeUnit.SECONDS) || !sessionUpdated.get()) {
        System.out.println("session.updated not confirmed; stop before sending audio.");
        return;
      }
      if ("client".equals(mode)) {
        agentWorker.submit(QwenLiveUsage::simulateExternalAgent);
      } else {
        conversation.createResponseItem(userTextItem("帮我看看今天适合户外活动吗"));
        conversation.createResponse();
      }
      streamAudio(pcm);
      running.set(false);
      agentWorker.shutdown();
      if (!agentWorker.awaitTermination(AWAIT_SECONDS, TimeUnit.SECONDS)) {
        System.out.println("agent worker did not stop in time.");
      }
      conversation.close();
      System.out.println(
          "session.closed received: " + conversation.awaitSessionClosed(AWAIT_SECONDS));
      System.out.println("session.closed reason: " + conversation.getCloseReason());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.out.println("interrupted: " + e.getMessage());
    } finally {
      running.set(false);
      if (conversation != null) conversation.cancel();
      agentWorker.shutdownNow();
      callbackExecutor.shutdown();
      boolean callbacksDrained = false;
      try {
        agentWorker.awaitTermination(AWAIT_SECONDS, TimeUnit.SECONDS);
        callbacksDrained = callbackExecutor.awaitTermination(AWAIT_SECONDS, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        if (!callbacksDrained) callbackExecutor.shutdownNow();
      }
      System.out.println("session id: " + (conversation == null ? null : conversation.getSessionId()));
      if (callbacksDrained) {
        System.out.println("output audio bytes: " + outputAudioBytes.get());
        System.out.println("usage seconds (cumulative): " + usageSeconds.get());
        System.out.println("input transcript: " + inputTranscript);
        System.out.println("output transcript: " + outputTranscript);
      } else {
        System.out.println("callbacks did not finish; final statistics are unavailable.");
      }
    }
  }

  private static void streamAudio(Path pcm) throws Exception {
    byte[] frame = new byte[FRAME_BYTES];
    long frames = 0;
    try (InputStream in = Files.newInputStream(pcm)) {
      int read;
      while (running.get() && (read = in.read(frame)) > 0) {
        conversation.appendAudioBytes(frame, 0, read);
        frames++;
        Thread.sleep(FRAME_INTERVAL_MS);
      }
    }
    System.out.println("sent audio frames: " + frames);
  }

  private static void simulateExternalAgent() {
    try {
      while (running.get()) {
        JsonObject event = delegations.poll(1, TimeUnit.SECONDS);
        if (event == null) {
          continue;
        }
        String delegationId = text(event, FIELD_DELEGATION_ID);
        if (delegationId == null) {
          System.out.println("delegation event without delegation_id, skipped.");
          continue;
        }
        System.out.println("delegation arguments: " + text(event, FIELD_ARGUMENTS));
        conversation.appendInstruction(delegationId, "稍等，我正在查询");
        conversation.appendThinking(delegationId, "本地模拟 Agent：已解析委派参数，准备生成结果");
        conversation.appendCommentary(delegationId, "这是本地模拟结果，未调用任何真实工具");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      System.out.println("agent worker stopped: " + e.getMessage());
    }
  }

  private static JsonObject userTextItem(String text) {
    JsonObject part = new JsonObject();
    part.addProperty(FIELD_TYPE, "input_text");
    part.addProperty("text", text);
    JsonArray content = new JsonArray();
    content.add(part);
    JsonObject item = new JsonObject();
    item.addProperty(FIELD_TYPE, "message");
    item.addProperty("role", "user");
    item.add(FIELD_CONTENT, content);
    return item;
  }

  private static boolean hasWavHeader(Path pcm) throws Exception {
    byte[] magic = new byte[4];
    try (InputStream in = Files.newInputStream(pcm)) {
      return in.read(magic) == 4 && "RIFF".equals(new String(magic, "US-ASCII"));
    }
  }

  private static String text(JsonObject event, String field) {
    JsonElement value = event.get(field);
    return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
  }

  private static class SampleCallback extends QwenLiveCallback {
    @Override
    public void onOpen() {
      System.out.println("websocket opened.");
    }

    @Override
    public void onEvent(JsonObject message) {
      String type = text(message, FIELD_TYPE);
      if (type == null) {
        System.out.println("event without type: " + message);
        return;
      }
      switch (type) {
        case EVENT_SESSION_UPDATED:
          sessionUpdated.set(true);
          sessionSettled.countDown();
          break;
        case EVENT_ERROR:
          break;
        case EVENT_SESSION_CLOSED:
          sessionSettled.countDown();
          break;
        case EVENT_SESSION_OUTPUT_AUDIO_DELTA:
          String audio = text(message, FIELD_AUDIO);
          if (audio == null) {
            audio = text(message, FIELD_DELTA);
          }
          if (audio != null) {
            outputAudioBytes.addAndGet(Base64.getDecoder().decode(audio).length);
          }
          break;
        case EVENT_SESSION_INPUT_TRANSCRIPT_DELTA:
          append(inputTranscript, text(message, FIELD_DELTA));
          break;
        case EVENT_SESSION_OUTPUT_TRANSCRIPT_DELTA:
          append(outputTranscript, text(message, FIELD_DELTA));
          break;
        case EVENT_SESSION_USAGE_UPDATED:
          JsonElement usage = message.get(FIELD_USAGE);
          if (usage != null && usage.isJsonObject()) {
            JsonElement seconds = usage.getAsJsonObject().get(FIELD_SECONDS);
            usageSeconds.set(seconds == null ? null : seconds.toString());
          }
          break;
        case EVENT_SESSION_DELEGATION_CREATED:
          if (!delegations.offer(message)) {
            System.out.println("delegation queue is full, event dropped: " + message);
          }
          break;
        case EVENT_RESPONSE_EVENT:
          System.out.println("backend response event: " + message.get(FIELD_EVENT));
          break;
        default:
          System.out.println("event: " + type);
      }
    }

    @Override
    public void onClose(int code, String reason) {
      System.out.println("websocket closed: " + code + ", " + reason);
      sessionSettled.countDown();
    }

    @Override
    public void onError(Throwable throwable) {
      System.out.println("live error: " + throwable);
      sessionSettled.countDown();
    }
  }

  private static void append(StringBuilder target, String delta) {
    if (delta == null) {
      return;
    }
    synchronized (target) {
      target.append(delta);
    }
  }
}
