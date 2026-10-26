// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.engineering.testing;

import com.alibaba.dashscope.ClientOptions;
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.v3.errors.DashScopeException;
import com.alibaba.dashscope.v3.types.completions.ChatCompletion;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.v3.types.messages.Message;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Local mock debugging: develop against a canned gateway with zero network calls (beyond loopback)
 * and zero token cost. A JDK {@link HttpServer} serves canned chat envelopes in-process; pointing
 * the client at it via {@code builder.baseUrl(...)} (equivalently {@code -Ddashscope.base.url=...})
 * exercises both the success path and the error paths (bad parameter, rate limit, content filter)
 * deterministically — before spending a single real token.
 *
 * <p>Translation of the Python blueprint example 02_advanced_engineering/local_mock_debugging.py.
 *
 * <p>Drop the {@code baseUrl} override and the exact same calling code runs against the real
 * gateway.
 */
public final class LocalMockDebugging {

  private LocalMockDebugging() {}

  /** The native chat-completion envelope the real gateway returns. */
  private static String chatEnvelope(String text, String requestId) {
    return "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":"
        + "{\"role\":\"assistant\",\"content\":\""
        + text
        + "\"}}]},"
        + "\"usage\":{\"input_tokens\":8,\"output_tokens\":4,\"total_tokens\":12},"
        + "\"request_id\":\""
        + requestId
        + "\"}";
  }

  /**
   * Route by request body so one server serves many scenarios: send a message containing {@code
   * trigger-error} to exercise the error path; anything else returns a normal completion.
   */
  private static HttpServer startMockGateway() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String body = new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8);
          byte[] payload;
          int status;
          if (body.contains("trigger-error")) {
            status = 400;
            payload =
                "{\"code\":\"InvalidParameter\",\"message\":\"mocked bad request\"}"
                    .getBytes(StandardCharsets.UTF_8);
          } else {
            status = 200;
            payload =
                chatEnvelope("你好，这是本地 Mock 返回。", "mock-req-1").getBytes(StandardCharsets.UTF_8);
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
          exchange.sendResponseHeaders(status, payload.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
          }
          exchange.close();
        });
    server.start();
    return server;
  }

  /** A client whose whole exchange is served in-process; the key is never validated. */
  private static DashScopeClient mockClient(String baseUrl) {
    return DashScopeClient.builder().apiKey("sk-mock").baseUrl(baseUrl).build();
  }

  private static ChatCompletion chat(DashScopeClient client, String content) {
    return client
        .chat()
        .completions()
        .create(
            ChatCompletionCreateParams.builder()
                .model("qwen3.7-plus")
                .messages(Collections.singletonList(Message.user(content)))
                .build());
  }

  public static void main(String[] args) throws Exception {
    HttpServer server = startMockGateway();
    try {
      String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1";
      try (DashScopeClient client = mockClient(baseUrl)) {
        System.out.println("----- mocked success -----");
        ChatCompletion resp = chat(client, "你好");
        System.out.println("output_text : " + resp.outputText());
        System.out.println("request_id  : " + resp.getRequestId());
        System.out.println("total_tokens: " + resp.getUsage().getTotalTokens());

        // Error path 1: the SDK raises a typed exception by default.
        System.out.println("----- mocked error (raiseOnError=true) -----");
        try {
          chat(client, "trigger-error");
        } catch (DashScopeException exc) {
          System.out.println("caught      : " + exc.getClass().getSimpleName());
          System.out.println("code        : " + exc.getCode());
          System.out.println("status_code : " + exc.getStatusCode());
          System.out.println("message     : " + exc.getMessage());
        }

        // Error path 2: opt out of raising to inspect the error inline. The derived client
        // shares the parent's connection pool and must not outlive it.
        System.out.println("----- mocked error (raiseOnError=false) -----");
        DashScopeClient lenient =
            client.withOptions(ClientOptions.builder().raiseOnError(false).build());
        ChatCompletion err = chat(lenient, "trigger-error");
        System.out.println("is_error    : " + err.isError());
        System.out.println("code        : " + err.getCode());
        System.out.println("message     : " + err.getMessage());
      }
    } finally {
      server.stop(0);
    }
  }

  private static byte[] readAll(java.io.InputStream in) throws IOException {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) >= 0) {
      out.write(buf, 0, n);
    }
    return out.toByteArray();
  }
}
