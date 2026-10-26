// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.errors.SdkException;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ws.TaskChannel;
import com.alibaba.dashscope.transport.ws.TaskEvent;
import com.alibaba.dashscope.transport.ws.TaskProtocol;
import com.alibaba.dashscope.transport.ws.WsConnection;
import com.alibaba.dashscope.transport.ws.WsSession;
import com.alibaba.dashscope.types.audio.AsrRecognizeParams;
import com.alibaba.dashscope.types.audio.AsrResult;
import com.alibaba.dashscope.types.common.Usage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * ASR realtime session (audio design §1.1): ws duplex over the task protocol. Pull model — no SDK
 * threads; user discipline is one send thread + one receive thread, {@code finally close()}.
 *
 * <p>Pumped events are buffered inside the session, so {@code stop()} may be called before
 * consuming results (the design-doc pattern) and {@link #events()}/{@link #results()} can replay
 * from the beginning at any time. {@link #close()} is idempotent and the session can be re-{@link
 * #start()}ed (new task, new socket unless a shared {@link WsConnection} was supplied).
 */
public final class RecognitionSession implements AutoCloseable {
  private static final String TASK_GROUP = "audio";
  private static final String TASK = "asr";
  private static final String FUNCTION = "recognition";

  private final HttpTransport transport;
  private final String url;
  private final AsrRecognizeParams params;
  private final WsConnection shared;
  private final RequestOptions options;
  private final Duration receiveTimeout;

  private final Object pumpLock = new Object();
  private final List<TaskEvent> delivered = new ArrayList<>();

  private TaskChannel channel;
  private String taskId;
  private boolean started;
  private boolean terminal;
  private RuntimeException terminalFailure;
  private JsonNode lastResponse;
  private Usage usage;
  private String requestId;
  private long firstFrameNanos = -1;
  private long stopNanos = -1;
  private Duration firstPackageDelay;
  private Duration lastPackageDelay;

  RecognitionSession(
      HttpTransport transport,
      String url,
      AsrRecognizeParams params,
      WsConnection shared,
      RequestOptions options) {
    this.transport = transport;
    this.url = url;
    this.params = params;
    this.shared = shared;
    this.options = options == null ? RequestOptions.NONE : options;
    this.receiveTimeout = transport.config().getTimeout();
  }

  /** Connect (unless a shared connection was supplied), send run-task, block until task-started. */
  public synchronized void start() {
    if (channel != null) {
      throw illegalState("session already started; close() before re-starting");
    }
    delivered.clear();
    terminal = false;
    terminalFailure = null;
    lastResponse = null;
    usage = null;
    requestId = null;
    firstFrameNanos = -1;
    stopNanos = -1;
    firstPackageDelay = null;
    lastPackageDelay = null;

    Map<String, Object> parameters = new LinkedHashMap<>();
    if (params.getFormat() != null) {
      parameters.put("format", params.getFormat());
    }
    if (params.getSampleRate() != null) {
      parameters.put("sample_rate", params.getSampleRate());
    }
    if (params.getVocabularyId() != null) {
      parameters.put("vocabulary_id", params.getVocabularyId());
    }
    if (params.getDisfluencyRemovalEnabled() != null) {
      parameters.put("disfluency_removal_enabled", params.getDisfluencyRemovalEnabled());
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
    if (params.getPhraseId() != null) {
      // phraseId rides as top-level resources (design §1.4), never inside parameters.
      payload
          .putArray("resources")
          .addObject()
          .put("resource_id", params.getPhraseId())
          .put("resource_type", "asr_phrase");
    }

    channel =
        shared != null
            ? shared.attach(taskId)
            : new WsSession(transport, url, taskId, TASK_GROUP, options);
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
          throw Errors.connection(
              SdkErrorCode.SDK_CONNECTION_ERROR, "websocket closed before task-started", null);
        }
      }
    } catch (RuntimeException e) {
      // run-task failure: close first, then raise (audio design §6).
      close();
      throw e;
    }
  }

  /** Send one binary audio frame (zero-copy path upstream; at most one copy internally). */
  public void sendAudioFrame(byte[] data) {
    sendAudioFrame(ByteBuffer.wrap(data));
  }

  public void sendAudioFrame(ByteBuffer data) {
    if (!started || terminal || channel == null) {
      throw illegalState("sendAudioFrame outside an active task");
    }
    if (firstFrameNanos < 0) {
      firstFrameNanos = System.nanoTime();
    }
    channel.sendBinary(data);
  }

  /** continue-task with the map carried as {@code payload.input} verbatim (2.x parity). */
  public void updateContext(Map<String, Object> payloadInput) {
    if (payloadInput == null) {
      throw new IllegalArgumentException("payloadInput must not be null");
    }
    if (!started || terminal || channel == null) {
      throw illegalState("updateContext outside an active task");
    }
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode payload = TaskProtocol.newPayload(params.getModel(), TASK_GROUP, TASK, FUNCTION);
    payload.set("input", mapper.valueToTree(payloadInput));
    channel.sendText(
        TaskProtocol.frame(
                TaskProtocol.CONTINUE_TASK, taskId, TaskProtocol.STREAMING_DUPLEX, payload)
            .toString());
  }

  /** finish-task and wait for task-finished with the client receive timeout. */
  public void stop() {
    stop(receiveTimeout);
  }

  /**
   * finish-task and wait for task-finished, bounded by {@code timeout} (fixes the 2.x unbounded
   * {@code stopLatch.await()} deadlock). Events pumped while waiting stay buffered for {@link
   * #events()}/{@link #results()}.
   */
  public void stop(Duration timeout) {
    TaskChannel ch = channel;
    if (ch == null || !started) {
      return;
    }
    if (!terminal) {
      ch.sendText(
          TaskProtocol.finishTaskFrame(taskId, TaskProtocol.STREAMING_DUPLEX, null).toString());
      if (stopNanos < 0) {
        stopNanos = System.nanoTime();
      }
      long deadline = System.nanoTime() + timeout.toNanos();
      while (!terminal) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
          throw Errors.timeout(
              SdkErrorCode.SDK_TIMEOUT, "waiting for task-finished after finish-task", null);
        }
        pumpOnce(Duration.ofNanos(Math.min(remaining, receiveTimeout.toNanos())));
        if (!terminal && terminalFailure == null && System.nanoTime() >= deadline) {
          // recv timed out without delivering anything.
          throw Errors.timeout(
              SdkErrorCode.SDK_TIMEOUT, "waiting for task-finished after finish-task", null);
        }
      }
    }
    if (terminalFailure != null) {
      throw terminalFailure;
    }
  }

  /** Raw event frames (header included), task-finished included; task-failed raises. */
  public Iterable<JsonNode> events() {
    return () ->
        new Iterator<JsonNode>() {
          private final Iterator<TaskEvent> it = eventIterator();

          @Override
          public boolean hasNext() {
            return it.hasNext();
          }

          @Override
          public JsonNode next() {
            TaskEvent event = it.next();
            return event.getKind() == TaskEvent.Kind.TEXT ? event.getRaw() : null;
          }
        };
  }

  /** Typed results: result-generated only, server heartbeat sentences filtered out. */
  public Iterable<AsrResult> results() {
    return () ->
        new Iterator<AsrResult>() {
          private final Iterator<TaskEvent> it = eventIterator();
          private AsrResult next;

          @Override
          public boolean hasNext() {
            while (next == null) {
              if (!it.hasNext()) {
                return false;
              }
              TaskEvent event = it.next();
              if (event.getKind() != TaskEvent.Kind.TEXT
                  || !TaskProtocol.RESULT_GENERATED.equals(event.getEvent())
                  || event.getPayload() == null) {
                continue;
              }
              JsonNode output = event.getPayload().get("output");
              JsonNode sentence = output == null ? null : output.get("sentence");
              if (sentence == null || !sentence.isObject()) {
                continue;
              }
              AsrResult result =
                  JsonSupport.mapper()
                      .convertValue(event.getPayload().get("output"), AsrResult.class);
              if (result.getSentence() != null
                  && Boolean.TRUE.equals(result.getSentence().getHeartbeat())) {
                continue;
              }
              next = result;
            }
            return true;
          }

          @Override
          public AsrResult next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            AsrResult out = next;
            next = null;
            return out;
          }
        };
  }

  public String getTaskId() {
    return taskId;
  }

  public String getRequestId() {
    return requestId;
  }

  /** The last text frame received (raw, header included). */
  public JsonNode getLastResponse() {
    return lastResponse;
  }

  public Usage getUsage() {
    return usage;
  }

  /** First audio frame to first result; null until both happened. */
  public Duration getFirstPackageDelay() {
    return firstPackageDelay;
  }

  /** finish-task to task-finished; null until both happened. */
  public Duration getLastPackageDelay() {
    return lastPackageDelay;
  }

  /**
   * Idempotent. With a shared connection this detaches the task only; the socket stays open. The
   * session may be re-started afterwards.
   */
  @Override
  public synchronized void close() {
    TaskChannel ch = channel;
    channel = null;
    started = false;
    if (ch != null) {
      ch.close();
    }
  }

  private Iterator<TaskEvent> eventIterator() {
    return new Iterator<TaskEvent>() {
      private int index;

      @Override
      public boolean hasNext() {
        while (true) {
          synchronized (pumpLock) {
            if (index < delivered.size()) {
              return true;
            }
            if (terminalFailure != null) {
              throw terminalFailure;
            }
            if (terminal) {
              return false;
            }
          }
          pumpOnce(receiveTimeout);
        }
      }

      @Override
      public TaskEvent next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        synchronized (pumpLock) {
          return delivered.get(index++);
        }
      }
    };
  }

  /** Pump one inbound event into the session buffer; terminal events flip state. */
  private void pumpOnce(Duration timeout) {
    TaskEvent event;
    synchronized (pumpLock) {
      if (terminal) {
        return;
      }
      if (channel == null) {
        // Never busy-spin on a dead session (not started / start failed / closed mid-iteration):
        // with no channel there is no blocking recv, so returning here would spin the caller.
        terminal = true;
        terminalFailure = illegalState("session not started or already closed");
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
            // §6 close ordering: record (recordText above) -> close -> rethrow.
            close();
            throw terminalFailure;
          }
          delivered.add(event);
          if (TaskProtocol.RESULT_GENERATED.equals(event.getEvent())
              && firstPackageDelay == null
              && firstFrameNanos >= 0) {
            firstPackageDelay = Duration.ofNanos(System.nanoTime() - firstFrameNanos);
          }
          if (TaskProtocol.TASK_FINISHED.equals(event.getEvent())) {
            terminal = true;
            if (stopNanos >= 0) {
              lastPackageDelay = Duration.ofNanos(System.nanoTime() - stopNanos);
            }
          }
          break;
        case BINARY:
          delivered.add(event);
          break;
        case CLOSED:
          terminal = true;
          terminalFailure =
              Errors.connection(
                  SdkErrorCode.SDK_CONNECTION_ERROR, "websocket closed before task-finished", null);
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

  private static SdkException illegalState(String detail) {
    return Errors.fromSdkErrorCode(
        SdkErrorCode.SDK_WS_ILLEGAL_STATE, Collections.singletonMap("detail", detail));
  }
}
