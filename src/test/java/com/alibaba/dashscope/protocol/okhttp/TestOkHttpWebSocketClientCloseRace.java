// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol.okhttp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.FullDuplexTestParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.FullDuplexRequest;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.plugins.RxJavaPlugins;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Regression coverage for the post-close send race: once the client is cancelled/disposed, the
 * streamingData onComplete finish-task send (and any reconnect it triggers) must be quiet and must
 * never leak an exception to the global RxJava handler as an UndeliverableException.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class TestOkHttpWebSocketClientCloseRace {

  private OkHttpWebSocketClient clientThatFailsFinishTask(AtomicInteger finishSends) {
    return new OkHttpWebSocketClient(new OkHttpClient(), false) {
      @Override
      protected void sendTextWithRetry(
          String apiKey,
          boolean isSecurityCheck,
          String message,
          String workspace,
          Map<String, String> customHeaders,
          String baseWebSocketUrl,
          String module) {
        if (message.contains("finish-task")) {
          finishSends.incrementAndGet();
          throw new RuntimeException(new NoApiKeyException());
        }
      }
    };
  }

  private FullDuplexRequest request(Flowable<Object> streamData) {
    FullDuplexTestParam param =
        FullDuplexTestParam.builder().model("m").streamData(streamData).build();
    ApiServiceOption option =
        ApiServiceOption.builder()
            .protocol(Protocol.WEBSOCKET)
            .streamingMode(StreamingMode.DUPLEX)
            .taskGroup("g")
            .task("t")
            .function("f")
            .build();
    return new FullDuplexRequest(param, option);
  }

  @Test
  public void finishTaskSkippedAfterCancel() {
    List<Throwable> undeliverable = new CopyOnWriteArrayList<>();
    AtomicInteger finishSends = new AtomicInteger();
    OkHttpWebSocketClient client = clientThatFailsFinishTask(finishSends);
    // The source runs synchronously inside subscribe: cancel() sets isClosed before onComplete
    // drives the finish-task Action, reproducing the dispose-then-complete race deterministically.
    Flowable<Object> src =
        Flowable.create(
            emitter -> {
              client.cancel();
              emitter.onComplete();
            },
            BackpressureStrategy.BUFFER);
    RxJavaPlugins.setErrorHandler(undeliverable::add);
    try {
      client.executeStreamRequest(request(src));
      assertEquals(0, finishSends.get(), "finish-task must not be sent after cancel");
      assertTrue(
          undeliverable.isEmpty(), "expected no undeliverable errors, got: " + undeliverable);
    } finally {
      RxJavaPlugins.setErrorHandler(null);
    }
  }

  @Test
  public void finishTaskReconnectFailureDoesNotLeakWhenCancelledInFlight() {
    List<Throwable> undeliverable = new CopyOnWriteArrayList<>();
    AtomicInteger reconnectAttempts = new AtomicInteger();
    OkHttpWebSocketClient client =
        new OkHttpWebSocketClient(new OkHttpClient(), false) {
          @Override
          protected void sendTextWithRetry(
              String apiKey,
              boolean isSecurityCheck,
              String message,
              String workspace,
              Map<String, String> customHeaders,
              String baseWebSocketUrl,
              String module) {
            if (!message.contains("finish-task")) {
              return;
            }
            Map<String, String> headers =
                new HashMap<String, String>() {
                  @Override
                  public String get(Object key) {
                    // Request construction runs after the connection emitter is installed.
                    // Cancel here to complete the emitter, then simulate a late auth failure.
                    reconnectAttempts.incrementAndGet();
                    cancel();
                    throw new RuntimeException(new NoApiKeyException());
                  }
                };
            super.sendTextWithRetry(
                apiKey, isSecurityCheck, message, workspace, headers, baseWebSocketUrl, module);
          }
        };
    RxJavaPlugins.setErrorHandler(undeliverable::add);
    try {
      client.executeStreamRequest(request(Flowable.empty()));
      assertEquals(1, reconnectAttempts.get(), "finish-task must enter real request construction");
      assertTrue(
          undeliverable.isEmpty(), "expected no undeliverable errors, got: " + undeliverable);
    } finally {
      RxJavaPlugins.setErrorHandler(null);
    }
  }

  @Test
  public void finishTaskFailureSurfacesWhenOpen() {
    List<Throwable> undeliverable = new CopyOnWriteArrayList<>();
    AtomicInteger finishSends = new AtomicInteger();
    OkHttpWebSocketClient client = clientThatFailsFinishTask(finishSends);
    RxJavaPlugins.setErrorHandler(undeliverable::add);
    try {
      client.executeStreamRequest(request(Flowable.empty()));
      assertEquals(1, finishSends.get(), "finish-task should be attempted on normal completion");
      assertTrue(
          undeliverable.isEmpty(),
          "a live-flow finish-task failure must be caught, not leaked, got: " + undeliverable);
    } finally {
      RxJavaPlugins.setErrorHandler(null);
    }
  }
}
