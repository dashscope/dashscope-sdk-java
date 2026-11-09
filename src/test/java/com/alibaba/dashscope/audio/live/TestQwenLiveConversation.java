// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import okio.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TestQwenLiveConversation {
  private MockWebServer server;
  private QwenLiveConnectionParam param;
  private RecordingCallback callback;
  private QwenLiveConversation conversation;
  private ExecutorService executor;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    param =
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .apikey("test-key")
            .url(server.url("/realtime").toString())
            .build();
    callback = new RecordingCallback();
    executor = Executors.newSingleThreadExecutor();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (conversation != null) conversation.cancel();
    executor.shutdownNow();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    server.shutdown();
  }

  @Test
  void unconnectedCloseIsSafe() throws Exception {
    conversation = new QwenLiveConversation(param, callback);
    conversation.close();
    conversation.close();
    conversation.cancel();
    assertFalse(conversation.awaitSessionClosed(0));
    assertThrows(IllegalStateException.class, conversation::connect);
    assertThrows(IllegalStateException.class, conversation::checkStatus);
  }

  @Test
  void waitingBeforeConnectAndNegativeTimeout() throws Exception {
    conversation = new QwenLiveConversation(param, callback);
    assertFalse(conversation.awaitSessionClosed(0));
    assertThrows(IllegalArgumentException.class, () -> conversation.awaitSessionClosed(-1));
  }

  @Test
  void ordinaryWebSocketCloseIsNotSessionConfirmation() throws Exception {
    server.enqueue(
        new MockResponse()
            .withWebSocketUpgrade(
                new WebSocketListener() {
                  @Override
                  public void onOpen(WebSocket socket, Response response) {
                    socket.close(1000, "bye");
                  }
                }));
    conversation = new QwenLiveConversation(param, callback);
    try {
      conversation.connect();
    } catch (IllegalStateException expected) {
    }
    assertTrue(callback.transportClosed.await(5, TimeUnit.SECONDS));
    assertFalse(conversation.awaitSessionClosed(0));
    assertThrows(IllegalStateException.class, conversation::checkStatus);
  }

  @Test
  void rejectedHandshakeIsNotSuccessfulConnect() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(401).setBody("denied"));
    conversation = new QwenLiveConversation(param, callback);
    IllegalStateException failure =
        assertThrows(IllegalStateException.class, conversation::connect);
    assertNotNull(failure.getCause());
    assertTrue(callback.transportClosed.await(5, TimeUnit.SECONDS));
    assertFalse(conversation.awaitSessionClosed(0));
    assertThrows(IllegalStateException.class, conversation::connect);
  }

  @Test
  void handshakeHeadersKeepHostWorkspaceAndHeaderWorkspaceSeparate() throws Exception {
    server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {}));
    QwenLiveConnectionParam withHeaders =
        QwenLiveConnectionParam.builder()
            .model("qwen-live")
            .apikey("test-key")
            .workspaceId("host-space")
            .workspace("header-space")
            .header("X-Test", 123)
            .url(server.url("/realtime").toString())
            .build();
    conversation = new QwenLiveConversation(withHeaders, callback);
    conversation.connect();
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertNotNull(request);
    assertEquals("Bearer test-key", request.getHeader("Authorization"));
    assertEquals("header-space", request.getHeader("X-DashScope-WorkSpace"));
    assertEquals("123", request.getHeader("X-Test"));
    assertEquals("/realtime?model=qwen-live", request.getPath());
  }

  @Test
  void workspaceIdAloneDoesNotBecomeWorkspaceHeader() throws Exception {
    FakeFactory factory = new FakeFactory();
    param.setWorkspaceId("host-space");
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    assertNull(factory.socket.request().header("X-DashScope-WorkSpace"));
    assertEquals("Bearer test-key", factory.socket.request().header("Authorization"));
  }

  @Test
  void synchronousOnOpenBeforeFactoryReturnIsSupported() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    conversation.checkStatus();
    assertEquals(1, callback.openCalls.get());
    assertThrows(IllegalStateException.class, conversation::connect);
  }

  @Test
  void synchronousFailureCancelsReturnedSocket() throws Exception {
    FakeFactory factory = new FakeFactory();
    factory.action =
        (socket, listener) -> listener.onFailure(socket, new IOException("early"), null);
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    assertThrows(IllegalStateException.class, conversation::connect);
    assertTrue(factory.socket.cancelCalls.get() > 0);
    assertFalse(conversation.awaitSessionClosed(0));
  }

  @Test
  void cancellationInsideOnOpenCannotRestoreOpenState() throws Exception {
    FakeFactory factory = new FakeFactory();
    QwenLiveCallback cancels =
        new RecordingCallback() {
          @Override
          public void onOpen() {
            conversation.cancel();
          }
        };
    conversation = new QwenLiveConversation(param, cancels, factory, 1);
    assertThrows(IllegalStateException.class, conversation::connect);
    assertTrue(factory.socket.cancelCalls.get() > 0);
    assertThrows(IllegalStateException.class, conversation::checkStatus);
  }

  @Test
  void cancellationDuringFactoryReturnCancelsLateSocket() throws Exception {
    FakeFactory factory = new FakeFactory();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    factory.action =
        (socket, listener) -> {
          entered.countDown();
          boundedAwait(release);
          listener.onOpen(socket, null);
        };
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    Future<?> result =
        executor.submit(() -> assertThrows(IllegalStateException.class, conversation::connect));
    try {
      assertTrue(entered.await(5, TimeUnit.SECONDS));
      conversation.cancel();
    } finally {
      release.countDown();
    }
    result.get(5, TimeUnit.SECONDS);
    assertTrue(factory.socket.cancelCalls.get() > 0);
    assertEquals(0, callback.openCalls.get());
  }

  @Test
  void concurrentConnectIsRejectedWithoutSecondSocket() throws Exception {
    FakeFactory factory = new FakeFactory();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    factory.action =
        (socket, listener) -> {
          entered.countDown();
          boundedAwait(release);
          listener.onOpen(socket, null);
        };
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    Future<?> first =
        executor.submit(
            () -> {
              try {
                conversation.connect();
              } catch (Exception e) {
                throw new AssertionError(e);
              }
            });
    try {
      assertTrue(entered.await(5, TimeUnit.SECONDS));
      assertThrows(IllegalStateException.class, conversation::connect);
    } finally {
      release.countDown();
    }
    first.get(5, TimeUnit.SECONDS);
    assertEquals(1, factory.calls.get());
  }

  @Test
  void timeoutCancelsSocketAndReleasesProtocolWait() throws Exception {
    FakeFactory factory = new FakeFactory();
    factory.action = (socket, listener) -> {};
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    assertThrows(IllegalStateException.class, conversation::connect);
    assertTrue(factory.socket.cancelCalls.get() > 0);
    assertFalse(conversation.awaitSessionClosed(0));
  }

  @Test
  void interruptDuringConnectRestoresFlagAndCancelsSocket() throws Exception {
    FakeFactory factory = new FakeFactory();
    CountDownLatch entered = new CountDownLatch(1);
    AtomicReference<Thread> worker = new AtomicReference<>();
    factory.action = (socket, listener) -> entered.countDown();
    conversation = new QwenLiveConversation(param, callback, factory, 10);
    Future<Boolean> result =
        executor.submit(
            () -> {
              worker.set(Thread.currentThread());
              try {
                conversation.connect();
                return false;
              } catch (InterruptedException expected) {
                return Thread.currentThread().isInterrupted();
              }
            });
    assertTrue(entered.await(5, TimeUnit.SECONDS));
    worker.get().interrupt();
    assertTrue(result.get(5, TimeUnit.SECONDS));
    assertTrue(factory.socket.cancelCalls.get() > 0);
  }

  @Test
  void closeDuringConnectReleasesWaiters() throws Exception {
    FakeFactory factory = new FakeFactory();
    CountDownLatch entered = new CountDownLatch(1);
    factory.action = (socket, listener) -> entered.countDown();
    conversation = new QwenLiveConversation(param, callback, factory, 10);
    Future<?> result =
        executor.submit(() -> assertThrows(IllegalStateException.class, conversation::connect));
    assertTrue(entered.await(5, TimeUnit.SECONDS));
    conversation.close();
    result.get(5, TimeUnit.SECONDS);
    assertTrue(factory.socket.cancelCalls.get() > 0);
  }

  @Test
  void closeThenCancelIsIdempotentButStillForcesSocketRelease() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    conversation.close();
    conversation.close();
    assertEquals(1, factory.socket.closeCalls.get());
    conversation.cancel();
    conversation.cancel();
    assertEquals(1, factory.socket.cancelCalls.get());
  }

  @ParameterizedTest
  @CsvSource({"false,false", "false,true", "true,false", "true,true"})
  void gracefulCloseKeepsWaitingForDelayedSessionConfirmation(
      boolean alreadyWaiting, boolean repeatClose) throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    Future<Boolean> result = alreadyWaiting ? startSessionClosedWait() : null;

    conversation.close();
    if (repeatClose) conversation.close();
    assertThrows(IllegalStateException.class, () -> conversation.appendAudio("AAA="));
    if (result == null) result = startSessionClosedWait();
    Future<Boolean> waiting = result;
    assertThrows(TimeoutException.class, () -> waiting.get(100, TimeUnit.MILLISECONDS));

    factory.listener.onMessage(
        factory.socket, "{\"type\":\"session.closed\",\"reason\":\"close_requested\"}");
    assertTrue(waiting.get(1, TimeUnit.SECONDS));
    assertEquals("close_requested", conversation.getCloseReason());
    assertTrue(conversation.awaitSessionClosed(0));
  }

  @Test
  void gracefulCloseWaitsUntilTimeoutWithoutConfirmation() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    conversation.close();
    assertFalse(conversation.awaitSessionClosed(0));

    long started = System.nanoTime();
    assertFalse(conversation.awaitSessionClosed(1));
    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    assertTrue(elapsedMillis >= 900, "Protocol wait returned early: " + elapsedMillis + " ms");
  }

  @ParameterizedTest
  @ValueSource(strings = {"cancel", "closing", "closed", "failure"})
  void terminalTransportAfterGracefulCloseReleasesProtocolWait(String terminal) throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    conversation.close();
    Future<Boolean> result = startSessionClosedWait();
    assertThrows(TimeoutException.class, () -> result.get(100, TimeUnit.MILLISECONDS));

    switch (terminal) {
      case "cancel":
        conversation.cancel();
        break;
      case "closing":
        factory.listener.onClosing(factory.socket, 1000, "bye");
        break;
      case "closed":
        factory.listener.onClosed(factory.socket, 1000, "bye");
        break;
      case "failure":
        factory.listener.onFailure(factory.socket, new IOException("connection lost"), null);
        break;
      default:
        fail("Unknown terminal event: " + terminal);
    }
    assertFalse(result.get(1, TimeUnit.SECONDS));
    assertFalse(executor.submit(() -> conversation.awaitSessionClosed(30)).get(1, TimeUnit.SECONDS));
  }

  private Future<Boolean> startSessionClosedWait() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    Future<Boolean> result =
        executor.submit(
            () -> {
              started.countDown();
              return conversation.awaitSessionClosed(30);
            });
    assertTrue(started.await(5, TimeUnit.SECONDS));
    return result;
  }

  @Test
  void rejectedCloseFallsBackToCancel() throws Exception {
    FakeFactory factory = new FakeFactory();
    factory.socket.closeResult = false;
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    conversation.close();
    assertTrue(factory.socket.cancelCalls.get() > 0);
    assertFalse(executor.submit(() -> conversation.awaitSessionClosed(30)).get(1, TimeUnit.SECONDS));
  }

  @Test
  void invalidCloseArgumentsDoNotCloseConversation() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    for (int code : new int[] {999, 1005, 1006, 1015, 2000, 5000}) {
      assertThrows(IllegalArgumentException.class, () -> conversation.close(code, "bad"));
    }
    StringBuilder reason = new StringBuilder();
    for (int i = 0; i < 42; i++) reason.append('中');
    assertThrows(IllegalArgumentException.class, () -> conversation.close(1000, reason.toString()));
    conversation.checkStatus();
    assertEquals(0, factory.socket.closeCalls.get());
  }

  @Test
  void factoryExceptionIsTerminalAndReleasesWaiters() throws Exception {
    conversation =
        new QwenLiveConversation(
            param,
            callback,
            (request, listener) -> {
              throw new IllegalStateException("factory failed");
            },
            1);
    assertThrows(IllegalStateException.class, conversation::connect);
    assertFalse(conversation.awaitSessionClosed(0));
    assertThrows(IllegalStateException.class, conversation::connect);
  }

  @Test
  void interruptDuringProtocolWaitDoesNotCancelConnection() throws Exception {
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    conversation.connect();
    Future<Boolean> result =
        executor.submit(
            () -> {
              Thread.currentThread().interrupt();
              try {
                conversation.awaitSessionClosed(1);
                return false;
              } catch (InterruptedException expected) {
                return Thread.currentThread().isInterrupted();
              }
            });
    assertTrue(result.get(5, TimeUnit.SECONDS));
    conversation.checkStatus();
    assertEquals(0, factory.socket.cancelCalls.get());
  }

  @Test
  void throwingCloseAndErrorCallbacksDoNotBlockStateRelease() throws Exception {
    FakeFactory factory = new FakeFactory();
    QwenLiveCallback throwing =
        new RecordingCallback() {
          @Override
          public void onClose(int code, String reason) {
            throw new IllegalStateException("callback-close");
          }

          @Override
          public void onError(Throwable failure) {
            throw new IllegalStateException("callback-error");
          }
        };
    conversation = new QwenLiveConversation(param, throwing, factory, 1);
    conversation.connect();
    assertDoesNotThrow(() -> factory.listener.onClosed(factory.socket, 1000, "bye"));
    assertDoesNotThrow(
        () -> factory.listener.onFailure(factory.socket, new IOException("failure"), null));
    assertFalse(conversation.awaitSessionClosed(0));
    assertThrows(IllegalStateException.class, conversation::checkStatus);
  }

  @Test
  void defaultErrorCallbackLogsUnhandledFailure() throws Exception {
    QwenLiveCallback minimal =
        new QwenLiveCallback() {
          @Override
          public void onEvent(JsonObject event) {}

          @Override
          public void onClose(int code, String reason) {}
        };
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    PrintStream previous = System.err;
    try (PrintStream captured = new PrintStream(output, true, "UTF-8")) {
      System.setErr(captured);
      minimal.onError(new IOException("unhandled-live-transport-failure"));
    } finally {
      System.setErr(previous);
    }
    assertTrue(output.toString("UTF-8").contains("unhandled-live-transport-failure"));
  }

  @Test
  void asyncCallbacksDoNotBlockTransportAndKeepEventOrder() throws Exception {
    ExecutorService callbackExecutor = Executors.newFixedThreadPool(2);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    BlockingQueue<String> calls = new LinkedBlockingQueue<>();
    try {
      enableAsyncCallbacks(callbackExecutor, 8);
      FakeFactory factory = new FakeFactory();
      callback =
          new RecordingCallback() {
            @Override
            public void onOpen() {
              calls.add("open");
            }

            @Override
            public void onEvent(JsonObject event) {
              String type = event.get("type").getAsString();
              if ("slow.event".equals(type)) {
                entered.countDown();
                boundedAwait(release);
              }
              calls.add(type);
            }

            @Override
            public void onClose(int code, String reason) {
              calls.add("transport.closed");
            }
          };
      conversation = new QwenLiveConversation(param, callback, factory, 1);
      conversation.connect();
      assertEquals("open", calls.poll(1, TimeUnit.SECONDS));
      Future<?> received =
          executor.submit(
              () -> factory.listener.onMessage(factory.socket, "{\"type\":\"slow.event\"}"));
      assertTrue(entered.await(1, TimeUnit.SECONDS));
      assertDoesNotThrow(() -> received.get(1, TimeUnit.SECONDS));

      factory.listener.onMessage(
          factory.socket, "{\"type\":\"session.created\",\"session\":{\"id\":\"async-session\"}}");
      assertEquals("async-session", conversation.getSessionId());
      conversation.close();
      factory.listener.onMessage(
          factory.socket, "{\"type\":\"session.closed\",\"reason\":\"close_requested\"}");
      assertTrue(conversation.awaitSessionClosed(0));
      factory.listener.onClosed(factory.socket, 1000, "bye");
      assertNull(calls.poll(100, TimeUnit.MILLISECONDS));

      release.countDown();
      assertEquals("slow.event", calls.poll(1, TimeUnit.SECONDS));
      assertEquals("session.created", calls.poll(1, TimeUnit.SECONDS));
      assertEquals("session.closed", calls.poll(1, TimeUnit.SECONDS));
      assertEquals("transport.closed", calls.poll(1, TimeUnit.SECONDS));
      assertEquals("still-owned", callbackExecutor.submit(() -> "still-owned").get(1, TimeUnit.SECONDS));
    } finally {
      release.countDown();
      callbackExecutor.shutdownNow();
      assertTrue(callbackExecutor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void asyncCallbackFailureIsReportedBeforeNextEvent() throws Exception {
    ExecutorService callbackExecutor = Executors.newFixedThreadPool(2);
    BlockingQueue<String> calls = new LinkedBlockingQueue<>();
    try {
      enableAsyncCallbacks(callbackExecutor, 8);
      FakeFactory factory = new FakeFactory();
      callback =
          new RecordingCallback() {
            @Override
            public void onEvent(JsonObject event) {
              String type = event.get("type").getAsString();
              if ("bad.event".equals(type)) throw new IllegalStateException("callback-failed");
              calls.add(type);
            }

            @Override
            public void onError(Throwable failure) {
              calls.add(failure.getMessage());
            }
          };
      conversation = new QwenLiveConversation(param, callback, factory, 1);
      conversation.connect();
      factory.listener.onMessage(factory.socket, "{\"type\":\"bad.event\"}");
      factory.listener.onMessage(factory.socket, "{\"type\":\"next.event\"}");
      assertEquals("callback-failed", calls.poll(1, TimeUnit.SECONDS));
      assertEquals("next.event", calls.poll(1, TimeUnit.SECONDS));
      conversation.checkStatus();
    } finally {
      callbackExecutor.shutdownNow();
      assertTrue(callbackExecutor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void assertionErrorsInCallbacksDoNotStrandAsyncQueue(boolean errorHandlerThrows) throws Exception {
    ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    BlockingQueue<String> calls = new LinkedBlockingQueue<>();
    try {
      enableAsyncCallbacks(callbackExecutor, 8);
      FakeFactory factory = new FakeFactory();
      callback =
          new RecordingCallback() {
            @Override
            public void onEvent(JsonObject event) {
              String type = event.get("type").getAsString();
              if ("bad.event".equals(type)) {
                if (errorHandlerThrows) throw new IllegalStateException("callback-failed");
                throw new AssertionError("callback-failed");
              }
              calls.add(type);
            }

            @Override
            public void onError(Throwable failure) {
              calls.add(failure.getMessage());
              if (errorHandlerThrows) throw new AssertionError("error-handler-failed");
            }
          };
      conversation = new QwenLiveConversation(param, callback, factory, 1);
      conversation.connect();
      factory.listener.onMessage(factory.socket, "{\"type\":\"bad.event\"}");
      factory.listener.onMessage(factory.socket, "{\"type\":\"next.event\"}");
      assertEquals("callback-failed", calls.poll(1, TimeUnit.SECONDS));
      assertEquals("next.event", calls.poll(1, TimeUnit.SECONDS));
      conversation.checkStatus();
    } finally {
      callbackExecutor.shutdownNow();
      assertTrue(callbackExecutor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void rejectedCallbackExecutorCancelsWithoutCallingBusinessCodeOnTransportThread() throws Exception {
    enableAsyncCallbacks(task -> { throw new RejectedExecutionException("executor-unavailable"); }, 8);
    FakeFactory factory = new FakeFactory();
    conversation = new QwenLiveConversation(param, callback, factory, 1);
    assertThrows(IllegalStateException.class, conversation::connect);
    assertThrows(IllegalStateException.class, conversation::checkStatus);
    assertFalse(conversation.awaitSessionClosed(0));
    assertEquals(0, callback.openCalls.get());
    assertTrue(callback.errors.isEmpty());
  }

  @Test
  void fullAsyncCallbackQueueCancelsAndReportsErrorOnCallbackThread() throws Exception {
    ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicReference<Thread> callbackThread = new AtomicReference<>();
    AtomicReference<Thread> errorThread = new AtomicReference<>();
    BlockingQueue<String> deliveredEvents = new LinkedBlockingQueue<>();
    try {
      enableAsyncCallbacks(callbackExecutor, 1);
      FakeFactory factory = new FakeFactory();
      callback =
          new RecordingCallback() {
            @Override
            public void onOpen() {
              callbackThread.set(Thread.currentThread());
              entered.countDown();
              boundedAwait(release);
            }

            @Override
            public void onEvent(JsonObject event) {
              deliveredEvents.add(event.get("type").getAsString());
            }

            @Override
            public void onError(Throwable failure) {
              errorThread.set(Thread.currentThread());
              errors.add(failure);
            }
          };
      conversation = new QwenLiveConversation(param, callback, factory, 1);
      conversation.connect();
      assertTrue(entered.await(1, TimeUnit.SECONDS));
      factory.listener.onMessage(factory.socket, "{\"type\":\"queued.event\"}");
      factory.listener.onMessage(factory.socket, "{\"type\":\"overflow.event\"}");
      assertThrows(IllegalStateException.class, conversation::checkStatus);
      assertFalse(conversation.awaitSessionClosed(0));
      release.countDown();
      assertInstanceOf(RejectedExecutionException.class, callback.errors.poll(1, TimeUnit.SECONDS));
      assertEquals(callbackThread.get(), errorThread.get());
      assertTrue(deliveredEvents.isEmpty());
    } finally {
      release.countDown();
      callbackExecutor.shutdownNow();
      assertTrue(callbackExecutor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void queueOverflowReschedulesErrorWhenWorkerExitsDuringCancellation() throws Exception {
    ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    CountDownLatch callbackEntered = new CountDownLatch(1);
    CountDownLatch callbackRelease = new CountDownLatch(1);
    CountDownLatch cancelEntered = new CountDownLatch(1);
    CountDownLatch cancelRelease = new CountDownLatch(1);
    AtomicReference<WebSocketListener> listener = new AtomicReference<>();
    try {
      enableAsyncCallbacks(callbackExecutor, 1);
      FakeSocket socket =
          new FakeSocket() {
            @Override
            public void cancel() {
              super.cancel();
              cancelEntered.countDown();
              boundedAwait(cancelRelease);
            }
          };
      WebSocket.Factory factory =
          (request, connectionListener) -> {
            listener.set(connectionListener);
            connectionListener.onOpen(socket, null);
            return socket;
          };
      callback =
          new RecordingCallback() {
            @Override
            public void onOpen() {
              callbackEntered.countDown();
              boundedAwait(callbackRelease);
            }
          };
      conversation = new QwenLiveConversation(param, callback, factory, 1);
      conversation.connect();
      assertTrue(callbackEntered.await(1, TimeUnit.SECONDS));
      listener.get().onMessage(socket, "{\"type\":\"queued.event\"}");
      Future<?> overflow =
          executor.submit(() -> listener.get().onMessage(socket, "{\"type\":\"overflow.event\"}"));
      assertTrue(cancelEntered.await(1, TimeUnit.SECONDS));
      assertThrows(IllegalStateException.class, conversation::checkStatus);
      callbackRelease.countDown();
      callbackExecutor.submit(() -> {}).get(1, TimeUnit.SECONDS);
      assertTrue(callback.errors.isEmpty());
      cancelRelease.countDown();
      overflow.get(1, TimeUnit.SECONDS);
      assertInstanceOf(RejectedExecutionException.class, callback.errors.poll(1, TimeUnit.SECONDS));
      assertTrue(callback.events.isEmpty());
      assertFalse(conversation.awaitSessionClosed(0));
    } finally {
      callbackRelease.countDown();
      cancelRelease.countDown();
      callbackExecutor.shutdownNow();
      assertTrue(callbackExecutor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void invalidCallbackQueueCapacityIsRejectedBeforeConnecting(int capacity) {
    param.setCallbackQueueCapacity(capacity);
    assertThrows(
        IllegalArgumentException.class,
        () -> new QwenLiveConversation(param, callback, new FakeFactory(), 1));
  }

  private void enableAsyncCallbacks(Executor callbackExecutor, int capacity) {
    param.setCallbackExecutor(callbackExecutor);
    param.setCallbackQueueCapacity(capacity);
  }

  static void boundedAwait(CountDownLatch latch) {
    try {
      assertTrue(latch.await(5, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }
  }

  static class RecordingCallback extends QwenLiveCallback {
    final CountDownLatch transportClosed = new CountDownLatch(1);
    final AtomicInteger openCalls = new AtomicInteger();
    final BlockingQueue<JsonObject> events = new LinkedBlockingQueue<>();
    final BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();

    @Override
    public void onOpen() {
      openCalls.incrementAndGet();
    }

    @Override
    public void onEvent(JsonObject message) {
      events.add(message);
    }

    @Override
    public void onClose(int code, String reason) {
      transportClosed.countDown();
    }

    @Override
    public void onError(Throwable failure) {
      errors.add(failure);
      transportClosed.countDown();
    }
  }

  interface SocketAction {
    void accept(FakeSocket socket, WebSocketListener listener);
  }

  static class FakeFactory implements WebSocket.Factory {
    final FakeSocket socket = new FakeSocket();
    final AtomicInteger calls = new AtomicInteger();
    WebSocketListener listener;
    SocketAction action = (socket, listener) -> listener.onOpen(socket, null);

    @Override
    public WebSocket newWebSocket(Request request, WebSocketListener listener) {
      calls.incrementAndGet();
      this.listener = listener;
      socket.request = request;
      action.accept(socket, listener);
      return socket;
    }
  }

  static class FakeSocket implements WebSocket {
    Request request;
    boolean sendResult = true;
    boolean closeResult = true;
    final AtomicInteger cancelCalls = new AtomicInteger();
    final AtomicInteger closeCalls = new AtomicInteger();
    final BlockingQueue<String> sent = new LinkedBlockingQueue<>();

    @Override
    public Request request() {
      return request;
    }

    @Override
    public long queueSize() {
      return 0;
    }

    @Override
    public boolean send(String text) {
      if (sendResult) sent.add(text);
      return sendResult;
    }

    @Override
    public boolean send(ByteString bytes) {
      return sendResult;
    }

    @Override
    public boolean close(int code, String reason) {
      closeCalls.incrementAndGet();
      return closeResult;
    }

    @Override
    public void cancel() {
      cancelCalls.incrementAndGet();
    }
  }
}
