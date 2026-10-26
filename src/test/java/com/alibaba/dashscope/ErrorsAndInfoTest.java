// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.AuthenticationException;
import com.alibaba.dashscope.errors.BadRequestException;
import com.alibaba.dashscope.errors.ContentFilterException;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.GatewayTimeoutException;
import com.alibaba.dashscope.errors.InternalServerException;
import com.alibaba.dashscope.errors.NotFoundException;
import com.alibaba.dashscope.errors.PermissionDeniedException;
import com.alibaba.dashscope.errors.PublicErrorCode;
import com.alibaba.dashscope.errors.RateLimitException;
import com.alibaba.dashscope.errors.RequestTooLargeException;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.errors.SdkException;
import com.alibaba.dashscope.errors.ServiceUnavailableException;
import com.alibaba.dashscope.shared.SdkInfo;
import com.alibaba.dashscope.shared.SdkSession;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ErrorsAndInfoTest {

  private static ApiStatusException fromStatus(int status, String code) {
    return Errors.fromStatus(status, code, null, "req-1", null, Collections.emptyMap());
  }

  @Test
  void statusLayerMapsToSubclasses() {
    assertSame(BadRequestException.class, fromStatus(400, "InvalidParameter").getClass());
    assertSame(AuthenticationException.class, fromStatus(401, "InvalidApiKey").getClass());
    assertSame(PermissionDeniedException.class, fromStatus(403, "AccessDenied").getClass());
    assertSame(NotFoundException.class, fromStatus(404, "Model.NotExist").getClass());
    assertSame(RequestTooLargeException.class, fromStatus(413, null).getClass());
    assertSame(RateLimitException.class, fromStatus(429, "Throttling").getClass());
    assertSame(InternalServerException.class, fromStatus(500, "InternalError").getClass());
    assertSame(ServiceUnavailableException.class, fromStatus(503, "ServiceUnavailable").getClass());
    assertSame(GatewayTimeoutException.class, fromStatus(504, "GatewayTimeout").getClass());
    assertSame(ApiStatusException.class, fromStatus(418, null).getClass());
  }

  @Test
  void statusLayerFallsBackToClassForUnknownCode() {
    assertSame(RateLimitException.class, fromStatus(429, "Unlisted.Code").getClass());
    assertSame(ApiStatusException.class, fromStatus(522, null).getClass());
  }

  @Test
  void blankMessageUsesCatalogOnlyForUnambiguousStatus() {
    assertEquals(
        "The requested resource was not found: {resource}.", fromStatus(404, null).getMessage());
    assertEquals("HTTP 400", fromStatus(400, null).getMessage());
    assertEquals("HTTP 418", fromStatus(418, null).getMessage());
  }

  @Test
  void businessCodeLayerPinsStatus200() {
    ApiStatusException auth =
        Errors.fromBusinessCode("InvalidApiKey", "bad key", "r", null, Collections.emptyMap());
    assertSame(AuthenticationException.class, auth.getClass());
    assertEquals(200, auth.getStatusCode());
    assertEquals("InvalidApiKey", auth.getCode());

    ApiStatusException filter =
        Errors.fromBusinessCode(
            "DataInspectionFailed", "unsafe", "r", null, Collections.emptyMap());
    assertSame(ContentFilterException.class, filter.getClass());
    assertEquals(200, filter.getStatusCode());

    ApiStatusException unknown =
        Errors.fromBusinessCode("Foo.Bar", "x", "r", null, Collections.emptyMap());
    assertSame(ApiStatusException.class, unknown.getClass());
    assertEquals(200, unknown.getStatusCode());
  }

  @Test
  void arrearageMapsToPermissionDeniedNotRateLimit() {
    assertSame(
        PermissionDeniedException.class,
        Errors.fromBusinessCode("Arrearage", "overdue", "r", null, Collections.emptyMap())
            .getClass());
  }

  @Test
  void sdkErrorCodeCarriesSolutionAndRetryFlag() {
    SdkException e = Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY);
    assertEquals("sdk.InvalidApiKey", e.getCode());
    assertFalse(e.isAllowRetry());
    assertTrue(e.getSolution().contains("DASHSCOPE_API_KEY"));
    assertNull(e.getStatusCode());
  }

  @Test
  void sdkErrorVarsAreDefensivelyCopied() {
    String[] vars = SdkErrorCode.SDK_WS_ILLEGAL_STATE.getVars();
    vars[0] = "changed";
    assertEquals("detail", SdkErrorCode.SDK_WS_ILLEGAL_STATE.getVars()[0]);
  }

  @Test
  void fromThrowableFallsBackToFamilyError() {
    SdkException e = Errors.fromThrowable(new IllegalStateException("boom"), "sdk.agentic_rl");
    assertEquals("sdk.agentic_rl.Error", e.getCode());
    assertEquals("boom", e.getMessage());
  }

  @Test
  void catalogEntryStaysPlainAndKeepsCause() {
    // An unparseable 2xx body is an SDK-side failure: no status subclass is fabricated,
    // statusCode stays null (the exchange never failed with 500), and the parse error
    // survives as the cause.
    Exception parseFailure = new java.io.IOException("unexpected character at line 1");
    ApiStatusException e =
        Errors.fromCatalogEntry(
            PublicErrorCode.RESPONSE_ERROR,
            "failed to parse response JSON: " + parseFailure.getMessage(),
            "req-9",
            null,
            Collections.emptyMap(),
            parseFailure);
    assertSame(ApiStatusException.class, e.getClass());
    assertEquals("ResponseError", e.getCode());
    assertNull(e.getStatusCode());
    assertEquals("req-9", e.getRequestId());
    assertSame(parseFailure, e.getCause());

    // A blank message resolves to the catalog text.
    ApiStatusException blank =
        Errors.fromCatalogEntry(
            PublicErrorCode.RESPONSE_ERROR, null, null, null, Collections.emptyMap(), null);
    assertEquals(PublicErrorCode.RESPONSE_ERROR.getErrorMsg(), blank.getMessage());
  }

  @Test
  void versionSingleSourceAndSharedSession() {
    // J1 sentinel: pom.xml is the only literal; this fails if Maven filtering breaks.
    assertEquals("3.0.0", SdkInfo.VERSION);
    assertEquals(SdkInfo.VERSION, Version.version);
    assertFalse(SdkSession.ID.isEmpty());
    assertSame(SdkSession.ID, SdkSession.ID);
  }
}
