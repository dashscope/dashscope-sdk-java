// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio;

import com.alibaba.dashscope.common.InternalErrorCode;
import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.exception.ApiException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Typed AgentStudio error. Codes converge onto {@link PublicErrorCode} (shared with the Python
 * SDK): {@link #getCode()} is the unified Anthropic-compatible code, the raw server code is on
 * {@link #getRawCode()}, and {@link #getKind()} branches on the error category. Extends {@link
 * ApiException} so existing {@code catch (ApiException)} still works.
 */
public class AgentStudioException extends ApiException {

  public enum Kind {
    INVALID_REQUEST,
    AUTHENTICATION,
    PERMISSION_DENIED,
    NOT_FOUND,
    CONFLICT,
    RATE_LIMIT,
    SERVER_ERROR,
    NETWORK,
    UNKNOWN
  }

  private final Kind kind;
  private final String code;
  private final String errorMessage;

  private AgentStudioException(
      Status status, Kind kind, String code, String errorMessage, Throwable cause) {
    super(status, cause);
    this.kind = kind;
    this.code = code;
    this.errorMessage = errorMessage;
  }

  public Kind getKind() {
    return kind;
  }

  public int getStatusCode() {
    return getStatus() != null ? getStatus().getStatusCode() : -1;
  }

  /** Unified error code from the Anthropic-compatible taxonomy (e.g. {@code not_found_error}). */
  public String getCode() {
    return code;
  }

  /** Raw server-supplied code before normalization (may be {@code null} or empty). */
  public String getRawCode() {
    return getStatus() != null ? getStatus().getCode() : null;
  }

  /** Resolved message: the server's text when present, else the registry default. */
  public String getErrorMessage() {
    return errorMessage;
  }

  public String getRequestId() {
    return getStatus() != null ? getStatus().getRequestId() : null;
  }

  public boolean isRetryable() {
    return isRetryable(getStatusCode());
  }

  /** A -1 status code marks a network-level failure (no HTTP response). */
  public static boolean isRetryable(int statusCode) {
    return statusCode == -1
        || statusCode == 408
        || statusCode == 409
        || statusCode == 429
        || statusCode >= 500;
  }

  public static Kind classify(int statusCode) {
    if (statusCode == -1) {
      return Kind.NETWORK;
    }
    switch (statusCode) {
      case 400:
        return Kind.INVALID_REQUEST;
      case 401:
        return Kind.AUTHENTICATION;
      case 403:
        return Kind.PERMISSION_DENIED;
      case 404:
        return Kind.NOT_FOUND;
      case 409:
        return Kind.CONFLICT;
      case 429:
        return Kind.RATE_LIMIT;
      default:
        return statusCode >= 500 ? Kind.SERVER_ERROR : Kind.UNKNOWN;
    }
  }

  public static AgentStudioException wrap(ApiException e) {
    if (e instanceof AgentStudioException) {
      return (AgentStudioException) e;
    }
    Status status = e.getStatus();
    if (status == null) {
      status = Status.builder().statusCode(-1).build();
    }
    int statusCode = status.getStatusCode();
    String unifiedCode = unifyCode(statusCode, status.getCode());
    String message = resolveMessage(statusCode, status.getMessage());
    return new AgentStudioException(
        status, classify(statusCode), unifiedCode, message, e.getCause());
  }

  @Override
  public String getMessage() {
    return String.format(
        "[%s] status=%d code=%s message=%s request_id=%s",
        kind, getStatusCode(), code, errorMessage, getRequestId());
  }

  // --- Normalization (mirrors dashscope/agentstudio/exceptions.py) ---

  private static final Map<Integer, PublicErrorCode> STATUS_TO_PUBLIC = new HashMap<>();
  private static final Set<String> REGISTRY_ANTHROPIC_CODES = new HashSet<>();
  private static final Map<String, String> LEGACY_CODE_ALIASES = new HashMap<>();
  private static final Map<Kind, InternalErrorCode> KIND_TO_INTERNAL = new HashMap<>();
  private static final Pattern PLACEHOLDER = Pattern.compile("\\s*:?\\s*\\{[^}]+\\}");

  static {
    STATUS_TO_PUBLIC.put(400, PublicErrorCode.INVALID_REQUEST);
    STATUS_TO_PUBLIC.put(401, PublicErrorCode.AUTH_FAILED);
    STATUS_TO_PUBLIC.put(403, PublicErrorCode.PERMISSION_DENIED);
    STATUS_TO_PUBLIC.put(404, PublicErrorCode.RESOURCE_NOT_FOUND);
    STATUS_TO_PUBLIC.put(413, PublicErrorCode.REQUEST_TOO_LARGE);
    STATUS_TO_PUBLIC.put(429, PublicErrorCode.RATE_LIMIT_EXCEEDED);
    STATUS_TO_PUBLIC.put(500, PublicErrorCode.INTERNAL_ERROR);
    STATUS_TO_PUBLIC.put(502, PublicErrorCode.INTERNAL_ERROR);
    STATUS_TO_PUBLIC.put(503, PublicErrorCode.SERVICE_UNAVAILABLE);
    STATUS_TO_PUBLIC.put(504, PublicErrorCode.REQUEST_TIMEOUT);

    for (PublicErrorCode def : PublicErrorCode.values()) {
      REGISTRY_ANTHROPIC_CODES.add(def.getAnthropicErrorCode());
    }

    LEGACY_CODE_ALIASES.put("permission_denied_error", "permission_error"); // TODO(bma-fix)

    // Internal codes only where public has none: NETWORK (no response),
    // CONFLICT (409), UNKNOWN (unmapped non-5xx). All else resolves to public.
    KIND_TO_INTERNAL.put(Kind.NETWORK, InternalErrorCode.SDK_AGENTSTUDIO_NETWORK_ERROR);
    KIND_TO_INTERNAL.put(Kind.CONFLICT, InternalErrorCode.SDK_AGENTSTUDIO_CONFLICT);
    KIND_TO_INTERNAL.put(Kind.UNKNOWN, InternalErrorCode.SDK_AGENTSTUDIO_UNKNOWN_ERROR);
  }

  /**
   * Resolve the unified code: a recognized server code wins, else the per-status registry row, else
   * the kind default.
   */
  static String unifyCode(int statusCode, String serverCode) {
    String normalized = normalizeServerCode(serverCode);
    if (normalized != null) {
      return normalized;
    }
    PublicErrorCode pub = STATUS_TO_PUBLIC.get(statusCode);
    if (pub != null) {
      return pub.getAnthropicErrorCode();
    }
    // No status row: use the category's internal code (NETWORK/CONFLICT/UNKNOWN),
    // else fall back to the generic public api_error (unmapped 5xx).
    InternalErrorCode internal = KIND_TO_INTERNAL.get(classify(statusCode));
    return internal != null
        ? internal.getCode()
        : PublicErrorCode.INTERNAL_ERROR.getAnthropicErrorCode();
  }

  private static String normalizeServerCode(String code) {
    if (code == null || code.isEmpty()) {
      return null;
    }
    if (LEGACY_CODE_ALIASES.containsKey(code)) {
      return LEGACY_CODE_ALIASES.get(code);
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

  static String resolveMessage(int statusCode, String serverMessage) {
    if (serverMessage != null && !serverMessage.isEmpty()) {
      return serverMessage;
    }
    PublicErrorCode pub = STATUS_TO_PUBLIC.get(statusCode);
    return pub != null ? defaultMessage(pub) : "HTTP " + statusCode;
  }

  /** Default message with unresolved {@code {var}} placeholders stripped. */
  private static String defaultMessage(PublicErrorCode pub) {
    String msg = PLACEHOLDER.matcher(pub.getErrorMsg()).replaceAll("").trim();
    if (!msg.isEmpty() && !msg.endsWith(".")) {
      msg += ".";
    }
    return msg;
  }
}
