package com.alibaba.dashscope.protocol.okhttp;

import com.alibaba.dashscope.protocol.FullDuplexRequest;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import io.reactivex.functions.Action;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okio.ByteString;

import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author songsong.shao
 * @date 2025/11/5
 */
@Slf4j
public class OkHttpWebSocketClientForAudio extends OkHttpWebSocketClient {

    private static final AtomicInteger STREAMING_REQUEST_THREAD_NUM = new AtomicInteger(0);
    private static final AtomicBoolean SHUTDOWN_INITIATED = new AtomicBoolean(false);

    private static final ExecutorService STREAMING_REQUEST_EXECUTOR =
            new ThreadPoolExecutor(1, 100, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
                Thread t = new Thread(r, "WS-STREAMING-REQ-Worker-" + STREAMING_REQUEST_THREAD_NUM.updateAndGet(n -> n == Integer.MAX_VALUE ? 0 : n + 1));
                t.setDaemon(true);
                return t;
            });

    public OkHttpWebSocketClientForAudio(OkHttpClient client, boolean passTaskStarted) {
        super(client, passTaskStarted);
        log.info("Use OkHttpWebSocketClientForAudio");
    }

    @Override
    protected CompletableFuture<Void> sendStreamRequest(FullDuplexRequest req) {
        CompletableFuture<Void> future =
                CompletableFuture.runAsync(
                        () -> {
                            try {
                                isFirstMessage.set(false);

                                JsonObject startMessage = req.getStartTaskMessage();
                                log.info("send run-task request {}", JsonUtils.toJson(startMessage));
                                String taskId =
                                        startMessage.get("header").getAsJsonObject().get("task_id").getAsString();
                                // send start message out.
                                sendTextWithRetry(
                                        req.getApiKey(),
                                        req.isSecurityCheck(),
                                        JsonUtils.toJson(startMessage),
                                        req.getWorkspace(),
                                        req.getHeaders(),
                                        req.getBaseWebSocketUrl());

                                Flowable<Object> streamingData = req.getStreamingData();
                                streamingData.subscribe(
                                        data -> {
                                            try {
                                                if (data instanceof String) {
                                                    JsonObject continueData = req.getContinueMessage((String) data, taskId);
                                                    sendTextWithRetry(
                                                            req.getApiKey(),
                                                            req.isSecurityCheck(),
                                                            JsonUtils.toJson(continueData),
                                                            req.getWorkspace(),
                                                            req.getHeaders(),
                                                            req.getBaseWebSocketUrl());
                                                } else if (data instanceof byte[]) {
                                                    sendBinaryWithRetry(
                                                            req.getApiKey(),
                                                            req.isSecurityCheck(),
                                                            ByteString.of((byte[]) data),
                                                            req.getWorkspace(),
                                                            req.getHeaders(),
                                                            req.getBaseWebSocketUrl());
                                                } else if (data instanceof ByteBuffer) {
                                                    sendBinaryWithRetry(
                                                            req.getApiKey(),
                                                            req.isSecurityCheck(),
                                                            ByteString.of((ByteBuffer) data),
                                                            req.getWorkspace(),
                                                            req.getHeaders(),
                                                            req.getBaseWebSocketUrl());
                                                } else {
                                                    JsonObject continueData = req.getContinueMessage(data, taskId);
                                                    sendTextWithRetry(
                                                            req.getApiKey(),
                                                            req.isSecurityCheck(),
                                                            JsonUtils.toJson(continueData),
                                                            req.getWorkspace(),
                                                            req.getHeaders(),
                                                            req.getBaseWebSocketUrl());
                                                }
                                            } catch (Throwable ex) {
                                                log.error(String.format("sendStreamData exception: %s", ex.getMessage()));
                                                responseEmitter.onError(ex);
                                            }
                                        },
                                        err -> {
                                            log.error(String.format("Get stream data error!"));
                                            responseEmitter.onError(err);
                                        },
                                        new Action() {
                                            @Override
                                            public void run() throws Exception {
                                                log.debug(String.format("Stream data send completed!"));
                                                sendTextWithRetry(
                                                        req.getApiKey(),
                                                        req.isSecurityCheck(),
                                                        JsonUtils.toJson(req.getFinishedTaskMessage(taskId)),
                                                        req.getWorkspace(),
                                                        req.getHeaders(),
                                                        req.getBaseWebSocketUrl());
                                            }
                                        });
                            } catch (Throwable ex) {
                                log.error(String.format("sendStreamData exception: %s", ex.getMessage()));
                                responseEmitter.onError(ex);
                            }
                        });
        return future;
    }

    static {//auto close when jvm shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(OkHttpWebSocketClientForAudio::shutdownStreamingExecutor));
    }
    /**
     * Shutdown the streaming request executor gracefully.
     * This method should be called when the application is shutting down
     * to ensure proper resource cleanup.
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
                    log.warn("Streaming request executor did not terminate in 60 seconds, forcing shutdown...");
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
