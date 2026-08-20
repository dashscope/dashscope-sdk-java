package com.alibaba.dashscope.protocol.okhttp;

import com.alibaba.dashscope.protocol.FullDuplexRequest;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.JsonObject;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

/** @author songsong.shao */
@Slf4j
public class OkHttpWebSocketClientForAudio extends OkHttpWebSocketClient {

  private static final AtomicInteger STREAMING_REQUEST_THREAD_NUM = new AtomicInteger(0);
  private static final AtomicBoolean SHUTDOWN_INITIATED = new AtomicBoolean(false);

  /** Ceiling of concurrently starting sessions, shared by every audio model of the process. */
  private static final int MAX_THREADS =
      intSetting(
          "dashscope.audio.streaming.max.threads", "DASHSCOPE_AUDIO_STREAMING_MAX_THREADS", 100);
  /** Buffer absorbing bursts once every thread is busy, 0 or less disables queueing. */
  private static final int QUEUE_CAPACITY =
      intSetting(
          "dashscope.audio.streaming.queue.capacity",
          "DASHSCOPE_AUDIO_STREAMING_QUEUE_CAPACITY",
          200);

  private static final ThreadPoolExecutor STREAMING_REQUEST_EXECUTOR = createStreamingExecutor();

  private static ThreadPoolExecutor createStreamingExecutor() {
    log.info(
        "Audio streaming request executor: maxThreads={}, queueCapacity={}",
        MAX_THREADS,
        QUEUE_CAPACITY);
    BlockingQueue<Runnable> queue =
        QUEUE_CAPACITY > 0
            ? new LinkedBlockingQueue<Runnable>(QUEUE_CAPACITY)
            : new SynchronousQueue<Runnable>();
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            MAX_THREADS,
            MAX_THREADS,
            60L,
            TimeUnit.SECONDS,
            queue,
            r -> {
              Thread t =
                  new Thread(
                      r,
                      "WS-STREAMING-REQ-Worker-"
                          + STREAMING_REQUEST_THREAD_NUM.updateAndGet(
                              n -> n == Integer.MAX_VALUE ? 0 : n + 1));
              t.setDaemon(true);
              return t;
            },
            OkHttpWebSocketClientForAudio::rejectStreamingRequest);
    // A bounded queue only grows the pool beyond the core size once the queue is full, so the core
    // size carries the ceiling and idle threads are reclaimed through the keep alive instead.
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  /**
   * Replaces the default abort policy, so that a saturated pool reports its own state and the
   * settings to widen it instead of the bare toString() of the executor.
   */
  private static void rejectStreamingRequest(Runnable task, ThreadPoolExecutor executor) {
    throw new RejectedExecutionException(
        StringUtils.format(
            "Audio streaming request executor is saturated: activeThreads=%d, poolSize=%d, "
                + "maxThreads=%d, queued=%d, queueCapacity=%d, completed=%d. Lower the number of "
                + "sessions started concurrently, or raise DASHSCOPE_AUDIO_STREAMING_MAX_THREADS / "
                + "DASHSCOPE_AUDIO_STREAMING_QUEUE_CAPACITY.",
            executor.getActiveCount(),
            executor.getPoolSize(),
            MAX_THREADS,
            executor.getQueue().size(),
            QUEUE_CAPACITY,
            executor.getCompletedTaskCount()));
  }

  public OkHttpWebSocketClientForAudio(OkHttpClient client, boolean passTaskStarted) {
    super(client, passTaskStarted);
    log.info("Use OkHttpWebSocketClientForAudio");
  }

  @Override
  protected void onBeforeSendStartMessage(JsonObject startMessage) {
    log.info("send run-task request {}", JsonUtils.toJson(startMessage));
  }

  @Override
  protected CompletableFuture<Void> sendStreamRequest(FullDuplexRequest req) {
    return CompletableFuture.runAsync(() -> executeStreamRequest(req), STREAMING_REQUEST_EXECUTOR);
  }

  static { // auto close when jvm shutdown
    Runtime.getRuntime()
        .addShutdownHook(new Thread(OkHttpWebSocketClientForAudio::shutdownStreamingExecutor));
  }

  /**
   * Shutdown the streaming request executor gracefully. This method should be called when the
   * application is shutting down to ensure proper resource cleanup.
   */
  private static void shutdownStreamingExecutor() {
    if (!SHUTDOWN_INITIATED.compareAndSet(false, true)) {
      log.debug("Shutdown already in progress");
      return;
    }

    if (!STREAMING_REQUEST_EXECUTOR.isShutdown()) {
      log.debug("Shutting down streaming request executor...");
      STREAMING_REQUEST_EXECUTOR.shutdown();
      try {
        // Wait up to 60 seconds for existing tasks to terminate
        if (!STREAMING_REQUEST_EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
          log.warn(
              "Streaming request executor did not terminate in 60 seconds, forcing shutdown...");
          STREAMING_REQUEST_EXECUTOR.shutdownNow();
          // Wait up to 60 seconds for tasks to respond to being cancelled
          if (!STREAMING_REQUEST_EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
            log.error("Streaming request executor did not terminate");
          }
        }
      } catch (InterruptedException ie) {
        // (Re-)Cancel if current thread also interrupted
        STREAMING_REQUEST_EXECUTOR.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
      log.info("Streaming request executor shut down completed");
    }
  }
}
