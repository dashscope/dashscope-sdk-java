// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.common.SdkErrorCode;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry-driven error factory (design §5). Three entries: {@link #fromStatus} for non-2xx HTTP,
 * {@link #fromBusinessCode} for the HTTP-200-with-business-code layer, {@link #fromSdkErrorCode}
 * for SDK-side failures. Unmapped wire codes are never guessed into a subclass: they land on plain
 * {@link ApiStatusException}.
 *
 * <p>Class selection is catalog-driven: the wire-code crosswalk only picks <em>which</em> {@link
 * PublicErrorCode} applies, and the exception class comes from that entry. The single sanctioned
 * refinement is {@code DataInspectionFailed} → {@link ContentFilterException} (subclass of the
 * class its catalog entry names).
 */
public final class Errors {
  private Errors() {}

  // ---- wire codes returned in response "code" (see Python v2 ErrorCode) ----
  private static final String WIRE_INVALID_API_KEY = "InvalidApiKey";
  private static final String WIRE_ACCESS_DENIED = "AccessDenied";
  private static final String WIRE_ACCESS_DENIED_UNPURCHASED = "AccessDenied.Unpurchased";
  private static final String WIRE_INVALID_PARAMETER = "InvalidParameter";
  private static final String WIRE_MODEL_NOT_EXIST = "Model.NotExist";
  private static final String WIRE_DATA_INSPECTION_FAILED = "DataInspectionFailed";
  private static final String WIRE_THROTTLING = "Throttling";
  private static final String WIRE_THROTTLING_RATE_QUOTA = "Throttling.RateQuota";
  private static final String WIRE_THROTTLING_ALLOCATION_QUOTA = "Throttling.AllocationQuota";
  private static final String WIRE_INTERNAL_ERROR = "InternalError";
  private static final String WIRE_SERVICE_UNAVAILABLE = "ServiceUnavailable";
  private static final String WIRE_GATEWAY_TIMEOUT = "GatewayTimeout";
  private static final String WIRE_ARREARAGE = "Arrearage";

  @FunctionalInterface
  private interface Factory {
    ApiStatusException create(
        String message,
        String code,
        Integer statusCode,
        String requestId,
        Object body,
        Map<String, String> headers);
  }

  private static final Map<String, PublicErrorCode> WIRE_TO_PUBLIC = new HashMap<>();
  private static final Map<String, PublicErrorCode> PUBLIC_BY_ERROR_CODE = new HashMap<>();
  private static final Map<Integer, PublicErrorCode> UNIQUE_STATUS_ENTRY = new HashMap<>();
  private static final Map<String, Factory> CLASS_BY_ERROR_CODE = new HashMap<>();
  private static final Map<Integer, Factory> CLASS_BY_STATUS = new HashMap<>();
  private static final Map<String, Factory> REFINED_BY_WIRE = new HashMap<>();
  private static final Map<String, SdkErrorCode> SDK_BY_CODE = new HashMap<>();

  static {
    // The SDK's own crosswalk from gateway wire code to catalog entry; the catalog
    // itself carries no wire codes. Arrearage is a refused request, not an exhausted
    // quota: insufficient_quota is 429 and therefore retryable, so it maps to 403.
    WIRE_TO_PUBLIC.put(WIRE_INVALID_API_KEY, PublicErrorCode.INVALID_API_KEY);
    WIRE_TO_PUBLIC.put(WIRE_ACCESS_DENIED, PublicErrorCode.PERMISSION_DENIED);
    WIRE_TO_PUBLIC.put(WIRE_ACCESS_DENIED_UNPURCHASED, PublicErrorCode.PERMISSION_DENIED);
    WIRE_TO_PUBLIC.put(WIRE_INVALID_PARAMETER, PublicErrorCode.INVALID_REQUEST);
    WIRE_TO_PUBLIC.put(WIRE_MODEL_NOT_EXIST, PublicErrorCode.RESOURCE_NOT_FOUND);
    WIRE_TO_PUBLIC.put(WIRE_DATA_INSPECTION_FAILED, PublicErrorCode.CONTENT_POLICY_VIOLATION);
    WIRE_TO_PUBLIC.put(WIRE_THROTTLING, PublicErrorCode.RATE_LIMIT_EXCEEDED);
    WIRE_TO_PUBLIC.put(WIRE_THROTTLING_RATE_QUOTA, PublicErrorCode.RATE_LIMIT_EXCEEDED);
    WIRE_TO_PUBLIC.put(WIRE_THROTTLING_ALLOCATION_QUOTA, PublicErrorCode.RATE_LIMIT_EXCEEDED);
    WIRE_TO_PUBLIC.put(WIRE_INTERNAL_ERROR, PublicErrorCode.INTERNAL_ERROR);
    WIRE_TO_PUBLIC.put(WIRE_SERVICE_UNAVAILABLE, PublicErrorCode.SERVICE_UNAVAILABLE);
    WIRE_TO_PUBLIC.put(WIRE_GATEWAY_TIMEOUT, PublicErrorCode.REQUEST_TIMEOUT);
    WIRE_TO_PUBLIC.put(WIRE_ARREARAGE, PublicErrorCode.PERMISSION_DENIED);

    REFINED_BY_WIRE.put(WIRE_DATA_INSPECTION_FAILED, ContentFilterException::new);

    CLASS_BY_ERROR_CODE.put("BadRequestError", BadRequestException::new);
    CLASS_BY_ERROR_CODE.put("AuthenticationError", AuthenticationException::new);
    CLASS_BY_ERROR_CODE.put("PermissionDeniedError", PermissionDeniedException::new);
    CLASS_BY_ERROR_CODE.put("NotFoundError", NotFoundException::new);
    CLASS_BY_ERROR_CODE.put("RequestTooLargeError", RequestTooLargeException::new);
    CLASS_BY_ERROR_CODE.put("RateLimitError", RateLimitException::new);
    CLASS_BY_ERROR_CODE.put("InternalServerError", InternalServerException::new);
    CLASS_BY_ERROR_CODE.put("ServiceUnavailableError", ServiceUnavailableException::new);
    CLASS_BY_ERROR_CODE.put("GatewayTimeoutError", GatewayTimeoutException::new);

    Map<Integer, Integer> statusCounts = new HashMap<>();
    for (PublicErrorCode entry : PublicErrorCode.values()) {
      statusCounts.merge(entry.getStatusCode(), 1, Integer::sum);
      if (entry.getErrorCode() != null) {
        PUBLIC_BY_ERROR_CODE.putIfAbsent(entry.getErrorCode(), entry);
      }
    }
    for (PublicErrorCode entry : PublicErrorCode.values()) {
      // Only statuses a single entry claims can pick a catalog message without
      // stating a reason the gateway never gave (400/401/429/500 carry siblings).
      if (statusCounts.get(entry.getStatusCode()) == 1) {
        UNIQUE_STATUS_ENTRY.put(entry.getStatusCode(), entry);
      }
    }

    // Status → class map is built from the subclasses' catalog-referenced STATUS
    // constants, never hand-written literals.
    CLASS_BY_STATUS.put(BadRequestException.STATUS, BadRequestException::new);
    CLASS_BY_STATUS.put(AuthenticationException.STATUS, AuthenticationException::new);
    CLASS_BY_STATUS.put(PermissionDeniedException.STATUS, PermissionDeniedException::new);
    CLASS_BY_STATUS.put(NotFoundException.STATUS, NotFoundException::new);
    CLASS_BY_STATUS.put(RequestTooLargeException.STATUS, RequestTooLargeException::new);
    CLASS_BY_STATUS.put(RateLimitException.STATUS, RateLimitException::new);
    CLASS_BY_STATUS.put(InternalServerException.STATUS, InternalServerException::new);
    CLASS_BY_STATUS.put(ServiceUnavailableException.STATUS, ServiceUnavailableException::new);
    CLASS_BY_STATUS.put(GatewayTimeoutException.STATUS, GatewayTimeoutException::new);

    for (SdkErrorCode entry : SdkErrorCode.values()) {
      SDK_BY_CODE.put(entry.getCode(), entry);
    }
  }

  private static Factory factoryFor(String wireCode, Integer statusCode) {
    if (wireCode != null) {
      Factory refined = REFINED_BY_WIRE.get(wireCode);
      if (refined != null) {
        return refined;
      }
      PublicErrorCode entry = WIRE_TO_PUBLIC.get(wireCode);
      if (entry != null) {
        Factory f = CLASS_BY_ERROR_CODE.get(entry.getErrorCode());
        if (f != null) {
          return f;
        }
      }
    }
    if (statusCode != null) {
      Factory f = CLASS_BY_STATUS.get(statusCode);
      if (f != null) {
        return f;
      }
    }
    return ApiStatusException::new;
  }

  /**
   * Catalog text for a failure that arrived without a message. Placeholders stay verbatim —
   * nothing here knows the {@code {parameter}} / {@code {resource}} / {@code {limit}} values.
   * An entry that cannot be identified yields no text rather than a sibling's guess.
   */
  private static String catalogMessage(String wireCode, Integer statusCode) {
    PublicErrorCode entry = wireCode == null ? null : WIRE_TO_PUBLIC.get(wireCode);
    if (entry == null && statusCode != null) {
      entry = UNIQUE_STATUS_ENTRY.get(statusCode);
    }
    return entry == null ? "" : entry.getErrorMsg();
  }

  /** Map a non-2xx HTTP response into the exception family. */
  public static ApiStatusException fromStatus(
      int statusCode,
      String code,
      String message,
      String requestId,
      Object body,
      Map<String, String> headers) {
    String resolved = isBlank(message) ? catalogMessage(code, statusCode) : message;
    if (isBlank(resolved)) {
      resolved = "HTTP " + statusCode;
    }
    return factoryFor(code, statusCode).create(resolved, code, statusCode, requestId, body, headers);
  }

  /** Map the HTTP-200-with-business-code layer. Status is pinned to 200 so callers can tell
   * which layer failed. */
  public static ApiStatusException fromBusinessCode(
      String code, String message, String requestId, Object body, Map<String, String> headers) {
    int statusCode = 200;
    String resolved = isBlank(message) ? catalogMessage(code, null) : message;
    if (isBlank(resolved)) {
      resolved = "business error, code=" + code;
    }
    return factoryFor(code, statusCode).create(resolved, code, statusCode, requestId, body, headers);
  }

  /** SDK-side failure straight from the catalog. */
  public static SdkException fromSdkErrorCode(SdkErrorCode entry, Map<String, String> vars) {
    String message = vars == null ? entry.getMessage() : entry.formatMessage(vars);
    return new SdkException(message, entry.getCode(), entry.getSolution(), entry.isAllowRetry(), null);
  }

  public static SdkException fromSdkErrorCode(SdkErrorCode entry) {
    return fromSdkErrorCode(entry, null);
  }

  /**
   * Translate a mount-layer subsystem exception into the 3.0 family. Two-tier lookup: {@code
   * <family>.<ClassName>} when the catalog carries that exact class, otherwise the family's
   * {@code <family>.Error} fallback. Message text and legacy numeric codes are never sniffed.
   */
  public static SdkException fromSdkErrorCode(String code, String message, Throwable cause) {
    SdkErrorCode entry = SDK_BY_CODE.get(code);
    if (entry == null) {
      return new SdkException(message, code, null, false, cause);
    }
    return new SdkException(message, entry.getCode(), entry.getSolution(), entry.isAllowRetry(), cause);
  }

  public static SdkException fromThrowable(Throwable exc, String family) {
    String name = family + "." + exc.getClass().getSimpleName();
    SdkErrorCode entry = SDK_BY_CODE.get(name);
    if (entry == null) {
      entry = SDK_BY_CODE.get(family + ".Error");
    }
    if (entry == null) {
      return new SdkException(String.valueOf(exc.getMessage()), name, null, false, exc);
    }
    return new SdkException(
        String.valueOf(exc.getMessage()), entry.getCode(), entry.getSolution(), entry.isAllowRetry(), exc);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isEmpty();
  }
}
