// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport.ws;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.ByteBuffer;

/**
 * One inbound item on a {@link TaskChannel}: a parsed task-protocol text event, a raw binary frame,
 * the socket-closed signal, or a transport-level failure. Binary frames carry no header, hence no
 * task id (audio design §7: ownership is claimed, not parsed).
 */
public final class TaskEvent {
  public enum Kind {
    TEXT,
    BINARY,
    CLOSED,
    FAILURE
  }

  private final Kind kind;
  private final String taskId;
  private final String event;
  private final String errorCode;
  private final String errorMessage;
  private final JsonNode payload;
  private final JsonNode raw;
  private final ByteBuffer binary;
  private final int closeCode;
  private final String closeReason;
  private final RuntimeException failure;

  private TaskEvent(
      Kind kind,
      String taskId,
      String event,
      String errorCode,
      String errorMessage,
      JsonNode payload,
      JsonNode raw,
      ByteBuffer binary,
      int closeCode,
      String closeReason,
      RuntimeException failure) {
    this.kind = kind;
    this.taskId = taskId;
    this.event = event;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.payload = payload;
    this.raw = raw;
    this.binary = binary;
    this.closeCode = closeCode;
    this.closeReason = closeReason;
    this.failure = failure;
  }

  static TaskEvent text(
      String taskId,
      String event,
      String errorCode,
      String errorMessage,
      JsonNode payload,
      JsonNode raw) {
    return new TaskEvent(
        Kind.TEXT, taskId, event, errorCode, errorMessage, payload, raw, null, 0, null, null);
  }

  static TaskEvent binary(ByteBuffer bytes) {
    return new TaskEvent(Kind.BINARY, null, null, null, null, null, null, bytes, 0, null, null);
  }

  static TaskEvent closed(int code, String reason) {
    return new TaskEvent(Kind.CLOSED, null, null, null, null, null, null, null, code, reason, null);
  }

  static TaskEvent failure(RuntimeException failure) {
    return new TaskEvent(Kind.FAILURE, null, null, null, null, null, null, null, 0, null, failure);
  }

  public Kind getKind() {
    return kind;
  }

  /** TEXT only: {@code header.task_id}. */
  public String getTaskId() {
    return taskId;
  }

  /** TEXT only: {@code header.event} (task-started / result-generated / task-finished / ...). */
  public String getEvent() {
    return event;
  }

  /** TEXT only: {@code header.error_code}; set on task-failed. */
  public String getErrorCode() {
    return errorCode;
  }

  /** TEXT only: {@code header.error_message}; set on task-failed. */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** TEXT only: the {@code payload} node (may be null). */
  public JsonNode getPayload() {
    return payload;
  }

  /** TEXT only: the full frame, header included. */
  public JsonNode getRaw() {
    return raw;
  }

  /** BINARY only: zero-copy view of the frame. */
  public ByteBuffer getBinary() {
    return binary;
  }

  public int getCloseCode() {
    return closeCode;
  }

  public String getCloseReason() {
    return closeReason;
  }

  /** FAILURE only: the transport-level exception. */
  public RuntimeException getFailure() {
    return failure;
  }
}
