// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.errors.SdkException;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.RequestTags;
import com.alibaba.dashscope.transport.ws.WsSession;
import com.alibaba.dashscope.types.audio.RealtimeParams;
import com.alibaba.dashscope.types.audio.UpdateSessionParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Realtime session (audio design §1.3): OpenAI-Realtime-style protocol — pure JSON text frames
 * distinguished by {@code type}, audio base64-encoded (a protocol limitation; ~33% size overhead,
 * prefer the task protocol for heavy traffic), no {@code task_id}, so every session owns a
 * dedicated socket and cannot share a {@code WsConnection}.
 *
 * <p>Pull model — no SDK threads: OkHttp's reader enqueues events, {@link #events()} streams them
 * to the caller's blocking thread, consuming each frame after delivery. User discipline is one send
 * thread + one receive thread, {@code finally close()}.
 *
 * <p>{@link #close()} is idempotent and the session may re-{@link #connect()} afterwards; a
 * generation counter drops late frames from a previous socket so stale events never leak into the
 * new generation (2.x omni parity). {@link #endSession()} is unsupported on the qwen_audio dialect.
 */
public final class RealtimeSession implements AutoCloseable {
  /** Default wait bound for {@code session.finished} after {@code session.finish} (2.x parity). */
  public static final Duration DEFAULT_END_SESSION_TIMEOUT = Duration.ofSeconds(20);

  private static final String MODULE = "audio";

  private final HttpTransport transport;
  private final String url;
  private final RealtimeParams params;
  private final RequestOptions options;
  private final Duration receiveTimeout;
  private final RealtimeDialect dialect;

  private final Object pumpLock = new Object();
  private final Deque<JsonNode> delivered = new ArrayDeque<>();
  private final LinkedBlockingQueue<Inbound> inbox;
  private final Object sendLock = new Object();
  private final AtomicInteger generation = new AtomicInteger(0);
  private final AtomicBoolean eventsClaimed = new AtomicBoolean(false);

  private volatile WebSocket socket;
  private volatile CountDownLatch openLatch;
  private volatile RuntimeException openFailure;

  /** Set when close() was invoked locally; a locally-closed socket is not a disconnect error. */
  private volatile int localCloseGeneration = -1;

  private volatile boolean terminal;
  private volatile RuntimeException terminalFailure;
  private volatile boolean sawCompletion;
  private volatile boolean sessionFinished;
  private volatile String sessionId;
  private volatile String lastResponseId;
  private volatile long responseCreateNanos = -1;
  private volatile Duration firstAudioDelay;
  private volatile Duration firstTextDelay;

  RealtimeSession(
      HttpTransport transport, String url, RealtimeParams params, RequestOptions options) {
    this.transport = transport;
    this.url = url;
    this.params = params;
    this.options = options == null ? RequestOptions.NONE : options;
    Integer queueCapacity = this.options.getWsQueueCapacity();
    this.inbox =
        queueCapacity != null && queueCapacity > 0
            ? new LinkedBlockingQueue<>(queueCapacity)
            : new LinkedBlockingQueue<>();
    this.receiveTimeout =
        this.options.getTimeout() != null
            ? this.options.getTimeout()
            : transport.config().getTimeout();
    this.dialect = RealtimeDialect.fromModel(params.getModel());
    connect();
  }

  /**
   * (Re)connects the session. Idempotent while a socket is open (2.x omni parity); after {@link
   * #close()} it opens a fresh generation and drops everything buffered from the old one. The
   * handshake wait runs outside the session monitor so {@link #close()} can always take the monitor
   * promptly and cancel a still-connecting socket. A concurrent {@link #connect()} returns as soon
   * as a connect attempt owns the socket, i.e. while its handshake may still be in flight.
   */
  public void connect() {
    CountDownLatch latch;
    WebSocket ws;
    synchronized (this) {
      if (socket != null) {
        return;
      }
      int gen = generation.incrementAndGet();
      // Drop anything the previous generation left behind; the listener also drops late frames.
      inbox.clear();
      synchronized (pumpLock) {
        delivered.clear();
        terminal = false;
        terminalFailure = null;
        sawCompletion = false;
        sessionFinished = false;
        eventsClaimed.set(false);
      }
      sessionId = null;
      lastResponseId = null;
      responseCreateNanos = -1;
      firstAudioDelay = null;
      firstTextDelay = null;

      String apiKey =
          options.getApiKey() != null ? options.getApiKey() : transport.config().getApiKey();
      if (apiKey == null || apiKey.isEmpty()) {
        throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY);
      }
      String workspace =
          options.getWorkspace() != null
              ? options.getWorkspace()
              : transport.config().getWorkspace();
      Request.Builder builder =
          new Request.Builder()
              .url(url)
              .tag(RequestTags.NoRetry.class, RequestTags.NoRetry.INSTANCE)
              .tag(RequestTags.Auth.class, new RequestTags.Auth(apiKey, workspace))
              .tag(RequestTags.Module.class, new RequestTags.Module(MODULE));
      for (Map.Entry<String, String> e : options.getHeaders().entrySet()) {
        builder.header(e.getKey(), e.getValue());
      }
      latch = new CountDownLatch(1);
      openLatch = latch;
      openFailure = null;
      // Read timeout must stay off the socket: server-VAD silence can exceed the inherited HTTP
      // readTimeout; receiving is already task-level (inbox poll in pumpOnce). The inherited
      // callTimeout is cleared for the same reason (okhttp exits its watchdog on upgrade today).
      OkHttpClient.Builder wsClientBuilder =
          transport
              .okhttp()
              .newBuilder()
              .connectTimeout(connectTimeout())
              .readTimeout(0, TimeUnit.MILLISECONDS)
              .callTimeout(0, TimeUnit.MILLISECONDS);
      Duration pingInterval = transport.config().getWsPingInterval();
      if (pingInterval != null && !pingInterval.isZero() && !pingInterval.isNegative()) {
        wsClientBuilder.pingInterval(pingInterval);
      }
      OkHttpClient wsClient = wsClientBuilder.build();
      ws = wsClient.newWebSocket(builder.build(), new Listener(gen));
      socket = ws;
    }

    // Await outside the monitor: close() takes the monitor and cancels an un-latched socket,
    // failing an in-flight handshake promptly instead of blocking close() for the whole connect
    // timeout (2.x c891540 parity: okhttp's close alone does not abort a handshake).
    boolean opened;
    try {
      opened = latch.await(connectTimeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ws.cancel();
      synchronized (this) {
        if (socket == ws) {
          socket = null;
        }
      }
      throw Errors.connection(SdkErrorCode.SDK_CONNECTION_INTERRUPTED, "websocket connect", e);
    }
    synchronized (this) {
      if (openFailure != null) {
        // socket may already be null (close() or the listener detached this socket); only undo
        // state that still belongs to this connect attempt.
        if (socket == ws) {
          socket = null;
          ws.cancel();
        }
        throw openFailure;
      }
      if (!opened) {
        if (socket == ws) {
          socket = null;
          ws.cancel();
        }
        throw Errors.timeout(
            SdkErrorCode.SDK_TIMEOUT, "websocket handshake after " + connectTimeout(), null);
      }
      if (socket != ws) {
        // The socket was detached while its handshake completed (a local close, or the peer
        // closing right after the upgrade); the session state already reflects it, so surface
        // the failure here instead of deferring it to the first events() iteration.
        throw Errors.connection(
            SdkErrorCode.SDK_CONNECTION_ERROR, "websocket closed during connect", null);
      }
    }
  }

  /** session.update with the given configuration (only set fields are serialized). */
  public void updateSession(UpdateSessionParams update) {
    if (update == null) {
      throw new IllegalArgumentException("update must not be null");
    }
    ObjectNode frame = newEventFrame("session.update");
    frame.set("session", update.toSessionJson());
    sendFrame(frame);
  }

  /** Appends PCM audio; base64-encoded on the wire (protocol limitation). */
  public void appendAudio(byte[] audio) {
    appendAudioBase64(Base64.getEncoder().encodeToString(audio));
  }

  /** Appends audio already base64-encoded. */
  public void appendAudioBase64(String audioBase64) {
    ObjectNode frame = newEventFrame("input_audio_buffer.append");
    frame.put("audio", audioBase64);
    sendFrame(frame);
  }

  /** Appends one image frame of the video input; base64-encoded on the wire. */
  public void appendImage(byte[] image) {
    ObjectNode frame = newEventFrame("input_image_buffer.append");
    frame.put("image", Base64.getEncoder().encodeToString(image));
    sendFrame(frame);
  }

  /** Commits the audio buffered so far (not needed in server-VAD mode). */
  public void commit() {
    sendFrame(newEventFrame("input_audio_buffer.commit"));
  }

  /** Clears the audio buffered so far. */
  public void clearAppendedAudio() {
    sendFrame(newEventFrame("input_audio_buffer.clear"));
  }

  /** Creates a conversation item. */
  public void createItem(JsonNode item) {
    ObjectNode frame = newEventFrame("conversation.item.create");
    frame.set("item", item);
    sendFrame(frame);
  }

  /** response.create with no overrides. */
  public void createResponse() {
    sendFrame(newEventFrame("response.create"));
  }

  /**
   * response.create with overrides; {@code instructions} is always carried (JSON null when not
   * given, 2.x parity), {@code modalities} only when non-null.
   */
  public void createResponse(String instructions, List<String> modalities) {
    ObjectNode frame = newEventFrame("response.create");
    ObjectNode response = frame.putObject("response");
    if (instructions != null) {
      response.put("instructions", instructions);
    } else {
      response.putNull("instructions");
    }
    if (modalities != null) {
      response.set("modalities", JsonSupport.mapper().valueToTree(modalities));
    }
    sendFrame(frame);
  }

  /** Cancels the in-flight response. */
  public void cancelResponse() {
    sendFrame(newEventFrame("response.cancel"));
  }

  /** Sends session.finish without waiting; see {@link #endSession(Duration)}. */
  public void endSessionAsync() {
    if (dialect == RealtimeDialect.QWEN_AUDIO) {
      throw new UnsupportedOperationException(
          "endSession is not supported by the qwen_audio dialect (model "
              + params.getModel()
              + ")");
    }
    sendFrame(newEventFrame("session.finish"));
  }

  /** session.finish, then waits up to {@link #DEFAULT_END_SESSION_TIMEOUT} for session.finished. */
  public void endSession() {
    endSession(DEFAULT_END_SESSION_TIMEOUT);
  }

  /**
   * session.finish, then blocks until session.finished or {@code timeout}; events pumped while
   * waiting stay buffered for {@link #events()}. Does not close the socket — call {@link #close()}.
   */
  public void endSession(Duration timeout) {
    endSessionAsync();
    long deadline = System.nanoTime() + timeout.toNanos();
    while (true) {
      synchronized (pumpLock) {
        if (terminalFailure != null) {
          throw terminalFailure;
        }
        if (sawSessionFinished()) {
          return;
        }
        if (terminal) {
          throw Errors.connection(
              SdkErrorCode.SDK_CONNECTION_ERROR, "websocket closed before session.finished", null);
        }
      }
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) {
        throw Errors.timeout(SdkErrorCode.SDK_TIMEOUT, "waiting for session.finished", null);
      }
      pumpOnce(Duration.ofNanos(Math.min(remaining, receiveTimeout.toNanos())));
    }
  }

  /**
   * Raw event frames, delivered once and released after delivery (a consuming, single-use stream: a
   * second {@code iterator()} throws {@link SdkException}; the claim resets on {@link #connect()});
   * ends when the socket closes (after a completion was seen or a local close) — an early
   * disconnect raises {@link ApiConnectionException} mid-iteration, a transport failure raises its
   * mapped exception. Server {@code error}-type frames are delivered as ordinary events.
   */
  public Iterable<JsonNode> events() {
    return () -> {
      if (!eventsClaimed.compareAndSet(false, true)) {
        throw illegalState("the realtime event stream is single-use");
      }
      return new Iterator<JsonNode>() {

        @Override
        public boolean hasNext() {
          while (true) {
            synchronized (pumpLock) {
              if (!delivered.isEmpty()) {
                return true;
              }
              if (terminalFailure != null) {
                throw terminalFailure;
              }
              if (terminal) {
                return false;
              }
            }
            pumpOnce(receiveTimeout);
          }
        }

        @Override
        public JsonNode next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          synchronized (pumpLock) {
            return delivered.removeFirst();
          }
        }
      };
    };
  }

  /** Low-level escape hatch: sends the frame verbatim, auto-adding {@code event_id} if absent. */
  public void sendEvent(ObjectNode frame) {
    if (!frame.has("event_id")) {
      frame.put("event_id", newEventId());
    }
    sendFrame(frame);
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getLastResponseId() {
    return lastResponseId;
  }

  /** response.created to first response.audio.delta; null until both happened. */
  public Duration getFirstAudioDelay() {
    return firstAudioDelay;
  }

  /** response.created to first response.audio_transcript.delta; null until both happened. */
  public Duration getFirstTextDelay() {
    return firstTextDelay;
  }

  RealtimeDialect getDialect() {
    return dialect;
  }

  /**
   * Idempotent; the session may be re-{@link #connect()}ed afterwards. The handshake wait in {@link
   * #connect()} runs outside this monitor, so this call always takes it promptly; a socket whose
   * handshake is still in flight is cancelled, failing that connect fast instead of blocking here
   * for the whole connect timeout (2.x c891540 parity: okhttp's close alone does not abort an
   * in-flight handshake).
   */
  @Override
  public void close() {
    synchronized (this) {
      WebSocket ws = socket;
      socket = null;
      if (ws != null) {
        localCloseGeneration = generation.get();
        // Wake a receiver immediately; do not depend on a later OkHttp onClosed callback.
        inbox.clear();
        inbox.offer(Inbound.closed(localCloseGeneration));
        CountDownLatch latch = openLatch;
        if (latch != null && latch.getCount() > 0) {
          // Handshake still in flight: only cancel aborts it promptly.
          ws.cancel();
        } else if (!ws.close(1000, "bye")) {
          ws.cancel();
        }
      }
    }
  }

  private ObjectNode newEventFrame(String type) {
    ObjectNode frame = JsonSupport.mapper().createObjectNode();
    frame.put("event_id", newEventId());
    frame.put("type", type);
    return frame;
  }

  private static String newEventId() {
    return "event_" + UUID.randomUUID().toString().replace("-", "");
  }

  private void sendFrame(ObjectNode frame) {
    WebSocket ws = socket;
    if (ws == null) {
      throw illegalState("session not connected");
    }
    synchronized (sendLock) {
      if (!ws.send(frame.toString())) {
        if (socket != ws) {
          throw Errors.connection(
              SdkErrorCode.SDK_CONNECTION_ERROR, "websocket is no longer connected", null);
        }
        throw Errors.fromSdkErrorCode(SdkErrorCode.SDK_WS_UPSTREAM_QUEUE_FULL);
      }
    }
  }

  private Duration connectTimeout() {
    return options.getTimeout() != null ? options.getTimeout() : WsSession.DEFAULT_CONNECT_TIMEOUT;
  }

  private void offerInbound(Inbound inbound) {
    if (inbox.offer(inbound)) {
      return;
    }
    RuntimeException failure = Errors.fromSdkErrorCode(SdkErrorCode.SDK_WS_QUEUE_CAPACITY_EXCEEDED);
    inbox.clear();
    inbox.offer(Inbound.failure(inbound.generation, failure));
    WebSocket ws = socket;
    if (ws != null) {
      ws.cancel();
    }
  }

  private boolean sawSessionFinished() {
    return sessionFinished;
  }

  /** Pumps one inbound item into the session buffer; terminal states flip and throw. */
  private void pumpOnce(Duration timeout) {
    long deadline = System.nanoTime() + timeout.toNanos();
    Inbound in;
    while (true) {
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) {
        return;
      }
      try {
        in = inbox.poll(remaining, TimeUnit.NANOSECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw Errors.connection(
            SdkErrorCode.SDK_CONNECTION_INTERRUPTED, "waiting for realtime event", e);
      }
      if (in == null) {
        return;
      }
      // A frame enqueued just before a reconnect may carry a stale generation; skip it.
      if (in.generation == generation.get()) {
        break;
      }
    }
    synchronized (pumpLock) {
      if (in.failure != null) {
        terminal = true;
        terminalFailure = in.failure;
        throw terminalFailure;
      }
      if (in.closed) {
        terminal = true;
        if (!sawCompletion && localCloseGeneration != in.generation) {
          terminalFailure =
              Errors.connection(
                  SdkErrorCode.SDK_CONNECTION_ERROR,
                  "websocket closed before session.finished/response.done",
                  null);
          throw terminalFailure;
        }
        return;
      }
      record(in.event);
      delivered.add(in.event);
    }
  }

  private void record(JsonNode event) {
    String type = event.path("type").asText();
    switch (type) {
      case "session.created":
        sessionId = event.path("session").path("id").asText(null);
        break;
      case "response.created":
        lastResponseId = event.path("response").path("id").asText(null);
        responseCreateNanos = System.nanoTime();
        firstAudioDelay = null;
        firstTextDelay = null;
        break;
      case "response.audio.delta":
        if (responseCreateNanos >= 0 && firstAudioDelay == null) {
          firstAudioDelay = Duration.ofNanos(System.nanoTime() - responseCreateNanos);
        }
        break;
      case "response.audio_transcript.delta":
        if (responseCreateNanos >= 0 && firstTextDelay == null) {
          firstTextDelay = Duration.ofNanos(System.nanoTime() - responseCreateNanos);
        }
        break;
      case "response.done":
        sawCompletion = true;
        break;
      case "session.finished":
        sawCompletion = true;
        sessionFinished = true;
        break;
      default:
        break;
    }
  }

  private static SdkException illegalState(String detail) {
    return Errors.fromSdkErrorCode(
        SdkErrorCode.SDK_WS_ILLEGAL_STATE, Collections.singletonMap("detail", detail));
  }

  /** One inbound item: either a text event, a transport failure, or a close notification. */
  private static final class Inbound {
    final int generation;
    final JsonNode event;
    final RuntimeException failure;
    final boolean closed;

    private Inbound(int generation, JsonNode event, RuntimeException failure, boolean closed) {
      this.generation = generation;
      this.event = event;
      this.failure = failure;
      this.closed = closed;
    }

    static Inbound text(int generation, JsonNode event) {
      return new Inbound(generation, event, null, false);
    }

    static Inbound failure(int generation, RuntimeException failure) {
      return new Inbound(generation, null, failure, false);
    }

    static Inbound closed(int generation) {
      return new Inbound(generation, null, null, true);
    }
  }

  private final class Listener extends WebSocketListener {
    /** Frames arriving with a stale generation are dropped (anti-cross-generation). */
    private final int gen;

    Listener(int gen) {
      this.gen = gen;
    }

    private boolean stale() {
      return gen != generation.get();
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
      CountDownLatch latch = openLatch;
      if (!stale() && latch != null) {
        latch.countDown();
      }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
      if (stale()) {
        return;
      }
      JsonNode event;
      try {
        event = JsonSupport.readTree(text);
      } catch (RuntimeException e) {
        offerInbound(Inbound.failure(gen, e));
        return;
      }
      offerInbound(Inbound.text(gen, event));
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      if (!stale()) {
        clearCurrentSocket(webSocket);
        offerInbound(Inbound.closed(gen));
      }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
      if (!stale()) {
        clearCurrentSocket(webSocket);
        offerInbound(Inbound.closed(gen));
      }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
      if (stale()) {
        if (response != null) {
          response.close();
        }
        return;
      }
      RuntimeException mapped = WsSession.mapFailure(t, response);
      CountDownLatch latch = openLatch;
      if (latch != null && latch.getCount() > 0) {
        openFailure = mapped;
        // Count down before clearCurrentSocket so connect() wakes promptly: the handshake await
        // runs outside the session monitor, and the post-await cleanup re-acquires the monitor
        // guarded by the socket identity, so it tolerates this callback detaching the socket.
        latch.countDown();
        clearCurrentSocket(webSocket);
      } else {
        clearCurrentSocket(webSocket);
        offerInbound(Inbound.failure(gen, mapped));
      }
    }

    private void clearCurrentSocket(WebSocket webSocket) {
      synchronized (RealtimeSession.this) {
        if (socket == webSocket) {
          socket = null;
        }
      }
    }
  }
}
