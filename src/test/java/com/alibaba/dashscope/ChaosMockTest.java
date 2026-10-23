// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.v3.config.ConfigResolver;
import com.alibaba.dashscope.v3.errors.ApiConnectionException;
import com.alibaba.dashscope.v3.errors.ApiTimeoutException;
import com.alibaba.dashscope.v3.resources.audio.RealtimeSession;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.ws.TaskEvent;
import com.alibaba.dashscope.v3.transport.ws.WsSession;
import com.alibaba.dashscope.v3.types.audio.RealtimeParams;
import com.alibaba.dashscope.v3.types.common.ResponseStream;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.v3.types.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Chaos tests (M5; audio design §11): mid-stream EOF, no-response timeout, half-open connection
 * detection via {@code wsPingInterval}, and close during an in-flight connect (2.x c891540 parity).
 * MockWebServer auto-answers ws pings and never hangs silently, so the ws cases run against a raw
 * scripted TCP endpoint.
 */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@ClearEnvironmentVariable(key = "DASHSCOPE_WEBSOCKET_BASE_URL")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class ChaosMockTest {

  @Test
  void httpNoResponseMapsToApiTimeout() throws IOException {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
      try (DashScopeClient client =
          DashScopeClient.builder()
              .apiKey("test-key")
              .baseUrl(server.url("/").toString())
              .timeout(Duration.ofSeconds(1))
              .maxRetries(0)
              .build()) {
        assertThrows(ApiTimeoutException.class, () -> client.chat().completions().create(params()));
      }
    }
  }

  @Test
  void httpDisconnectDuringSseBodyRaisesConnectionException() throws IOException {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setBody(
                  "data: {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                      + "\"content\":\"你\"}}]},\"request_id\":\"chaos\"}\n\n")
              .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
      try (DashScopeClient client =
          DashScopeClient.builder()
              .apiKey("test-key")
              .baseUrl(server.url("/").toString())
              .timeout(Duration.ofSeconds(5))
              .maxRetries(0)
              .build()) {
        try (ResponseStream<ChatCompletionChunk> stream =
            client.chat().completions().createStreaming(params())) {
          assertThrows(
              ApiConnectionException.class,
              () -> {
                for (ChatCompletionChunk ignored : stream) {
                  // drain until the truncated body fails the read
                }
              });
        }
      }
    }
  }

  @Test
  void wsTaskMidStreamEofRaisesConnectionException() throws Exception {
    try (RawWsServer raw = new RawWsServer(RawWsServer.Behavior.HANDSHAKE_TASK_STARTED_THEN_EOF)) {
      HttpTransport transport =
          new HttpTransport(
              ConfigResolver.resolve(ClientOptions.builder().apiKey("test-key").build()));
      try {
        WsSession session = new WsSession(transport, raw.wsUrl(), "t1", "audio", null);
        raw.awaitAccepted(1);
        TaskEvent started = session.recv(Duration.ofSeconds(10));
        assertEquals(TaskEvent.Kind.TEXT, started.getKind());
        assertEquals("task-started", started.getEvent());
        // Server closed the TCP connection without a ws close frame → EOF → connection error.
        assertThrows(ApiConnectionException.class, () -> session.recv(Duration.ofSeconds(10)));
      } finally {
        transport.close();
      }
    }
  }

  @Test
  void wsHalfOpenConnectionFailsFastWithPing() throws Exception {
    try (RawWsServer raw = new RawWsServer(RawWsServer.Behavior.HANDSHAKE_THEN_SILENCE);
        DashScopeClient client =
            DashScopeClient.builder()
                .apiKey("test-key")
                .baseUrl(raw.httpBaseUrl())
                .wsPingInterval(Duration.ofMillis(500))
                .timeout(Duration.ofSeconds(30))
                .maxRetries(0)
                .build()) {
      RealtimeSession rt =
          client
              .audio()
              .realtime()
              .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build());
      raw.awaitAccepted(1);
      long start = System.nanoTime();
      Iterator<JsonNode> it = rt.events().iterator();
      // The server answers nothing, not even pongs: OkHttp's ping must fail the socket well
      // before the 30 s receive timeout.
      assertThrows(ApiConnectionException.class, it::hasNext);
      long elapsedMs = (System.nanoTime() - start) / 1_000_000;
      assertTrue(
          elapsedMs < 10_000,
          "ping should detect the half-open connection fast, took " + elapsedMs);
      rt.close();
    }
  }

  @Test
  void wsCloseDuringConnectFailsConnectPromptly() throws Exception {
    try (RawWsServer raw =
            new RawWsServer(
                RawWsServer.Behavior.HANDSHAKE_THEN_SILENCE,
                RawWsServer.Behavior.ACCEPT_THEN_SILENCE);
        DashScopeClient client =
            DashScopeClient.builder()
                .apiKey("test-key")
                .baseUrl(raw.httpBaseUrl())
                .timeout(Duration.ofSeconds(5))
                .maxRetries(0)
                .build()) {
      RealtimeSession rt =
          client
              .audio()
              .realtime()
              .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build());
      raw.awaitAccepted(1);
      rt.close();

      AtomicReference<Throwable> connectFailure = new AtomicReference<>();
      Thread connectThread =
          new Thread(
              () -> {
                try {
                  rt.connect();
                } catch (Throwable t) {
                  connectFailure.set(t);
                }
              },
              "chaos-reconnect");
      connectThread.setDaemon(true);
      connectThread.start();
      raw.awaitAccepted(2); // reconnect is now blocked in the handshake against a silent peer

      long start = System.nanoTime();
      rt.close();
      long closeMs = (System.nanoTime() - start) / 1_000_000;
      connectThread.join(10_000);

      assertFalse(connectThread.isAlive(), "connect() still blocked 10 s after close()");
      assertTrue(closeMs < 5_000, "close() blocked for " + closeMs + " ms during connect");
      assertTrue(
          connectFailure.get() instanceof ApiConnectionException,
          "expected ApiConnectionException, got " + connectFailure.get());
    }
  }

  private static ChatCompletionCreateParams params() {
    return ChatCompletionCreateParams.builder()
        .model("qwen-plus")
        .messages(Collections.singletonList(Message.user("hi")))
        .build();
  }

  /**
   * Raw scripted websocket endpoint for cases MockWebServer cannot express: a peer that completes
   * the 101 handshake then goes permanently silent (half-open), drops TCP mid-task without a close
   * frame, or accepts TCP without ever answering the handshake.
   */
  private static final class RawWsServer implements AutoCloseable {
    enum Behavior {
      /** Full 101 handshake, then permanent silence (pings ignored). */
      HANDSHAKE_THEN_SILENCE,
      /** Full 101 handshake, one task-started text frame, then abrupt TCP EOF. */
      HANDSHAKE_TASK_STARTED_THEN_EOF,
      /** TCP accepted, handshake never answered. */
      ACCEPT_THEN_SILENCE
    }

    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final ServerSocket listener;
    private final List<Behavior> behaviors;
    private final List<Socket> openSockets = new CopyOnWriteArrayList<>();
    private final AtomicInteger accepted = new AtomicInteger(0);
    private final AtomicReference<Throwable> serverError = new AtomicReference<>();
    private volatile boolean stopped;

    RawWsServer(Behavior... behaviors) throws IOException {
      this.listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
      this.behaviors = new ArrayList<>(Arrays.asList(behaviors));
      Thread acceptThread = new Thread(this::acceptLoop, "raw-ws-accept");
      acceptThread.setDaemon(true);
      acceptThread.start();
    }

    String wsUrl() {
      return "ws://127.0.0.1:" + listener.getLocalPort() + "/api-ws/v1/inference";
    }

    String httpBaseUrl() {
      return "http://127.0.0.1:" + listener.getLocalPort() + "/";
    }

    void awaitAccepted(int count) throws InterruptedException {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
      while (accepted.get() < count && System.nanoTime() < deadline) {
        Thread.sleep(10);
      }
      assertEquals(count, accepted.get(), "raw server did not see " + count + " connections");
    }

    private void acceptLoop() {
      int index = 0;
      while (!stopped && index < behaviors.size()) {
        final Socket socket;
        try {
          socket = listener.accept();
        } catch (IOException e) {
          if (!stopped) {
            serverError.compareAndSet(null, e);
          }
          return;
        }
        Behavior behavior = behaviors.get(index++);
        accepted.incrementAndGet();
        openSockets.add(socket);
        Thread handler = new Thread(() -> handle(socket, behavior), "raw-ws-conn-" + index);
        handler.setDaemon(true);
        handler.start();
      }
    }

    private void handle(Socket socket, Behavior behavior) {
      try {
        if (behavior == Behavior.ACCEPT_THEN_SILENCE) {
          holdSilently(socket);
          return;
        }
        handshake(socket);
        if (behavior == Behavior.HANDSHAKE_TASK_STARTED_THEN_EOF) {
          writeTextFrame(
              socket,
              "{\"header\":{\"task_id\":\"t1\",\"event\":\"task-started\"},\"payload\":{}}");
          Thread.sleep(200);
          return; // finally closes the socket: abrupt EOF without a ws close frame
        }
        holdSilently(socket); // HANDSHAKE_THEN_SILENCE: never answer pings
      } catch (Throwable t) {
        if (!stopped) {
          serverError.compareAndSet(null, t);
        }
      } finally {
        try {
          socket.close();
        } catch (IOException ignored) {
        }
      }
    }

    private void holdSilently(Socket socket) throws IOException {
      // Drain (and discard) whatever the client sends, otherwise stay silent until tearDown.
      byte[] drain = new byte[4096];
      try {
        socket.setSoTimeout(1000);
        InputStream in = socket.getInputStream();
        while (!stopped) {
          try {
            if (in.read(drain) < 0) {
              return;
            }
          } catch (SocketTimeoutException ignored) {
          }
        }
      } finally {
        socket.setSoTimeout(0);
      }
    }

    private static void handshake(Socket socket) throws Exception {
      InputStream in = socket.getInputStream();
      ByteArrayOutputStream request = new ByteArrayOutputStream();
      int b;
      while ((b = in.read()) != -1) {
        request.write(b);
        if (request.toString("ISO-8859-1").endsWith("\r\n\r\n")) {
          break;
        }
      }
      String key = null;
      for (String line : request.toString("ISO-8859-1").split("\r\n")) {
        if (line.regionMatches(true, 0, "Sec-WebSocket-Key:", 0, 18)) {
          key = line.substring(18).trim();
        }
      }
      if (key == null) {
        throw new IOException("no Sec-WebSocket-Key in upgrade request");
      }
      String accept =
          Base64.getEncoder()
              .encodeToString(
                  MessageDigest.getInstance("SHA-1")
                      .digest((key + WS_GUID).getBytes(StandardCharsets.ISO_8859_1)));
      String response =
          "HTTP/1.1 101 Switching Protocols\r\n"
              + "Upgrade: websocket\r\n"
              + "Connection: Upgrade\r\n"
              + "Sec-WebSocket-Accept: "
              + accept
              + "\r\n\r\n";
      socket.getOutputStream().write(response.getBytes(StandardCharsets.ISO_8859_1));
      socket.getOutputStream().flush();
    }

    private static void writeTextFrame(Socket socket, String text) throws IOException {
      byte[] payload = text.getBytes(StandardCharsets.UTF_8);
      ByteArrayOutputStream frame = new ByteArrayOutputStream();
      frame.write(0x81); // FIN + text opcode
      if (payload.length < 126) {
        frame.write(payload.length);
      } else {
        frame.write(126);
        frame.write((payload.length >>> 8) & 0xFF);
        frame.write(payload.length & 0xFF);
      }
      frame.write(payload);
      socket.getOutputStream().write(frame.toByteArray());
      socket.getOutputStream().flush();
    }

    @Override
    public void close() {
      stopped = true;
      try {
        listener.close();
      } catch (IOException ignored) {
      }
      for (Socket socket : openSockets) {
        try {
          socket.close();
        } catch (IOException ignored) {
        }
      }
    }
  }
}
