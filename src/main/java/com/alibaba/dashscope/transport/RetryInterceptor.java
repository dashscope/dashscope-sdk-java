// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application-level retry policy (design §6):
 *
 * <ul>
 *   <li>retryable statuses {408, 409, 429, 500, 502, 503, 504} plus connection-level IO failures
 *   <li>honors {@code Retry-After} / {@code Retry-After-ms} / {@code X-Should-Retry}
 *   <li>exponential backoff 500ms → 8s with ±25% jitter
 *   <li>default maxRetries=2 (per-request override via {@link RequestTags.MaxRetries})
 *   <li>requests tagged {@link RequestTags.NoRetry} (SSE / WebSocket upgrade) are never retried:
 *       a failed stream cannot be safely replayed (double billing / duplicated results)
 * </ul>
 */
public final class RetryInterceptor implements Interceptor {
  private static final Logger log = LoggerFactory.getLogger(RetryInterceptor.class);

  private static final Set<Integer> RETRYABLE_STATUSES =
      new HashSet<>(Arrays.asList(408, 409, 429, 500, 502, 503, 504));
  private static final long INITIAL_BACKOFF_MS = 500;
  private static final long MAX_BACKOFF_MS = 8000;
  static final int DEFAULT_MAX_RETRIES = 2;

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    if (request.tag(RequestTags.NoRetry.class) != null) {
      return chain.proceed(request);
    }
    RequestTags.MaxRetries tag = request.tag(RequestTags.MaxRetries.class);
    int maxRetries = tag == null ? DEFAULT_MAX_RETRIES : tag.value;

    int attempt = 0;
    while (true) {
      Response response = null;
      IOException failure = null;
      try {
        response = chain.proceed(request);
      } catch (IOException e) {
        failure = e;
      }

      boolean retryable;
      long overrideDelayMs = -1;
      if (failure != null) {
        retryable = isRetryableIo(failure);
      } else {
        String shouldRetry = response.header("X-Should-Retry");
        if ("false".equalsIgnoreCase(shouldRetry)) {
          retryable = false;
        } else if ("true".equalsIgnoreCase(shouldRetry)) {
          retryable = true;
        } else {
          retryable = RETRYABLE_STATUSES.contains(response.code());
        }
        overrideDelayMs = retryAfterMs(response);
      }

      if (!retryable || attempt >= maxRetries) {
        if (failure != null) {
          throw failure;
        }
        return response;
      }

      if (response != null) {
        response.close();
      }
      long delayMs =
          overrideDelayMs >= 0 ? overrideDelayMs : backoffMs(attempt);
      log.debug("retrying request to {} (attempt {}/{}) after {}ms",
          request.url(), attempt + 1, maxRetries, delayMs);
      try {
        Thread.sleep(delayMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        if (failure != null) {
          throw failure;
        }
        return chain.proceed(request);
      }
      attempt++;
    }
  }

  /** Connection-level failures only: a read timeout mid-request is never retried — the server
   * may already be processing the call, and replaying it risks double billing. */
  private static boolean isRetryableIo(IOException e) {
    return e instanceof ConnectException
        || e instanceof UnknownHostException
        || (e instanceof SocketException && !(e instanceof java.net.SocketTimeoutException));
  }

  private static long backoffMs(int attempt) {
    long base = Math.min(MAX_BACKOFF_MS, INITIAL_BACKOFF_MS << attempt);
    double jitter = 0.75 + ThreadLocalRandom.current().nextDouble() * 0.5;
    return (long) (base * jitter);
  }

  /** {@code Retry-After-ms} wins over {@code Retry-After} (seconds); absent → -1. */
  private static long retryAfterMs(Response response) {
    String afterMs = response.header("Retry-After-ms");
    if (afterMs != null) {
      try {
        return Long.parseLong(afterMs.trim());
      } catch (NumberFormatException e) {
        return -1;
      }
    }
    String after = response.header("Retry-After");
    if (after != null) {
      try {
        return Long.parseLong(after.trim()) * 1000;
      } catch (NumberFormatException e) {
        return -1;
      }
    }
    return -1;
  }
}
