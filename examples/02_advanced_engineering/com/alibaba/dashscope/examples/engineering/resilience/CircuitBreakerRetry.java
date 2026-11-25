// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.engineering.resilience;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.DashScopeException;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * High availability: cross-region failover guarded by a hand-rolled circuit breaker, layered on top
 * of the SDK's built-in retry ({@code ClientOptions.maxRetries} / {@code RequestOptions.maxRetries}
 * already absorb transient 429/5xx <em>within</em> a region — the breaker exists to fail
 * <em>across</em> regions when a whole region keeps erroring).
 *
 * <p>Translation of the Python blueprint example 02_advanced_engineering/circuit_breaker_retry.py.
 *
 * <p>The demo runs fully offline and deterministically: per-region JDK {@link HttpServer} instances
 * play the gateways (the primary region 503s, the secondary 200s). Drop the {@code baseUrl}
 * override to hit the real gateways — note that API keys are region-specific.
 */
public final class CircuitBreakerRetry {

  /** Fail over across these regions, in order. */
  private static final List<String> REGIONS = Arrays.asList("cn-beijing", "ap-southeast-1");

  private enum BreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  /** Minimal per-region circuit breaker (CLOSED/OPEN/HALF_OPEN). */
  static final class CircuitBreaker {
    private final int failureThreshold;
    private final double resetTimeoutSeconds;
    private BreakerState state = BreakerState.CLOSED;
    private int failureCount;
    private long openedAtNanos;

    CircuitBreaker(int failureThreshold, double resetTimeoutSeconds) {
      this.failureThreshold = failureThreshold;
      this.resetTimeoutSeconds = resetTimeoutSeconds;
    }

    /** Whether a request may be attempted right now. */
    synchronized boolean allow() {
      if (state == BreakerState.OPEN) {
        long elapsedNanos = System.nanoTime() - openedAtNanos;
        if (elapsedNanos >= (long) (resetTimeoutSeconds * 1_000_000_000L)) {
          state = BreakerState.HALF_OPEN;
          return true;
        }
        return false;
      }
      return true;
    }

    synchronized void recordSuccess() {
      failureCount = 0;
      state = BreakerState.CLOSED;
    }

    synchronized void recordFailure() {
      failureCount++;
      if (state == BreakerState.HALF_OPEN || failureCount >= failureThreshold) {
        state = BreakerState.OPEN;
        openedAtNanos = System.nanoTime();
      }
    }

    synchronized BreakerState state() {
      return state;
    }
  }

  /** Connection errors and 5xx / 429 are worth failing over on. */
  private static boolean isRetryable(DashScopeException exc) {
    if (exc instanceof ApiConnectionException) {
      return true;
    }
    Integer status = exc.getStatusCode();
    return status != null && (status >= 500 || status == 429);
  }

  /** One answered call: which region served it plus the completion itself. */
  static final class ChatOutcome {
    final String region;
    final ChatCompletion completion;

    ChatOutcome(String region, ChatCompletion completion) {
      this.region = region;
      this.completion = completion;
    }
  }

  /** Try each region in order, guarded by a circuit breaker. */
  static final class MultiRegionClient implements AutoCloseable {
    private final List<String> regions;
    private final Function<String, DashScopeClient> clientFactory;
    // Written only in the constructor and held in a final field, so safely published as-is.
    private final Map<String, CircuitBreaker> breakers = new LinkedHashMap<>();
    // chat() may be called from a request pool (CircuitBreaker's own methods are synchronized for
    // exactly that reason), so this lazily filled cache must be thread-safe too: a racing
    // computeIfAbsent on a plain map can drop an entry, and that client would then never be closed
    // because close() only sees what the map still holds. This makes concurrent creation safe, not
    // concurrent creation-and-close: close() must still run after all chat() calls have returned.
    private final Map<String, DashScopeClient> clients = new ConcurrentHashMap<>();

    MultiRegionClient(
        List<String> regions,
        Function<String, DashScopeClient> clientFactory,
        int failureThreshold,
        double resetTimeoutSeconds) {
      this.regions = new ArrayList<>(regions);
      this.clientFactory = clientFactory;
      for (String region : regions) {
        breakers.put(region, new CircuitBreaker(failureThreshold, resetTimeoutSeconds));
      }
    }

    private DashScopeClient client(String region) {
      return clients.computeIfAbsent(region, clientFactory);
    }

    /** Return the outcome from the first healthy region. */
    ChatOutcome chat(String prompt, String model) {
      DashScopeException lastError = null;
      List<String> openCircuits = new ArrayList<>();
      for (String region : regions) {
        CircuitBreaker breaker = breakers.get(region);
        if (!breaker.allow()) {
          openCircuits.add(region);
          System.out.println("  [" + region + "] circuit OPEN -> skip");
          continue;
        }
        try {
          ChatCompletion completion =
              client(region)
                  .chat()
                  .completions()
                  .create(
                      ChatCompletionCreateParams.builder()
                          .model(model)
                          .messages(Collections.singletonList(Message.user(prompt)))
                          .build());
          breaker.recordSuccess();
          System.out.println("  [" + region + "] OK (circuit " + breaker.state() + ")");
          return new ChatOutcome(region, completion);
        } catch (DashScopeException exc) {
          lastError = exc;
          boolean retryable = isRetryable(exc);
          if (retryable) {
            breaker.recordFailure();
          }
          System.out.println(
              "  ["
                  + region
                  + "] "
                  + exc.getClass().getSimpleName()
                  + " status="
                  + exc.getStatusCode()
                  + " retryable="
                  + retryable
                  + " -> circuit "
                  + breaker.state());
          if (!retryable) {
            throw exc; // a 4xx is a real bug; do not mask it
          }
        }
      }
      if (lastError == null) {
        // Every region was skipped by an open circuit, so no call was attempted and there is no
        // underlying failure to report; name the open circuits instead of printing "...: null".
        throw new ApiConnectionException(
            "all regions unavailable, circuits open: " + openCircuits, null);
      }
      throw new ApiConnectionException(
          "all regions failed: "
              + lastError
              + (openCircuits.isEmpty() ? "" : " (skipped, circuits open: " + openCircuits + ")"),
          lastError);
    }

    @Override
    public void close() {
      clients.values().forEach(DashScopeClient::close);
    }
  }

  // ---- offline gateways: one canned HTTP server per region ---- //

  private static void respond(HttpExchange exchange, int status, String json) throws IOException {
    byte[] body = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, body.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(body);
    }
    exchange.close();
  }

  /**
   * Offline gateway: the primary region 503s, every other 200s. The handler closes over {@code
   * region} instead of sniffing the request host, so it stays correct when a workspace changes the
   * gateway.
   */
  private static HttpServer startGateway(String region, boolean primary) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          if (primary) {
            respond(
                exchange,
                503,
                "{\"code\":\"ServiceUnavailable\",\"message\":\"mocked regional outage\"}");
            return;
          }
          respond(
              exchange,
              200,
              "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":"
                  + "{\"role\":\"assistant\",\"content\":\"来自 "
                  + region
                  + " 的应答\"}}]},"
                  + "\"usage\":{\"total_tokens\":10},"
                  + "\"request_id\":\"mock-"
                  + region
                  + "\"}");
        });
    server.start();
    return server;
  }

  /** Drive three calls to show the breaker opening and failing over. */
  public static void main(String[] args) throws Exception {
    List<HttpServer> servers = new ArrayList<>();
    Map<String, String> gatewayUrls = new LinkedHashMap<>();
    for (String region : REGIONS) {
      // Offline demo: injected mock gateway + dummy creds. Real regional use needs a
      // region-specific workspace AND API key.
      HttpServer server = startGateway(region, region.equals(REGIONS.get(0)));
      servers.add(server);
      gatewayUrls.put(region, "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1");
    }
    try {
      MultiRegionClient mc =
          new MultiRegionClient(
              REGIONS,
              region ->
                  DashScopeClient.builder()
                      .apiKey("sk-mock")
                      .region(region)
                      .baseUrl(gatewayUrls.get(region))
                      .maxRetries(0) // keep the demo instant; SDK retry is a separate layer
                      .build(),
              2,
              30.0);
      try {
        // Three calls show the breaker progressing CLOSED -> OPEN: the first two probe the dead
        // primary and fail over; the third sees the primary OPEN and goes straight to the
        // healthy region.
        for (int i = 1; i <= 3; i++) {
          System.out.println("----- call " + i + " -----");
          ChatOutcome outcome = mc.chat("你好", "qwen3.7-plus");
          System.out.println("  served by: " + outcome.region);
          System.out.println("  answer   : " + outcome.completion.outputText());
          System.out.println("  request_id: " + outcome.completion.getRequestId());
        }
      } finally {
        mc.close();
      }
    } finally {
      servers.forEach(server -> server.stop(0));
    }
  }
}
