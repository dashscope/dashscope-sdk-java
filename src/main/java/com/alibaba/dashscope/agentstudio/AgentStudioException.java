// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio;

import com.alibaba.dashscope.common.InternalErrorCode;
import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

/**
 * Typed AgentStudio error. Each error path maps onto one exception type, and each type owns a
 * single code namespace so a caller always knows which vocabulary {@link #getCode()} speaks:
 *
 * <ul>
 *   <li>{@link StatusError} — the server returned an HTTP response; the code is an
 *       Anthropic-compatible {@link PublicErrorCode} code (e.g. {@code not_found_error}).
 *   <li>{@link ConnectionError} — no HTTP response was received (connect / timeout); the code is an
 *       {@code sdk.agentstudio.*} internal code.
 *   <li>{@link StreamError} — an SSE stream failure; the code is an {@code sdk.agentstudio.*}
 *       internal code.
 * </ul>
 *
 * <p>Extends {@link ApiException} so existing {@code catch (ApiException)} still works.
 */
public class AgentStudioException extends ApiException {

  private final String code;
  private final String errorMessage;
  private final boolean retryable;

  AgentStudioException(
      Status status, String code, String errorMessage, boolean retryable, Throwable cause) {
    super(status, cause);
    this.code = code;
    this.errorMessage = errorMessage;
    this.retryable = retryable;
  }

  /** HTTP status code, or {@code -1} when no HTTP response was received. */
  public int getStatusCode() {
    return getStatus() != null ? getStatus().getStatusCode() : -1;
  }

  /** Unified error code; the namespace depends on the concrete type (see class javadoc). */
  public String getCode() {
    return code;
  }

  /** Raw server-supplied code before normalization (may be {@code null} or empty). */
  public String getRawCode() {
    return getStatus() != null ? getStatus().getCode() : null;
  }

  /** Resolved, human-readable message. */
  public String getErrorMessage() {
    return errorMessage;
  }

  public String getRequestId() {
    return getStatus() != null ? getStatus().getRequestId() : null;
  }

  public boolean isRetryable() {
    return retryable;
  }

  @Override
  public String getMessage() {
    return String.format(
        "status=%d code=%s message=%s request_id=%s",
        getStatusCode(), code, errorMessage, getRequestId());
  }

  // --- Error subtypes: one code namespace each -------------------------------

  /** HTTP response error. {@link #getCode()} is always a public Anthropic-compatible code. */
  public static final class StatusError extends AgentStudioException {
    StatusError(Status status, String code, String message, boolean retryable, Throwable cause) {
      super(status, code, message, retryable, cause);
    }
  }

  /**
   * Transport failure (no HTTP response). {@link #getCode()} is an {@code sdk.agentstudio.*} code.
   */
  public static final class ConnectionError extends AgentStudioException {
    ConnectionError(
        Status status, String code, String message, boolean retryable, Throwable cause) {
      super(status, code, message, retryable, cause);
    }
  }

  /** SSE stream failure. {@link #getCode()} is an {@code sdk.agentstudio.*} code. */
  public static final class StreamError extends AgentStudioException {
    StreamError(Status status, String code, String message, boolean retryable, Throwable cause) {
      super(status, code, message, retryable, cause);
    }
  }

  // --- Factories -------------------------------------------------------------

  /**
   * Wrap an {@link ApiException}. A missing status or an {@link IOException} cause means no HTTP
   * response was received (transport failure) and yields a {@link ConnectionError}; otherwise the
   * server responded and we build a {@link StatusError}.
   */
  public static AgentStudioException wrap(ApiException e) {
    if (e instanceof AgentStudioException) {
      return (AgentStudioException) e;
    }
    Status status = e.getStatus();
    Throwable cause = e.getCause();
    if (status == null || cause instanceof IOException) {
      return connectionError(cause != null ? cause : e);
    }
    return statusError(status, cause);
  }

  /** HTTP response error carrying a public code (the server's code, else generic api_error). */
  public static StatusError statusError(Status status, Throwable cause) {
    int statusCode = status.getStatusCode();
    return new StatusError(
        status,
        publicCode(status.getCode()),
        resolveMessage(statusCode, status.getMessage()),
        isRetryableStatus(statusCode),
        cause);
  }

  /**
   * Transport failure: {@code APITimeoutError} on socket timeouts, else {@code APIConnectionError}.
   */
  public static ConnectionError connectionError(Throwable cause) {
    InternalErrorCode def =
        hasCause(cause, SocketTimeoutException.class)
            ? InternalErrorCode.SDK_AGENTSTUDIO_API_TIMEOUT_ERROR
            : InternalErrorCode.SDK_AGENTSTUDIO_API_CONNECTION_ERROR;
    String message = causeMessage(cause, def);
    return new ConnectionError(
        internalStatus(def, message), def.getCode(), message, def.isAllowRetry(), cause);
  }

  /** A stream read timeout, surfaced as {@code APITimeoutError}. */
  public static ConnectionError timeout(String message) {
    InternalErrorCode def = InternalErrorCode.SDK_AGENTSTUDIO_API_TIMEOUT_ERROR;
    String resolved = (message != null && !message.isEmpty()) ? message : def.getMessage();
    return new ConnectionError(
        internalStatus(def, resolved), def.getCode(), resolved, def.isAllowRetry(), null);
  }

  /** A fatal SSE protocol error. */
  public static StreamError streamError(Throwable cause) {
    InternalErrorCode def = InternalErrorCode.SDK_AGENTSTUDIO_STREAM_ERROR;
    String message = causeMessage(cause, def);
    return new StreamError(
        internalStatus(def, message), def.getCode(), message, def.isAllowRetry(), cause);
  }

  /** Raised when consumers attempt I/O on an already-closed stream. */
  public static StreamError streamClosed() {
    InternalErrorCode def = InternalErrorCode.SDK_AGENTSTUDIO_STREAM_CLOSED_ERROR;
    return new StreamError(
        internalStatus(def, def.getMessage()),
        def.getCode(),
        def.getMessage(),
        def.isAllowRetry(),
        null);
  }

  // --- Code / message normalization ------------------------------------------

  private static final Set<String> REGISTRY_ANTHROPIC_CODES = new HashSet<>();

  static {
    for (PublicErrorCode def : PublicErrorCode.values()) {
      REGISTRY_ANTHROPIC_CODES.add(def.getAnthropicErrorCode());
    }
  }

  /** Public code for a status error: the recognized server code, else generic {@code api_error}. */
  private static String publicCode(String serverCode) {
    String normalized = normalizeServerCode(serverCode);
    return normalized != null ? normalized : PublicErrorCode.INTERNAL_ERROR.getAnthropicErrorCode();
  }

  private static String normalizeServerCode(String code) {
    if (code == null || code.isEmpty()) {
      return null;
    }
    if (REGISTRY_ANTHROPIC_CODES.contains(code)) {
      return code; // already a unified Anthropic code
    }
    PublicErrorCode byErrorCode = PublicErrorCode.fromErrorCode(code);
    if (byErrorCode != null) {
      return byErrorCode.getAnthropicErrorCode(); // e.g. "NotFoundError" -> "not_found_error"
    }
    return null;
  }

  /** Retry policy for HTTP responses: transient statuses only. */
  private static boolean isRetryableStatus(int statusCode) {
    return statusCode == 408 || statusCode == 409 || statusCode == 429 || statusCode >= 500;
  }

  private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
    for (Throwable c = t; c != null; c = c.getCause()) {
      if (type.isInstance(c)) {
        return true;
      }
      if (c.getCause() == c) {
        break;
      }
    }
    return false;
  }

  private static String causeMessage(Throwable cause, InternalErrorCode def) {
    if (cause != null && cause.getMessage() != null && !cause.getMessage().isEmpty()) {
      return cause.getMessage();
    }
    return def.getMessage();
  }

  private static Status internalStatus(InternalErrorCode def, String message) {
    return Status.builder().statusCode(-1).code(def.getCode()).message(message).build();
  }

  /** Resolved message: the server's text when present, else a bare HTTP status. */
  static String resolveMessage(int statusCode, String serverMessage) {
    if (serverMessage != null && !serverMessage.isEmpty()) {
      return serverMessage;
    }
    return "HTTP " + statusCode;
  }
}
