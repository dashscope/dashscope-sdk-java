// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.tingwu;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.errors.Errors;
import com.alibaba.dashscope.v3.errors.SdkErrorCode;
import com.alibaba.dashscope.v3.errors.SdkException;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.ws.TaskChannel;
import com.alibaba.dashscope.v3.transport.ws.TaskEvent;
import com.alibaba.dashscope.v3.transport.ws.TaskProtocol;
import com.alibaba.dashscope.v3.transport.ws.WsConnection;
import com.alibaba.dashscope.v3.transport.ws.WsSession;
import com.alibaba.dashscope.v3.types.tingwu.TingWuRealtimeParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Realtime TingWu session (ws duplex, task protocol; task_group {@code aigc}, task {@code
 * multimodal-generation}, function {@code generation}). Pull model — no SDK threads; user
 * discipline is one send thread + one receive thread, {@code finally close()}.
 *
 * <p>Lazy: nothing connects until {@link #start()}. {@link #events()} streams raw frames live until
 * the {@code speech-end} action (or task end); a payload-level {@code task-failed} action raises
 * through the error catalog like the header-level one.
 */
public final class TingWuRealtimeSession implements AutoCloseable {
  private static final String TASK_GROUP = "aigc";
  private static final String TASK = "multimodal-generation";
  private static final String FUNCTION = "generation";
  private static final String ACTION_TASK_FAILED = "task-failed";
  private static final String ACTION_SPEECH_END = "speech-end";

  private final HttpTransport transport;
  private final String url;
  private final TingWuRealtimeParams params;
  private final WsConnection shared;
  private final RequestOptions options;
  private final Duration receiveTimeout;

  private TaskChannel channel;
  private String taskId;
  private boolean started;
  private boolean terminal;
  private String requestId;

  TingWuRealtimeSession(
      HttpTransport transport,
      String url,
      TingWuRealtimeParams params,
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
    terminal = false;
    requestId = null;

    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("format", params.getFormat());
    parameters.put("sampleRate", params.getSampleRate());
    if (params.getParameters() != null) {
      parameters.putAll(params.getParameters());
    }
    // Named fields win over the parameters escape hatch (Python v2 parity).
    if (params.getTerminology() != null) {
      parameters.put("terminology", params.getTerminology());
    }
    if (params.getMaxEndSilence() != null) {
      parameters.put("maxEndSilence", params.getMaxEndSilence());
    }
    taskId = TaskProtocol.resolveTaskId(parameters);

    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode payload = TaskProtocol.newPayload(params.getModel(), TASK_GROUP, TASK, FUNCTION);
    ObjectNode input = mapper.createObjectNode();
    input.put("appId", params.getAppId());
    input.put("directive", "start");
    if (params.getWorkspaceId() != null) {
      input.put("workspace_id", params.getWorkspaceId());
    }
    if (params.getDataId() != null) {
      input.put("dataId", params.getDataId());
    }
    payload.set("input", input);
    payload.set("parameters", mapper.valueToTree(parameters));

    channel =
        shared != null
            ? shared.attach(taskId)
            : new WsSession(transport, url, taskId, TingWuService.MODULE, options);
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
          recordRequestId(event);
          if (TaskProtocol.TASK_STARTED.equals(event.getEvent())) {
            started = true;
          } else if (TaskProtocol.TASK_FAILED.equals(event.getEvent())) {
            throw failTask(event);
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

  /** Send one raw audio frame (format/sampleRate as configured). */
  public void sendAudioFrame(byte[] data) {
    sendAudioFrame(ByteBuffer.wrap(data));
  }

  public void sendAudioFrame(ByteBuffer data) {
    if (!started || terminal || channel == null) {
      throw illegalState("sendAudioFrame outside an active task");
    }
    channel.sendBinary(data);
  }

  /**
   * Signal end of audio (finish-task with {@code {appId, directive: "stop"}}); results keep
   * arriving as events until {@code speech-end}.
   */
  public void stop() {
    TaskChannel ch = channel;
    if (ch == null || !started || terminal) {
      return;
    }
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode input = mapper.createObjectNode();
    input.put("appId", params.getAppId());
    input.put("directive", "stop");
    ObjectNode payload = mapper.createObjectNode();
    payload.set("input", input);
    ch.sendText(
        TaskProtocol.frame(TaskProtocol.FINISH_TASK, taskId, TaskProtocol.STREAMING_DUPLEX, payload)
            .toString());
  }

  /**
   * Raw event frames (header included), ending at the {@code speech-end} action (inclusive) or
   * task-finished; header- and payload-level task-failed both raise.
   */
  public Iterable<JsonNode> events() {
    return () ->
        new Iterator<JsonNode>() {
          private JsonNode next;
          private boolean drained;

          @Override
          public boolean hasNext() {
            if (!drained && next == null) {
              next = nextEvent();
              if (next == null) {
                drained = true;
              }
            }
            return next != null;
          }

          @Override
          public JsonNode next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            JsonNode out = next;
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

  private JsonNode nextEvent() {
    while (true) {
      if (terminal) {
        return null;
      }
      if (channel == null || !started) {
        throw illegalState("events() outside an active task");
      }
      TaskEvent event = channel.recv(receiveTimeout);
      if (event == null) {
        continue;
      }
      switch (event.getKind()) {
        case TEXT:
          recordRequestId(event);
          if (TaskProtocol.TASK_FAILED.equals(event.getEvent())) {
            terminal = true;
            RuntimeException failure = failTask(event);
            close();
            throw failure;
          }
          JsonNode output = event.getPayload() == null ? null : event.getPayload().get("output");
          String action =
              output != null && output.hasNonNull("action") ? output.get("action").asText() : null;
          if (ACTION_TASK_FAILED.equals(action)) {
            terminal = true;
            String message = textOrNull(output.get("errorMessage"));
            RuntimeException failure =
                Errors.fromBusinessCode(
                    textOrNull(output.get("errorCode")),
                    message != null ? message : "task failed",
                    requestId,
                    JsonSupport.toPlainValue(event.getRaw()),
                    null);
            close();
            throw failure;
          }
          if (ACTION_SPEECH_END.equals(action)
              || TaskProtocol.TASK_FINISHED.equals(event.getEvent())) {
            terminal = true;
          }
          return event.getRaw();
        case BINARY:
          // TingWu downlink is text-only; skip unexpected binaries.
          continue;
        case CLOSED:
          terminal = true;
          close();
          throw Errors.connection(
              SdkErrorCode.SDK_CONNECTION_ERROR, "websocket closed before speech-end", null);
        case FAILURE:
          terminal = true;
          close();
          throw event.getFailure();
        default:
          break;
      }
    }
  }

  private void recordRequestId(TaskEvent event) {
    JsonNode raw = event.getRaw();
    if (raw == null) {
      return;
    }
    // TingWu frames carry request_id in the header; task-protocol convention is top-level.
    JsonNode header = raw.get("header");
    JsonNode id = header != null ? header.get("request_id") : null;
    if (id == null || !id.isTextual()) {
      id = raw.get("request_id");
    }
    if (id != null && id.isTextual() && !id.asText().isEmpty()) {
      requestId = id.asText();
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

  private static String textOrNull(JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }

  private static SdkException illegalState(String detail) {
    return Errors.fromSdkErrorCode(
        SdkErrorCode.SDK_WS_ILLEGAL_STATE, Collections.singletonMap("detail", detail));
  }
}
