// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport.ws;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.errors.SdkException;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.RequestTags;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A shared physical WebSocket multiplexing many tasks by {@code header.task_id} (audio design §7).
 * One daemon reader thread ({@code dashscope-ws-reader}) demuxes inbound frames into per-task
 * queues; user threads block on {@link TaskHandle#recv}. Binary frames carry no task id: the uplink
 * is claimed by the first task to {@code sendBinary}, the downlink by {@link #claimBinaryDownlink}
 * (or implicitly when exactly one task is active). Viable mix: one ASR uplink + one TTS downlink +
 * any number of pure-JSON tasks; two concurrent binary streams on one connection raise {@code
 * sdk.ws.BinaryOwnerConflict}.
 */
public final class WsConnection implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(WsConnection.class);
  private static final RawItem POISON = RawItem.poison();

  private final WebSocket socket;
  private final LinkedBlockingQueue<RawItem> rawQueue = new LinkedBlockingQueue<>();
  private final ConcurrentHashMap<String, TaskHandle> tasks = new ConcurrentHashMap<>();
  private final AtomicReference<String> uplinkOwner = new AtomicReference<>();
  private final AtomicReference<String> downlinkOwner = new AtomicReference<>();
  private final Object sendLock = new Object();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final int maxQueueCapacity;
  private final Thread reader;
  private final CountDownLatch openLatch = new CountDownLatch(1);
  private volatile RuntimeException openFailure;

  public WsConnection(HttpTransport transport, String url, String module, RequestOptions options) {
    this(transport, url, module, options, 0, connectTimeout(options));
  }

  private static Duration connectTimeout(RequestOptions options) {
    return options != null && options.getTimeout() != null
        ? options.getTimeout()
        : WsSession.DEFAULT_CONNECT_TIMEOUT;
  }

  public WsConnection(
      HttpTransport transport,
      String url,
      String module,
      RequestOptions options,
      int maxQueueCapacity,
      Duration connectTimeout) {
    this.maxQueueCapacity = maxQueueCapacity;
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
    // Shared connections must block forever on read (design §5): idle periods are normal and a
    // socket read timeout would kill the single demux reader for ALL tasks. okhttp clears
    // soTimeout and exits the callTimeout watchdog on upgrade today; setting both explicitly
    // keeps the guarantee local. Liveness is the task-level recv timeout on the demux queues,
    // not the socket.
    OkHttpClient.Builder wsClientBuilder =
        transport
            .okhttp()
            .newBuilder()
            .connectTimeout(connectTimeout)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS);
    Duration pingInterval = transport.config().getWsPingInterval();
    if (pingInterval != null && !pingInterval.isZero() && !pingInterval.isNegative()) {
      wsClientBuilder.pingInterval(pingInterval);
    }
    OkHttpClient wsClient = wsClientBuilder.build();
    this.socket = wsClient.newWebSocket(builder.build(), new Listener());
    this.reader = new Thread(this::demuxLoop, "dashscope-ws-reader");
    this.reader.setDaemon(true);
    this.reader.start();
    awaitOpen(connectTimeout);
  }

  /** Register a task on this connection. Duplicate task_id → {@code sdk.ws.IllegalState}. */
  public TaskHandle attach(String taskId) {
    if (closed.get()) {
      throw illegalState("attach on closed connection");
    }
    TaskHandle handle = new TaskHandle(taskId);
    if (tasks.putIfAbsent(taskId, handle) != null) {
      throw illegalState("duplicate task_id " + taskId);
    }
    return handle;
  }

  /** Explicitly claim the binary downlink for a task (TTS start does this automatically). */
  public void claimBinaryDownlink(String taskId) {
    requireAttached(taskId);
    if (!downlinkOwner.compareAndSet(null, taskId) && !taskId.equals(downlinkOwner.get())) {
      throw binaryConflict(
          "downlink already claimed by " + downlinkOwner.get() + ", " + taskId + " cannot claim");
    }
  }

  /** Explicitly claim the binary uplink before the first frame (ASR start does this). */
  public void claimBinaryUplink(String taskId) {
    requireAttached(taskId);
    if (!uplinkOwner.compareAndSet(null, taskId) && !taskId.equals(uplinkOwner.get())) {
      throw binaryConflict(
          "uplink already claimed by " + uplinkOwner.get() + ", " + taskId + " cannot claim");
    }
  }

  private void requireAttached(String taskId) {
    if (!tasks.containsKey(taskId)) {
      throw illegalState("task_id " + taskId + " is not attached");
    }
  }

  private void awaitOpen(Duration connectTimeout) {
    boolean opened;
    try {
      opened = openLatch.await(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      abortReader();
      throw Errors.connection(SdkErrorCode.SDK_CONNECTION_INTERRUPTED, "websocket connect", e);
    }
    if (!opened) {
      abortReader();
      throw Errors.timeout(
          SdkErrorCode.SDK_TIMEOUT, "websocket handshake after " + connectTimeout, null);
    }
    if (openFailure != null) {
      abortReader();
      throw openFailure;
    }
  }

  /**
   * Constructor failure path: the constructor never returns the instance, so {@link #close()} is
   * unreachable; cancel the socket AND terminate the reader (poison + interrupt), otherwise it
   * blocks on {@code rawQueue.take()} forever, leaking the thread and the whole object graph.
   */
  private void abortReader() {
    socket.cancel();
    rawQueue.offer(POISON);
    reader.interrupt();
  }

  private void demuxLoop() {
    while (true) {
      RawItem item;
      try {
        item = rawQueue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      if (item == POISON) {
        return;
      }
      if (item.failure != null) {
        broadcast(TaskEvent.failure(item.failure));
        markAllDetached();
        tasks.clear();
        return;
      }
      if (item.closeCode != null) {
        broadcast(TaskEvent.closed(item.closeCode, item.closeReason));
        markAllDetached();
        tasks.clear();
        return;
      }
      if (item.binary != null) {
        routeBinary(item.binary.asByteBuffer());
        continue;
      }
      routeText(item.text);
    }
  }

  private void routeText(String text) {
    TaskEvent event;
    try {
      event = TaskProtocol.parse(text);
    } catch (RuntimeException e) {
      failConnection(e);
      return;
    }
    TaskHandle handle = tasks.get(event.getTaskId());
    if (handle == null) {
      // An event for a task that already terminated (or was never attached here); dropping is
      // safe because its terminal event was already delivered when it unregistered.
      log.warn(
          "ws event for unknown task {} (event={}), dropped", event.getTaskId(), event.getEvent());
      return;
    }
    handle.enqueue(event);
    if (TaskProtocol.TASK_FINISHED.equals(event.getEvent())
        || TaskProtocol.TASK_FAILED.equals(event.getEvent())) {
      unregisterTerminal(event.getTaskId());
    }
  }

  private void routeBinary(ByteBuffer bytes) {
    String owner = downlinkOwner.get();
    TaskHandle target = owner != null ? tasks.get(owner) : null;
    if (target == null) {
      if (tasks.size() == 1) {
        target = tasks.values().iterator().next();
      } else {
        failConnection(
            binaryConflict("unclaimed binary frame with " + tasks.size() + " active tasks"));
        return;
      }
    }
    target.enqueue(TaskEvent.binary(bytes));
  }

  private void broadcast(TaskEvent event) {
    for (TaskHandle handle : tasks.values()) {
      handle.enqueue(event);
    }
  }

  private void unregisterTerminal(String taskId) {
    TaskHandle handle = tasks.remove(taskId);
    if (handle != null) {
      handle.detached.set(true);
    }
    uplinkOwner.compareAndSet(taskId, null);
    downlinkOwner.compareAndSet(taskId, null);
  }

  private boolean ownsBinaryLane(String taskId) {
    return taskId.equals(uplinkOwner.get()) || taskId.equals(downlinkOwner.get());
  }

  private void detach(TaskHandle handle) {
    String taskId = handle.taskId;
    if (ownsBinaryLane(taskId)) {
      failConnection(
          illegalState(
              "binary task "
                  + taskId
                  + " detached before task-finished; closing shared connection to prevent"
                  + " cross-task audio"));
      return;
    }
    tasks.remove(taskId, handle);
    handle.signalLocalClose();
  }

  private void failConnection(RuntimeException failure) {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    socket.cancel();
    rawQueue.offer(POISON);
    broadcast(TaskEvent.failure(failure));
    markAllDetached();
    tasks.clear();
    reader.interrupt();
  }

  private void markAllDetached() {
    for (TaskHandle handle : tasks.values()) {
      handle.detached.set(true);
    }
  }

  private SdkException binaryConflict(String detail) {
    return Errors.fromSdkErrorCode(
        SdkErrorCode.SDK_WS_BINARY_OWNER_CONFLICT, Collections.singletonMap("detail", detail));
  }

  private static SdkException illegalState(String detail) {
    return Errors.fromSdkErrorCode(
        SdkErrorCode.SDK_WS_ILLEGAL_STATE, Collections.singletonMap("detail", detail));
  }

  private void sendText(String json) {
    if (closed.get()) {
      throw illegalState("send on closed connection");
    }
    synchronized (sendLock) {
      if (!socket.send(json)) {
        throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_WS_UPSTREAM_QUEUE_FULL);
      }
    }
  }

  private void sendBinary(String taskId, ByteBuffer data) {
    if (closed.get()) {
      throw illegalState("send on closed connection");
    }
    if (!uplinkOwner.compareAndSet(null, taskId) && !taskId.equals(uplinkOwner.get())) {
      throw binaryConflict(
          "uplink already claimed by " + uplinkOwner.get() + ", " + taskId + " cannot send");
    }
    synchronized (sendLock) {
      if (!socket.send(ByteString.of(data))) {
        throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_WS_UPSTREAM_QUEUE_FULL);
      }
    }
  }

  /** Broadcasts a connection-closed failure to all unfinished tasks and joins the reader (2s). */
  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    if (!socket.close(1000, "bye")) {
      socket.cancel();
    }
    rawQueue.offer(POISON);
    try {
      reader.join(2000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    broadcast(
        TaskEvent.failure(
            Errors.connection(SdkErrorCode.SDK_CONNECTION_ERROR, "websocket closed", null)));
    markAllDetached();
    tasks.clear();
  }

  /** One task's channel over this shared connection. {@link #close} detaches the task only. */
  public final class TaskHandle implements TaskChannel {
    private final String taskId;
    private final LinkedBlockingQueue<TaskEvent> inbox = new LinkedBlockingQueue<>();
    private final AtomicBoolean detached = new AtomicBoolean(false);
    private final AtomicBoolean overflowed = new AtomicBoolean(false);

    private TaskHandle(String taskId) {
      this.taskId = taskId;
    }

    @Override
    public String taskId() {
      return taskId;
    }

    @Override
    public void sendText(String json) {
      if (detached.get()) {
        throw illegalState("send on detached task " + taskId);
      }
      WsConnection.this.sendText(json);
    }

    @Override
    public void sendBinary(ByteBuffer data) {
      if (detached.get()) {
        throw illegalState("sendBinary on detached task " + taskId);
      }
      WsConnection.this.sendBinary(taskId, data);
    }

    @Override
    public TaskEvent recv(Duration timeout) {
      TaskEvent event;
      try {
        event = inbox.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw Errors.connection(
            SdkErrorCode.SDK_CONNECTION_INTERRUPTED, "waiting for task event", e);
      }
      if (event != null && event.getKind() == TaskEvent.Kind.FAILURE) {
        throw event.getFailure();
      }
      return event;
    }

    @Override
    public void close() {
      if (detached.compareAndSet(false, true)) {
        detach(this);
      }
    }

    private void enqueue(TaskEvent event) {
      if (maxQueueCapacity > 0 && inbox.size() >= maxQueueCapacity) {
        // Never drop frames silently (audio design §4): surface overflow, then the task is
        // torn down because protocol sync is lost.
        if (overflowed.compareAndSet(false, true)) {
          RuntimeException failure =
              Errors.fromSdkErrorCode(SdkErrorCode.SDK_WS_QUEUE_CAPACITY_EXCEEDED);
          inbox.clear();
          inbox.offer(TaskEvent.failure(failure));
          detached.set(true);
          if (ownsBinaryLane(taskId)) {
            failConnection(failure);
          } else {
            tasks.remove(taskId, this);
          }
        }
        return;
      }
      inbox.offer(event);
    }

    private void signalLocalClose() {
      inbox.clear();
      inbox.offer(TaskEvent.localClosed());
    }
  }

  private static final class RawItem {
    final String text;
    final ByteString binary;
    final Integer closeCode;
    final String closeReason;
    final RuntimeException failure;

    private RawItem(
        String text,
        ByteString binary,
        Integer closeCode,
        String closeReason,
        RuntimeException failure) {
      this.text = text;
      this.binary = binary;
      this.closeCode = closeCode;
      this.closeReason = closeReason;
      this.failure = failure;
    }

    static RawItem poison() {
      return new RawItem(null, null, null, null, null);
    }

    static RawItem text(String text) {
      return new RawItem(text, null, null, null, null);
    }

    static RawItem binary(ByteString binary) {
      return new RawItem(null, binary, null, null, null);
    }

    static RawItem closed(int code, String reason) {
      return new RawItem(null, null, code, reason, null);
    }

    static RawItem failure(RuntimeException failure) {
      return new RawItem(null, null, null, null, failure);
    }
  }

  private final class Listener extends WebSocketListener {
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
      openLatch.countDown();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
      if (!closed.get()) {
        rawQueue.offer(RawItem.text(text));
      }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
      if (!closed.get()) {
        rawQueue.offer(RawItem.binary(bytes));
      }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      if (!closed.get()) {
        rawQueue.offer(RawItem.closed(code, reason));
      }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
      if (!closed.get()) {
        rawQueue.offer(RawItem.closed(code, reason));
      }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
      RuntimeException mapped = WsSession.mapFailure(t, response);
      if (openLatch.getCount() > 0) {
        openFailure = mapped;
        openLatch.countDown();
      } else if (!closed.get()) {
        rawQueue.offer(RawItem.failure(mapped));
      }
    }
  }
}
