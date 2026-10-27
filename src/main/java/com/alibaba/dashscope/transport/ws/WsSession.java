// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport.ws;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.RequestTags;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * A single task over a dedicated WebSocket (audio design §3). No SDK-spawned threads: OkHttp's
 * socket reader hands events into a queue and {@link #recv} blocks the caller's thread on it.
 * Events whose {@code task_id} differs from this session's task are protocol errors.
 */
public final class WsSession implements TaskChannel {
  /** Handshake (ws upgrade) timeout; audio design §5. */
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(60);

  private final String taskId;
  private final WebSocket socket;
  private final LinkedBlockingQueue<TaskEvent> inbox;
  private final int maxQueueCapacity;
  private final Object sendLock = new Object();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean overflowed = new AtomicBoolean(false);
  private final CountDownLatch openLatch = new CountDownLatch(1);
  private volatile RuntimeException openFailure;

  public WsSession(
      HttpTransport transport, String url, String taskId, String module, RequestOptions options) {
    this(transport, url, taskId, module, options, 0, connectTimeout(options));
  }

  private static Duration connectTimeout(RequestOptions options) {
    return options != null && options.getTimeout() != null
        ? options.getTimeout()
        : DEFAULT_CONNECT_TIMEOUT;
  }

  /**
   * @param maxQueueCapacity 0 = unbounded downlink queue; overflow raises QueueCapacityExceeded.
   */
  public WsSession(
      HttpTransport transport,
      String url,
      String taskId,
      String module,
      RequestOptions options,
      int maxQueueCapacity,
      Duration connectTimeout) {
    this.taskId = taskId;
    this.maxQueueCapacity = maxQueueCapacity;
    this.inbox =
        maxQueueCapacity > 0
            ? new LinkedBlockingQueue<>(maxQueueCapacity)
            : new LinkedBlockingQueue<>();
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    String apiKey = opts.getApiKey() != null ? opts.getApiKey() : transport.config().getApiKey();
    if (apiKey == null || apiKey.isEmpty()) {
      throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY);
    }
    String workspace =
        opts.getWorkspace() != null ? opts.getWorkspace() : transport.config().getWorkspace();
    Request.Builder builder =
        new Request.Builder()
            .url(url)
            .tag(RequestTags.NoRetry.class, RequestTags.NoRetry.INSTANCE)
            .tag(RequestTags.Auth.class, new RequestTags.Auth(apiKey, workspace));
    if (module != null && !module.isEmpty()) {
      builder.tag(RequestTags.Module.class, new RequestTags.Module(module));
    }
    for (Map.Entry<String, String> e : opts.getHeaders().entrySet()) {
      builder.header(e.getKey(), e.getValue());
    }
    OkHttpClient.Builder wsClientBuilder =
        transport.okhttp().newBuilder().connectTimeout(connectTimeout);
    Duration pingInterval = transport.config().getWsPingInterval();
    if (pingInterval != null && !pingInterval.isZero() && !pingInterval.isNegative()) {
      wsClientBuilder.pingInterval(pingInterval);
    }
    OkHttpClient wsClient = wsClientBuilder.build();
    this.socket = wsClient.newWebSocket(builder.build(), new Listener());
    awaitOpen(connectTimeout);
  }

  private void awaitOpen(Duration connectTimeout) {
    boolean opened;
    try {
      opened = openLatch.await(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      socket.cancel();
      throw Errors.connection(SdkErrorCode.SDK_CONNECTION_INTERRUPTED, "websocket connect", e);
    }
    if (!opened) {
      socket.cancel();
      throw Errors.timeout(
          SdkErrorCode.SDK_TIMEOUT, "websocket handshake after " + connectTimeout, null);
    }
    if (openFailure != null) {
      socket.cancel();
      throw openFailure;
    }
  }

  private void enqueue(TaskEvent event) {
    if (closed.get()) {
      return;
    }
    if (maxQueueCapacity > 0 && !inbox.offer(event)) {
      // Never drop frames silently: surface the overflow to the consumer, then the connection
      // is torn down because protocol sync is lost (audio design §4).
      if (overflowed.compareAndSet(false, true)) {
        inbox.clear();
        inbox.offer(
            TaskEvent.failure(
                Errors.fromSdkErrorCode(SdkErrorCode.SDK_WS_QUEUE_CAPACITY_EXCEEDED)));
        socket.cancel();
      }
      return;
    }
    if (maxQueueCapacity == 0) {
      inbox.offer(event);
    }
  }

  @Override
  public String taskId() {
    return taskId;
  }

  @Override
  public void sendText(String json) {
    ensureOpen();
    synchronized (sendLock) {
      if (!socket.send(json)) {
        throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_WS_UPSTREAM_QUEUE_FULL);
      }
    }
  }

  @Override
  public void sendBinary(ByteBuffer data) {
    ensureOpen();
    synchronized (sendLock) {
      if (!socket.send(ByteString.of(data))) {
        throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_WS_UPSTREAM_QUEUE_FULL);
      }
    }
  }

  @Override
  public TaskEvent recv(Duration timeout) {
    TaskEvent event;
    try {
      event = inbox.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw Errors.connection(SdkErrorCode.SDK_CONNECTION_INTERRUPTED, "waiting for task event", e);
    }
    if (event != null && event.getKind() == TaskEvent.Kind.FAILURE) {
      throw event.getFailure();
    }
    return event;
  }

  private void ensureOpen() {
    if (closed.get()) {
      throw Errors.fromSdkErrorCode(
          SdkErrorCode.SDK_WS_ILLEGAL_STATE,
          java.util.Collections.singletonMap("detail", "send on closed session"));
    }
  }

  /** Idempotent: graceful 1000 close. Safe to call from any thread, any number of times. */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      inbox.clear();
      inbox.offer(TaskEvent.localClosed());
      if (!socket.close(1000, "bye")) {
        socket.cancel();
      }
    }
  }

  private final class Listener extends WebSocketListener {
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
      openLatch.countDown();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
      if (closed.get()) {
        return;
      }
      TaskEvent event;
      try {
        event = TaskProtocol.parse(text);
      } catch (RuntimeException e) {
        enqueue(TaskEvent.failure(e));
        return;
      }
      if (event.getTaskId() != null && !taskId.equals(event.getTaskId())) {
        enqueue(
            TaskEvent.failure(
                TaskProtocol.protocolError(
                    "event for task " + event.getTaskId() + " on session " + taskId)));
        return;
      }
      enqueue(event);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
      if (!closed.get()) {
        enqueue(TaskEvent.binary(bytes.asByteBuffer()));
      }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      if (!closed.get()) {
        enqueue(TaskEvent.closed(code, reason));
      }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
      if (!closed.get()) {
        enqueue(TaskEvent.closed(code, reason));
      }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
      RuntimeException mapped = mapFailure(t, response);
      if (!openLatchCountReached()) {
        openFailure = mapped;
        openLatch.countDown();
      } else if (!closed.get()) {
        enqueue(TaskEvent.failure(mapped));
      }
    }

    private boolean openLatchCountReached() {
      return openLatch.getCount() == 0;
    }
  }

  /** Maps a websocket failure to the exception family; reused by the realtime session. */
  public static RuntimeException mapFailure(Throwable t, Response response) {
    if (response != null) {
      return Errors.fromStatus(
          response.code(),
          null,
          "websocket handshake failed: HTTP " + response.code(),
          null,
          null,
          null);
    }
    return Errors.connection(
        SdkErrorCode.SDK_CONNECTION_ERROR, "websocket failure: " + t.getMessage(), t);
  }
}
