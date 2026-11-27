// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.applications.agentstudio.common.Status;
import com.alibaba.dashscope.applications.agentstudio.exception.ApiException;
import com.alibaba.dashscope.applications.agentstudio.exception.NoApiKeyException;
import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.errors.AuthenticationException;
import com.alibaba.dashscope.errors.NotFoundException;
import com.alibaba.dashscope.errors.RateLimitException;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.errors.SdkException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class AgentStudioErrorsTest {

  private static ApiException apiException(int statusCode, String code, String message) {
    return new ApiException(
        Status.builder()
            .statusCode(statusCode)
            .code(code)
            .message(message)
            .requestId("req-1")
            .build());
  }

  @Test
  void notFoundPreservesFields() {
    NotFoundException ex =
        assertThrows(
            NotFoundException.class,
            () -> {
              throw AgentStudioErrors.translate(
                  apiException(404, "AgentNotFound", "agent not found"));
            });
    assertEquals(404, ex.getStatusCode());
    assertEquals("AgentNotFound", ex.getCode());
    assertEquals("agent not found", ex.getMessage());
    assertEquals("req-1", ex.getRequestId());
  }

  @Test
  void invalidApiKeyMapsToAuthentication() {
    AuthenticationException ex =
        assertThrows(
            AuthenticationException.class,
            () -> {
              throw AgentStudioErrors.translate(apiException(401, "InvalidApiKey", "bad key"));
            });
    assertEquals(401, ex.getStatusCode());
    assertEquals("InvalidApiKey", ex.getCode());
  }

  @Test
  void unmappedWireCodeLandsOnPlainApiStatus() {
    ApiStatusException ex =
        assertThrows(
            ApiStatusException.class,
            () -> {
              throw AgentStudioErrors.translate(
                  apiException(409, "memory_path_conflict_error", "Memory path already exists."));
            });
    assertEquals(ApiStatusException.class, ex.getClass());
    assertEquals(409, ex.getStatusCode());
    assertEquals("memory_path_conflict_error", ex.getCode());
  }

  @Test
  void businessCodeLayerPinsStatus200() {
    RateLimitException ex =
        assertThrows(
            RateLimitException.class,
            () -> {
              throw AgentStudioErrors.translate(apiException(200, "Throttling", "slow down"));
            });
    assertEquals(200, ex.getStatusCode());
    assertEquals("Throttling", ex.getCode());
  }

  @Test
  void networkErrorMapsToConnection() {
    ApiConnectionException ex =
        assertThrows(
            ApiConnectionException.class,
            () -> {
              throw AgentStudioErrors.translate(new ApiException(new ConnectException("refused")));
            });
    assertEquals(SdkErrorCode.SDK_AGENTSTUDIO_API_CONNECTION_ERROR.getCode(), ex.getCode());
    assertTrue(ex.getMessage().contains("refused"));
  }

  @Test
  void socketTimeoutMapsToTimeout() {
    ApiTimeoutException ex =
        assertThrows(
            ApiTimeoutException.class,
            () -> {
              throw AgentStudioErrors.translate(
                  new ApiException(new SocketTimeoutException("read timed out")));
            });
    assertEquals(SdkErrorCode.SDK_AGENTSTUDIO_API_TIMEOUT_ERROR.getCode(), ex.getCode());
  }

  @Test
  void streamTimeoutMapsToTimeout() {
    ApiTimeoutException ex =
        assertThrows(
            ApiTimeoutException.class,
            () -> {
              throw AgentStudioErrors.translate(apiException(-1, "stream_timeout", "no event"));
            });
    assertEquals(SdkErrorCode.SDK_AGENTSTUDIO_API_TIMEOUT_ERROR.getCode(), ex.getCode());
    assertEquals("no event", ex.getMessage());
  }

  @Test
  void noApiKeyMapsToCatalogSdkError() {
    SdkException ex =
        assertThrows(
            SdkException.class,
            () -> {
              throw AgentStudioErrors.translate(new NoApiKeyException());
            });
    assertEquals(SdkErrorCode.SDK_INVALID_API_KEY.getCode(), ex.getCode());
    assertNotNull(ex.getSolution());
  }

  @Test
  void syncAndAsyncPathsShareTheSameFamily() {
    CompletableFuture<Object> future =
        AsyncHelper.failedFuture(apiException(404, "AgentNotFound", "agent not found"));
    assertThrows(NotFoundException.class, () -> AsyncHelper.joinAndUnwrap(future));
    CompletionException completion = assertThrows(CompletionException.class, future::join);
    assertTrue(completion.getCause() instanceof NotFoundException);
  }

  @Test
  void validationErrorsPassThroughUnwrapped() {
    IllegalArgumentException validation = new IllegalArgumentException("agentId is required!");
    CompletableFuture<Object> future = AsyncHelper.failedFuture(validation);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> AsyncHelper.joinAndUnwrap(future));
    assertSame(validation, ex);
  }
}
