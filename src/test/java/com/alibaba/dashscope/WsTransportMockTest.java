// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.config.ConfigResolver;
import com.alibaba.dashscope.errors.PermissionDeniedException;
import com.alibaba.dashscope.errors.SdkException;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.ws.TaskChannel;
import com.alibaba.dashscope.transport.ws.TaskEvent;
import com.alibaba.dashscope.transport.ws.WsConnection;
import com.alibaba.dashscope.transport.ws.WsSession;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer ws-upgrade tests for the demultiplexing transport (audio design §11). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class WsTransportMockTest {
  private static final Duration RECV_TIMEOUT = Duration.ofSeconds(10);

  private MockWebServer server;
  private HttpTransport transport;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    transport =
        new HttpTransport(
            ConfigResolver.resolve(
                ClientOptions.builder()
                    .apiKey("test-key")
                    .baseUrl(server.url("/").toString())
                    .wsBaseUrl(server.url("/api-ws/v1/inference").toString())
                    .build()));
  }

  @AfterEach
  void tearDown() throws IOException {
    transport.close();
    server.shutdown();
  }

  /** Captures the server-side socket on upgrade. */
  private static final class ServerSocket extends WebSocketListener {
    final CountDownLatch opened = new CountDownLatch(1);
    final AtomicReference<WebSocket> socket = new AtomicReference<>();
    final CountDownLatch closed = new CountDownLatch(1);

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
      socket.set(webSocket);
      opened.countDown();
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      webSocket.close(code, reason);
      closed.countDown();
    }

    void awaitOpen() throws InterruptedException {
      assertTrue(opened.await(10, TimeUnit.SECONDS), "server socket never opened");
    }

    void sendEvent(String taskId, String event) {
      socket
          .get()
          .send(
              "{\"header\":{\"task_id\":\""
                  + taskId
                  + "\",\"event\":\""
                  + event
                  + "\"},"
                  + "\"payload\":{\"output\":{},\"usage\":{}}}");
    }

    void sendFailed(String taskId, String code, String message) {
      socket
          .get()
          .send(
              "{\"header\":{\"task_id\":\""
                  + taskId
                  + "\",\"event\":\"task-failed\","
                  + "\"error_code\":\""
                  + code
                  + "\",\"error_message\":\""
                  + message
                  + "\"},"
                  + "\"payload\":{}}");
    }
  }

  private ServerSocket enqueueUpgrade() {
    ServerSocket listener = new ServerSocket();
    server.enqueue(new MockResponse().withWebSocketUpgrade(listener));
    return listener;
  }

  @Test
  void multipleTasksShareOneConnectionAndDemux() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      TaskChannel asr = conn.attach("task-asr");
      TaskChannel tts = conn.attach("task-tts");
      TaskChannel json3 = conn.attach("task-json");

      // Interleave events for the three tasks; each handle must receive only its own.
      serverSocket.sendEvent("task-asr", "task-started");
      serverSocket.sendEvent("task-tts", "task-started");
      serverSocket.sendEvent("task-json", "task-started");
      serverSocket.sendEvent("task-tts", "result-generated");
      serverSocket.sendEvent("task-asr", "result-generated");
      serverSocket.sendEvent("task-json", "result-generated");
      serverSocket.sendEvent("task-json", "task-finished");
      serverSocket.sendEvent("task-asr", "task-finished");
      serverSocket.sendEvent("task-tts", "task-finished");

      for (TaskChannel handle : new TaskChannel[] {asr, tts, json3}) {
        TaskEvent started = handle.recv(RECV_TIMEOUT);
        assertNotNull(started);
        assertEquals("task-started", started.getEvent());
        assertEquals(handle.taskId(), started.getTaskId());

        TaskEvent result = handle.recv(RECV_TIMEOUT);
        assertNotNull(result);
        assertEquals("result-generated", result.getEvent());

        TaskEvent finished = handle.recv(RECV_TIMEOUT);
        assertNotNull(finished);
        assertEquals("task-finished", finished.getEvent());
      }
    }
  }

  @Test
  void sharedConnectionSurvivesIdlePeriodBeyondHttpReadTimeout() throws Exception {
    // Design §5: WsConnection sockets block forever on read; the configured HTTP timeout must
    // not kill the demux reader during a quiet period (regression: inherited readTimeout).
    // The 1200ms idle also exceeds the inherited 500ms callTimeout, so both timeout paths are
    // covered. This is an end-to-end behavioral guard: it pins observable behavior (against
    // future okhttp upgrades), not the explicit readTimeout(0)/callTimeout(0) lines themselves.
    ServerSocket serverSocket = enqueueUpgrade();
    try (HttpTransport shortTimeout =
            new HttpTransport(
                ConfigResolver.resolve(
                    ClientOptions.builder()
                        .apiKey("test-key")
                        .baseUrl(server.url("/").toString())
                        .wsBaseUrl(server.url("/api-ws/v1/inference").toString())
                        .timeout(Duration.ofMillis(500))
                        .build()));
        WsConnection conn =
            new WsConnection(shortTimeout, shortTimeout.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      TaskChannel handle = conn.attach("task-idle");
      Thread.sleep(1200);
      serverSocket.sendEvent("task-idle", "result-generated");
      TaskEvent event = handle.recv(RECV_TIMEOUT);
      assertNotNull(event);
      assertEquals("result-generated", event.getEvent());
    }
  }

  @Test
  void concurrentReceiversOnSharedConnection() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      int taskCount = 4;
      int eventsPerTask = 5;
      TaskChannel[] handles = new TaskChannel[taskCount];
      for (int i = 0; i < taskCount; i++) {
        handles[i] = conn.attach("task-" + i);
      }
      List<AssertionError> failures = new CopyOnWriteArrayList<>();
      CountDownLatch done = new CountDownLatch(taskCount);
      for (TaskChannel handle : handles) {
        new Thread(
                () -> {
                  try {
                    for (int i = 0; i < eventsPerTask; i++) {
                      TaskEvent event = handle.recv(RECV_TIMEOUT);
                      assertNotNull(event);
                      assertEquals(handle.taskId(), event.getTaskId());
                    }
                  } catch (AssertionError e) {
                    failures.add(e);
                  } finally {
                    done.countDown();
                  }
                })
            .start();
      }
      for (int round = 0; round < eventsPerTask; round++) {
        for (int i = 0; i < taskCount; i++) {
          serverSocket.sendEvent("task-" + i, "result-generated");
        }
      }
      assertTrue(done.await(15, TimeUnit.SECONDS), "receivers did not finish");
      assertTrue(failures.isEmpty(), "receiver failures: " + failures);
    }
  }

  @Test
  void binaryDownlinkRoutesToSingleActiveTask() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      TaskChannel only = conn.attach("task-tts");
      serverSocket.socket.get().send(ByteString.of(new byte[] {1, 2, 3}));
      TaskEvent event = only.recv(RECV_TIMEOUT);
      assertNotNull(event);
      assertEquals(TaskEvent.Kind.BINARY, event.getKind());
      assertEquals(3, event.getBinary().remaining());
    }
  }

  @Test
  void binaryDownlinkWithTwoTasksNeedsClaim() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      TaskChannel a = conn.attach("task-a");
      conn.attach("task-b");
      conn.claimBinaryDownlink("task-a");
      serverSocket.socket.get().send(ByteString.of(new byte[] {9}));
      TaskEvent event = a.recv(RECV_TIMEOUT);
      assertNotNull(event);
      assertEquals(TaskEvent.Kind.BINARY, event.getKind());
    }
  }

  @Test
  void unclaimedBinaryWithMultipleTasksRaises() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      TaskChannel a = conn.attach("task-a");
      conn.attach("task-b");
      serverSocket.socket.get().send(ByteString.of(new byte[] {9}));
      SdkException e = assertThrows(SdkException.class, () -> drainUntilFailure(a));
      assertEquals("sdk.ws.BinaryOwnerConflict", e.getCode());
    }
  }

  private static void drainUntilFailure(TaskChannel channel) {
    // recv rethrows FAILURE events; loop past any non-failure events first.
    for (int i = 0; i < 4; i++) {
      channel.recv(RECV_TIMEOUT);
    }
  }

  @Test
  void secondBinaryUplinkIsRejected() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      TaskChannel a = conn.attach("task-a");
      TaskChannel b = conn.attach("task-b");
      a.sendBinary(ByteBuffer.wrap(new byte[] {1}));
      SdkException e =
          assertThrows(SdkException.class, () -> b.sendBinary(ByteBuffer.wrap(new byte[] {2})));
      assertEquals("sdk.ws.BinaryOwnerConflict", e.getCode());
    }
  }

  @Test
  void duplicateTaskIdIsRejected() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      conn.attach("task-a");
      SdkException e = assertThrows(SdkException.class, () -> conn.attach("task-a"));
      assertEquals("sdk.ws.IllegalState", e.getCode());
    }
  }

  @Test
  void taskFailedEventCarriesErrorFields() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      TaskChannel handle = conn.attach("task-a");
      serverSocket.sendFailed("task-a", "InvalidParameter", "bad param");
      TaskEvent event = handle.recv(RECV_TIMEOUT);
      assertNotNull(event);
      assertEquals("task-failed", event.getEvent());
      assertEquals("InvalidParameter", event.getErrorCode());
      assertEquals("bad param", event.getErrorMessage());
    }
  }

  @Test
  void crossTaskEventOnDedicatedSessionIsProtocolError() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    WsSession session =
        new WsSession(transport, transport.config().getWsBaseUrl(), "task-mine", "audio", null);
    try {
      serverSocket.awaitOpen();
      serverSocket.sendEvent("task-other", "result-generated");
      SdkException e = assertThrows(SdkException.class, () -> session.recv(RECV_TIMEOUT));
      assertEquals("sdk.ws.ProtocolError", e.getCode());
    } finally {
      session.close();
    }
  }

  @Test
  void handshakeFailureMapsToStatusException() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(403).setBody("forbidden"));
    PermissionDeniedException e =
        assertThrows(
            PermissionDeniedException.class,
            () ->
                new WsSession(
                    transport, transport.config().getWsBaseUrl(), "task-x", "audio", null));
    assertEquals(403, e.getStatusCode());
  }

  @Test
  void handshakeFailureTerminatesReaderThread() throws Exception {
    long before = liveReaderThreads();
    server.enqueue(new MockResponse().setResponseCode(403).setBody("forbidden"));
    assertThrows(
        PermissionDeniedException.class,
        () -> new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null));
    // Regression: a failed constructor never hands out the instance, so close() is unreachable —
    // it must poison + interrupt the demux reader itself instead of leaking it on take().
    long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
    while (liveReaderThreads() > before && System.nanoTime() < deadline) {
      Thread.sleep(20);
    }
    assertEquals(before, liveReaderThreads(), "dashscope-ws-reader leaked after handshake failure");
  }

  private static long liveReaderThreads() {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(t -> t.isAlive() && t.getName().equals("dashscope-ws-reader"))
        .count();
  }

  @Test
  void connectionCloseFailsPendingReceivers() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null);
    serverSocket.awaitOpen();
    TaskChannel handle = conn.attach("task-a");
    conn.close();
    assertThrows(
        RuntimeException.class,
        () -> {
          for (int i = 0; i < 4; i++) {
            handle.recv(RECV_TIMEOUT);
          }
        });
  }

  @Test
  void taskHandleCloseWakesBlockedReceiverImmediately() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      TaskChannel handle = conn.attach("task-close");
      AtomicReference<TaskEvent> received = new AtomicReference<>();
      Thread receiver =
          new Thread(() -> received.set(handle.recv(Duration.ofSeconds(30))), "ws-close-receiver");
      receiver.start();
      Thread.sleep(50);

      handle.close();

      receiver.join(1000);
      assertTrue(!receiver.isAlive(), "close did not wake recv");
      assertNotNull(received.get());
      assertEquals(TaskEvent.Kind.LOCAL_CLOSED, received.get().getKind());
    }
  }

  @Test
  void earlyBinaryOwnerDetachFailsSharedConnection() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null);
    try {
      serverSocket.awaitOpen();
      TaskChannel binary = conn.attach("task-binary");
      TaskChannel other = conn.attach("task-other");
      conn.claimBinaryUplink("task-binary");

      binary.close();

      SdkException failure =
          assertThrows(SdkException.class, () -> other.recv(Duration.ofSeconds(1)));
      assertEquals("sdk.ws.IllegalState", failure.getCode());
      assertThrows(SdkException.class, () -> conn.attach("task-new"));
    } finally {
      conn.close();
    }
  }

  @Test
  void terminalTaskCannotSendAgain() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      serverSocket.awaitOpen();
      TaskChannel handle = conn.attach("task-terminal");
      serverSocket.sendEvent("task-terminal", "task-finished");
      assertEquals("task-finished", handle.recv(RECV_TIMEOUT).getEvent());

      SdkException failure = assertThrows(SdkException.class, () -> handle.sendText("{}"));
      assertEquals("sdk.ws.IllegalState", failure.getCode());
    }
  }

  @Test
  void dedicatedSessionCloseWakesBlockedReceiverImmediately() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    WsSession session =
        new WsSession(transport, transport.config().getWsBaseUrl(), "task-close", "audio", null);
    try {
      serverSocket.awaitOpen();
      AtomicReference<TaskEvent> received = new AtomicReference<>();
      Thread receiver =
          new Thread(() -> received.set(session.recv(Duration.ofSeconds(30))), "ws-session-close");
      receiver.start();
      Thread.sleep(50);

      session.close();

      receiver.join(1000);
      assertTrue(!receiver.isAlive(), "close did not wake dedicated recv");
      assertEquals(TaskEvent.Kind.LOCAL_CLOSED, received.get().getKind());
    } finally {
      session.close();
    }
  }

  @Test
  void requestOptionsExposeBoundedSharedQueue() throws Exception {
    ServerSocket serverSocket = enqueueUpgrade();
    try (WsConnection conn =
        new WsConnection(
            transport,
            transport.config().getWsBaseUrl(),
            "audio",
            RequestOptions.builder().wsQueueCapacity(1).build())) {
      serverSocket.awaitOpen();
      TaskChannel handle = conn.attach("task-bounded");
      for (int i = 0; i < 100; i++) {
        serverSocket.sendEvent("task-bounded", "result-generated");
      }
      Thread.sleep(100);

      SdkException failure = assertThrows(SdkException.class, () -> drainUntilFailure(handle));
      assertEquals("sdk.ws.QueueCapacityExceeded", failure.getCode());
    }
  }
}
