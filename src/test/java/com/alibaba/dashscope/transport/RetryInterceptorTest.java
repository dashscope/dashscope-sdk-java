// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

class RetryInterceptorTest {
  private static final long NOW = Instant.parse("2026-11-04T00:00:00Z").toEpochMilli();

  @Test
  void httpDateUsesRemainingDelay() {
    assertEquals(5000, delay("Wed, 04 Nov 2026 00:00:05 GMT"));
    assertEquals(0, delay("Wed, 04 Nov 2026 00:00:00 GMT"));
    assertEquals(0, delay("Tue, 03 Nov 2026 23:59:59 GMT"));
  }

  @Test
  void obsoleteHttpDateFormatsRemainSupported() {
    assertEquals(5000, delay("Wednesday, 04-Nov-26 00:00:05 GMT"));
    assertEquals(5000, delay("Wed Nov  4 00:00:05 2026"));
  }

  @Test
  void deltaSecondsRemainSupported() {
    assertEquals(2000, delay("2"));
    assertEquals(0, delay("0"));
    assertEquals(2000, delay(" 2 "));
  }

  @Test
  void invalidOrMissingHeadersFallBackToBackoff() {
    assertEquals(-1, delay(null));
    assertEquals(-1, delay("not a date"));
    assertEquals(-1, delay("9223372036854775807"));
  }

  @Test
  void millisecondHeaderHasPriorityOverDateAndSeconds() {
    Response response =
        responseBuilder()
            .header("Retry-After-ms", "25")
            .header("Retry-After", "Wed, 04 Nov 2026 00:00:05 GMT")
            .build();
    assertEquals(25, RetryInterceptor.retryAfterMs(response, NOW));
    assertEquals(
        25,
        RetryInterceptor.retryAfterMs(
            response.newBuilder().header("Retry-After", "2").build(), NOW));
  }

  @Test
  void invalidMillisecondHeaderUsesBackoff() {
    assertEquals(
        -1,
        RetryInterceptor.retryAfterMs(
            responseBuilder()
                .header("Retry-After-ms", "invalid")
                .header("Retry-After", "2")
                .build(),
            NOW));
  }

  private static long delay(String value) {
    Response.Builder builder = responseBuilder();
    if (value != null) {
      builder.header("Retry-After", value);
    }
    return RetryInterceptor.retryAfterMs(builder.build(), NOW);
  }

  private static Response.Builder responseBuilder() {
    return new Response.Builder()
        .request(new Request.Builder().url("http://127.0.0.1/").build())
        .protocol(Protocol.HTTP_1_1)
        .code(429)
        .message("Too Many Requests");
  }
}
