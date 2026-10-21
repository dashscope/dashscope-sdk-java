// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.common.SdkErrorCode;
import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.SdkException;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ws.TaskChannel;
import com.alibaba.dashscope.transport.ws.TaskEvent;
import com.alibaba.dashscope.transport.ws.TaskProtocol;
import com.alibaba.dashscope.transport.ws.WsConnection;
import com.alibaba.dashscope.transport.ws.WsSession;
import com.alibaba.dashscope.types.audio.TtsConnectParams;
import com.alibaba.dashscope.types.common.Usage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TTS duplex session (audio design §1.2): ws task protocol, text up, binary audio down. Pull
 * model — no SDK threads.
 *
 * <p>{@link #events()} (mixed JsonNode + byte[]) and {@link #audioChunks()} (audio only) are
 * mutually exclusive single readers: the first claim wins, a conflicting claim throws {@link
 * SdkException} immediately. Pumped items are buffered, so a reader may also start after {@link
 * #stop()} and replay. {@link #close()} is idempotent but the session is NOT restartable (Python
 * parity — asymmetric with ASR). On a shared {@link WsConnection} the binary downlink is claimed
 * at {@code start()} and released at task end.
 */
public final class SynthesisSession implements AutoCloseable {
  private static final String TASK_GROUP = "audio";
  private static final String TASK = "tts";
  private static final String FUNCTION = "SpeechSynthesizer";
  private static final String DIRECTIVE_CANCEL = "cancel";

  private final HttpTransport transport;
  private final String url;
  private final TtsConnectParams params;
  private final WsConnection shared;
  private final RequestOptions options;
  private final Duration receiveTimeout;

  private final Object pumpLock = new Object();
  private final List<TaskEvent> delivered = new ArrayList<>();
  private final AtomicBoolean cancelSent = new AtomicBoolean(false);
  private final AtomicReference<String> activeReader = new AtomicReference<>();
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private TaskChannel channel;
  private String taskId;
  private boolean started;
  private boolean terminal;
  private RuntimeException terminalFailure;
  private JsonNode lastResponse;
  private Usage usage;
  private String requestId;
  private long firstTextNanos = -1;
  private long stopNanos = -1;
  private Duration firstPackageDelay;
  private Duration lastPackageDelay;

  SynthesisSession(
      HttpTransport transport,
      String url,
      TtsConnectParams params,
      WsConnection shared,
      RequestOptions options) {
    this.transport = transport;
    this.url = url;
    this.params = params;
    this.shared = shared;
    this.options = options == null ? RequestOptions.NONE : options;
    this.receiveTimeout = transport.config().getTimeout();
  }

  /** Connect (unless shared), send run-task, block until task-started; claims binary downlink. */
  public synchronized void start() {
    if (closed.get()) {
      throw illegalState("SynthesisSession is closed and cannot be restarted");
    }
    if (channel != null) {
      throw illegalState("session already started");
    }
    Map<String, Object> parameters = new LinkedHashMap<>();
    if (params.getVoice() != null) {
      parameters.put("voice", params.getVoice());
    }
    if (params.getFormat() != null) {
      parameters.put("format", params.getFormat());
    }
    if (params.getSampleRate() != null) {
      parameters.put("sample_rate", params.getSampleRate());
    }
    if (params.getVolume() != null) {
      parameters.put("volume", params.getVolume());
    }
    if (params.getSpeechRate() != null) {
      parameters.put("speech_rate", params.getSpeechRate());
    }
    if (params.getPitchRate() != null) {
      parameters.put("pitch_rate", params.getPitchRate());
    }
    if (Boolean.TRUE.equals(params.getEnableSsml())) {
      parameters.put("enable_ssml", true);
    }
    if (params.getPreTaskId() != null) {
      parameters.put("pre_task_id", params.getPreTaskId());
    }
    if (params.getParameters() != null) {
      parameters.putAll(params.getParameters());
    }
    taskId = TaskProtocol.resolveTaskId(parameters);

    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode payload = TaskProtocol.newPayload(params.getModel(), TASK_GROUP, TASK, FUNCTION);
    payload.set("input", mapper.createObjectNode());
    payload.set("parameters", mapper.valueToTree(parameters));

    channel = shared != null ? shared.attach(taskId) : new WsSession(transport, url, taskId, TASK_GROUP, options);
    if (shared != null) {
      shared.claimBinaryDownlink(taskId);
    }
    try {
      channel.sendText(
          TaskProtocol.frame(TaskProtocol.RUN_TASK, taskId, TaskProtocol.STREAMING_DUPLEX, payload)
              .toString());
      while (!started) {
        TaskEvent event = channel.recv(receiveTimeout);
        if (event == null) {
          continue;
        }
        if (event.getKind() == TaskEvent.Kind.TEXT) {
          recordText(event);
          if (TaskProtocol.TASK_STARTED.equals(event.getEvent())) {
            started = true;
          } else if (TaskProtocol.TASK_FAILED.equals(event.getEvent())) {
            throw failTask(event);
          } else {
            delivered.add(event);
          }
        } else if (event.getKind() == TaskEvent.Kind.FAILURE) {
          throw event.getFailure();
        } else if (event.getKind() == TaskEvent.Kind.CLOSED) {
          throw new ApiConnectionException("websocket closed before task-started", null);
        }
      }
    } catch (RuntimeException e) {
      close();
      throw e;
    }
  }

  /** continue-task carrying {@code payload.input.text} (2.x SpeechSynthesizerV2 parity). */
  public void sendText(String text) {
    if (text == null || text.isEmpty()) {
      throw new IllegalArgumentException("text must not be empty");
    }
    if (!started || terminal || channel == null) {
      throw illegalState("sendText outside an active task");
    }
    if (firstTextNanos < 0) {
      firstTextNanos = System.nanoTime();
    }
    ObjectNode input = JsonSupport.mapper().createObjectNode();
    input.put("text", text);
    sendContinue(input);
  }

  /** continue-task {@code {input:{flush:true}}} — synthesize buffered text without ending. */
  public void flush() {
    if (!started || terminal || channel == null) {
      throw illegalState("flush outside an active task");
    }
    ObjectNode input = JsonSupport.mapper().createObjectNode();
    input.put("flush", true);
    sendContinue(input);
  }

  /** finish-task and wait for task-finished with the client receive timeout. */
  public void stop() {
    stop(receiveTimeout);
  }

  /** finish-task and wait for task-finished, bounded by {@code timeout}. */
  public void stop(Duration timeout) {
    finishAndWait(null, timeout);
  }

  /**
   * finish-task with {@code directive:cancel} — once only, and never sent after a terminal event.
   */
  public void cancel() {
    cancel(receiveTimeout);
  }

  public void cancel(Duration timeout) {
    finishAndWait(DIRECTIVE_CANCEL, timeout);
  }

  private void finishAndWait(String directive, Duration timeout) {
    TaskChannel ch = channel;
    if (ch == null || !started) {
      return;
    }
    if (DIRECTIVE_CANCEL.equals(directive) && !cancelSent.compareAndSet(false, true)) {
      return;
    }
    if (!terminal) {
      ch.sendText(
          TaskProtocol.finishTaskFrame(taskId, TaskProtocol.STREAMING_DUPLEX, directive).toString());
      if (stopNanos < 0) {
        stopNanos = System.nanoTime();
      }
      long deadline = System.nanoTime() + timeout.toNanos();
      while (!terminal) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
          throw new ApiTimeoutException("timed out waiting for task-finished after finish-task", null);
        }
        pumpOnce(Duration.ofNanos(Math.min(remaining, receiveTimeout.toNanos())));
        if (!terminal && terminalFailure == null && System.nanoTime() >= deadline) {
          throw new ApiTimeoutException("timed out waiting for task-finished after finish-task", null);
        }
      }
    }
    if (terminalFailure != null) {
      throw terminalFailure;
    }
  }

  /**
   * Mixed stream: JsonNode for text events (task-finished included), byte[] for audio frames.
   * Mutually exclusive with {@link #audioChunks()}; task-failed raises.
   */
  public Iterable<Object> events() {
    claimReader("events");
    return () -> readerIterator("events", false);
  }

  /** Audio frames only. Mutually exclusive with {@link #events()}. */
  public Iterable<byte[]> audioChunks() {
    claimReader("audioChunks");
    return () -> readerIterator("audioChunks", true);
  }

  public String getTaskId() {
    return taskId;
  }

  public String getRequestId() {
    return requestId;
  }

  /** The last text frame received (raw, header included) — recorded before error rethrow (§6). */
  public JsonNode getLastResponse() {
    return lastResponse;
  }

  public Usage getUsage() {
    return usage;
  }

  /** First text sent to first audio/result; null until both happened. */
  public Duration getFirstPackageDelay() {
    return firstPackageDelay;
  }

  /** finish-task to task-finished; null until both happened. */
  public Duration getLastPackageDelay() {
    return lastPackageDelay;
  }

  /** Idempotent; detaches from a shared connection (socket stays open). NOT restartable. */
  @Override
  public synchronized void close() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    TaskChannel ch = channel;
    channel = null;
    started = false;
    if (ch != null) {
      ch.close();
    }
  }

  private void claimReader(String kind) {
    while (true) {
      String current = activeReader.get();
      if (current == null) {
        if (activeReader.compareAndSet(null, kind)) {
          return;
        }
      } else if (!current.equals(kind)) {
        throw illegalState("events() and audioChunks() are mutually exclusive; active: " + current);
      } else {
        throw illegalState(kind + " already has an active reader");
      }
    }
  }

  private void releaseReader(String kind) {
    activeReader.compareAndSet(kind, null);
  }

  @SuppressWarnings("unchecked")
  private <T> Iterator<T> readerIterator(String kind, boolean audioOnly) {
    return new Iterator<T>() {
      private int index;
      private Object next;
      private boolean done;

      @Override
      public boolean hasNext() {
        while (next == null && !done) {
          TaskEvent event;
          synchronized (pumpLock) {
            if (index < delivered.size()) {
              event = delivered.get(index++);
            } else {
              if (terminalFailure != null) {
                done = true;
                releaseReader(kind);
                throw terminalFailure;
              }
              if (terminal) {
                done = true;
                releaseReader(kind);
                return false;
              }
              event = null;
            }
          }
          if (event == null) {
            pumpOnce(receiveTimeout);
            continue;
          }
          if (audioOnly) {
            if (event.getKind() == TaskEvent.Kind.BINARY) {
              next = toBytes(event.getBinary());
            }
          } else if (event.getKind() == TaskEvent.Kind.TEXT) {
            next = event.getRaw();
          } else if (event.getKind() == TaskEvent.Kind.BINARY) {
            next = toBytes(event.getBinary());
          }
        }
        return next != null;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        Object out = next;
        next = null;
        return (T) out;
      }
    };
  }

  private void sendContinue(ObjectNode input) {
    ObjectNode payload = JsonSupport.mapper().createObjectNode();
    payload.set("input", input);
    channel.sendText(
        TaskProtocol.frame(TaskProtocol.CONTINUE_TASK, taskId, TaskProtocol.STREAMING_DUPLEX, payload)
            .toString());
  }

  private void pumpOnce(Duration timeout) {
    TaskEvent event;
    synchronized (pumpLock) {
      if (terminal || channel == null) {
        return;
      }
      event = channel.recv(timeout);
      if (event == null) {
        return;
      }
      switch (event.getKind()) {
        case TEXT:
          recordText(event);
          if (TaskProtocol.TASK_FAILED.equals(event.getEvent())) {
            terminal = true;
            terminalFailure = failTask(event);
            // §6 close 时序: lastResponse 已由 recordText 记录 → close → 重抛.
            close();
            throw terminalFailure;
          }
          delivered.add(event);
          markFirstPackageIfDue();
          if (TaskProtocol.TASK_FINISHED.equals(event.getEvent())) {
            markTerminal();
          }
          break;
        case BINARY:
          delivered.add(event);
          markFirstPackageIfDue();
          break;
        case CLOSED:
          terminal = true;
          terminalFailure = new ApiConnectionException("websocket closed before task-finished", null);
          close();
          throw terminalFailure;
        case FAILURE:
          terminal = true;
          terminalFailure = event.getFailure();
          close();
          throw terminalFailure;
        default:
          break;
      }
    }
  }

  private void markFirstPackageIfDue() {
    if (firstPackageDelay == null && firstTextNanos >= 0) {
      firstPackageDelay = Duration.ofNanos(System.nanoTime() - firstTextNanos);
    }
  }

  private void markTerminal() {
    terminal = true;
    if (stopNanos >= 0) {
      lastPackageDelay = Duration.ofNanos(System.nanoTime() - stopNanos);
    }
  }

  private void recordText(TaskEvent event) {
    lastResponse = event.getRaw();
    if (event.getRaw() != null && event.getRaw().hasNonNull("request_id")) {
      requestId = event.getRaw().get("request_id").asText();
    }
    JsonNode payload = event.getPayload();
    JsonNode usageNode = payload == null ? null : payload.get("usage");
    if (usageNode != null && usageNode.isObject()) {
      usage = JsonSupport.mapper().convertValue(usageNode, Usage.class);
    }
  }

  private RuntimeException failTask(TaskEvent event) {
    return Errors.fromBusinessCode(
        event.getErrorCode(),
        event.getErrorMessage(),
        requestId,
        JsonSupport.toPlainValue(event.getPayload()),
        null);
  }

  /** ByteBuffer view → owned array (the one allowed copy on the downlink path). */
  private static byte[] toBytes(java.nio.ByteBuffer buffer) {
    java.nio.ByteBuffer slice = buffer.slice();
    byte[] out = new byte[slice.remaining()];
    slice.get(out);
    return out;
  }

  private static SdkException illegalState(String detail) {
    return Errors.fromSdkErrorCode(
        SdkErrorCode.SDK_WS_ILLEGAL_STATE, Collections.singletonMap("detail", detail));
  }
}
