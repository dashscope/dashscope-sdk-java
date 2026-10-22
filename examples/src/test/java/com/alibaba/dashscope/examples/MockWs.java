// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okio.ByteString;

/**
 * Server-side WebSocket players for the examples dry-run: reactive counterparts of the v3
 * task protocol ({@code /api-ws/v1/inference}) and the realtime protocol ({@code
 * /api-ws/v1/realtime}), mirroring the SDK's own mock tests.
 */
public final class MockWs {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MockWs() {}

  /** 0.5 s of mono 16-bit PCM silence wrapped in a WAV container. */
  public static byte[] wavFileBytes(int sampleRate) {
    byte[] pcm = new byte[sampleRate]; // sampleRate bytes = 0.5 s at 16-bit mono
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    writeAscii(out, "RIFF");
    writeIntLE(out, 36 + pcm.length);
    writeAscii(out, "WAVEfmt ");
    writeIntLE(out, 16);
    writeShortLE(out, (short) 1); // PCM
    writeShortLE(out, (short) 1); // mono
    writeIntLE(out, sampleRate);
    writeIntLE(out, sampleRate * 2);
    writeShortLE(out, (short) 2);
    writeShortLE(out, (short) 16);
    writeAscii(out, "data");
    writeIntLE(out, pcm.length);
    out.write(pcm, 0, pcm.length);
    return out.toByteArray();
  }

  /** Raw PCM payload streamed to TTS/realtime receivers (even length, non-empty). */
  static byte[] pcmChunk(int bytes) {
    return new byte[bytes];
  }

  static void send(WebSocket socket, String json) {
    socket.send(json);
  }

  static MockResponse upgrade(WebSocketListener listener) {
    return new MockResponse().withWebSocketUpgrade(listener);
  }

  /**
   * Task protocol (ASR recognition, TTS synthesis, reusable connections): answers {@code
   * run-task} with task-started plus one canned payload (sentence for asr, binary PCM for tts)
   * and {@code finish-task} with task-finished carrying usage.
   */
  public static final class TaskSocket extends WebSocketListener {
    @Override
    public void onMessage(WebSocket webSocket, String text) {
      try {
        JsonNode frame = MAPPER.readTree(text);
        JsonNode header = frame.path("header");
        String action = header.path("action").asText();
        String taskId = header.path("task_id").asText();
        if ("run-task".equals(action)) {
          send(webSocket, taskEvent(taskId, "task-started", "{}"));
          String task = frame.path("payload").path("task").asText();
          if ("asr".equals(task)) {
            send(
                webSocket,
                taskEvent(
                    taskId,
                    "result-generated",
                    "{\"output\":{\"sentence\":{\"begin_time\":0,\"end_time\":100,"
                        + "\"text\":\"你好\",\"sentence_end\":true}}}"));
          } else if ("tts".equals(task)) {
            webSocket.send(ByteString.of(pcmChunk(4800)));
          }
        } else if ("finish-task".equals(action)) {
          send(
              webSocket,
              taskEvent(
                  taskId,
                  "task-finished",
                  "{\"output\":{},\"usage\":{\"input_tokens\":3,\"output_tokens\":2,"
                      + "\"total_tokens\":5}}"));
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      webSocket.close(code, reason);
    }

    private static String taskEvent(String taskId, String event, String payloadJson) {
      return "{\"header\":{\"task_id\":\""
          + taskId
          + "\",\"event\":\""
          + event
          + "\"},\"payload\":"
          + payloadJson
          + "}";
    }
  }

  /**
   * Realtime protocol: answers session.update, drives one response on commit (dialog models) or
   * on the first audio append (livetranslate), and finishes the session on session.finish.
   */
  public static final class RealtimeSocket extends WebSocketListener {
    private final boolean translation;
    private final AtomicBoolean responseSent = new AtomicBoolean();

    public RealtimeSocket(String model) {
      this.translation = model != null && model.contains("livetranslate");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
      try {
        JsonNode frame = MAPPER.readTree(text);
        String type = frame.path("type").asText();
        switch (type) {
          case "session.update":
            send(
                webSocket,
                "{\"type\":\"session.updated\",\"session\":{\"id\":\"sess-dry\"}}");
            break;
          case "input_audio_buffer.append":
            if (translation && responseSent.compareAndSet(false, true)) {
              emitTranslation(webSocket);
            }
            break;
          case "input_audio_buffer.commit":
            if (!translation && responseSent.compareAndSet(false, true)) {
              emitDialog(webSocket);
            }
            break;
          case "session.finish":
            send(
                webSocket,
                "{\"type\":\"session.finished\",\"session\":{\"id\":\"sess-dry\"}}");
            break;
          default:
            break;
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
      if (translation && responseSent.compareAndSet(false, true)) {
        emitTranslation(webSocket);
      }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      webSocket.close(code, reason);
    }

    private void emitDialog(WebSocket webSocket) {
      sendAudioDelta(webSocket);
      send(
          webSocket,
          "{\"type\":\"response.audio_transcript.done\",\"transcript\":\"你好\"}");
      sendResponseDone(webSocket);
    }

    private void emitTranslation(WebSocket webSocket) {
      send(
          webSocket,
          "{\"type\":\"conversation.item.input_audio_transcription.completed\","
              + "\"transcript\":\"你好\"}");
      sendAudioDelta(webSocket);
      send(
          webSocket,
          "{\"type\":\"response.audio_transcript.done\",\"transcript\":\"hello\"}");
      sendResponseDone(webSocket);
    }

    private void sendAudioDelta(WebSocket webSocket) {
      send(
          webSocket,
          "{\"type\":\"response.audio.delta\",\"delta\":\""
              + Base64.getEncoder().encodeToString(pcmChunk(4800))
              + "\"}");
    }

    private void sendResponseDone(WebSocket webSocket) {
      send(
          webSocket,
          "{\"type\":\"response.done\",\"response\":{\"id\":\"resp-dry\",\"status\":\"completed\","
              + "\"usage\":{\"input_tokens\":3,\"output_tokens\":2}}}");
    }
  }

  private static void writeAscii(ByteArrayOutputStream out, String value) {
    for (char c : value.toCharArray()) {
      out.write((byte) c);
    }
  }

  private static void writeIntLE(ByteArrayOutputStream out, int value) {
    out.write(value & 0xff);
    out.write((value >> 8) & 0xff);
    out.write((value >> 16) & 0xff);
    out.write((value >> 24) & 0xff);
  }

  private static void writeShortLE(ByteArrayOutputStream out, short value) {
    out.write(value & 0xff);
    out.write((value >> 8) & 0xff);
  }
}
