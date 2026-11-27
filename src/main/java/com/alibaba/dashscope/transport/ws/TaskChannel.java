// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport.ws;

import java.nio.ByteBuffer;
import java.time.Duration;

/**
 * One task's view over a WebSocket (audio design §3): implemented by {@link WsSession} (dedicated
 * socket) and by {@link WsConnection.TaskHandle} (a demultiplexed task on a shared socket). Sends
 * are lock-protected; {@link #recv} blocks on the caller's thread — the SDK starts no per-task
 * callback thread (pull model; users run one send thread + one receive thread).
 */
public interface TaskChannel extends AutoCloseable {
  String taskId();

  /** Send a text frame (already-serialized JSON). Full upstream queue → SdkException. */
  void sendText(String json);

  /** Send a raw binary frame (no header; ownership rules apply on shared connections). */
  void sendBinary(ByteBuffer data);

  /**
   * Next inbound item for this task, blocking up to {@code timeout}; {@code null} on timeout.
   * FAILURE events throw their carried exception.
   */
  TaskEvent recv(Duration timeout);

  /**
   * Idempotent. On a shared connection this detaches the task only; if the task owns a binary lane
   * and has not reached its terminal event, the whole shared connection is torn down instead
   * (binary frames carry no task id, so the lane cannot be handed over safely). A local close is
   * surfaced to a blocked {@link #recv} as a {@code LOCAL_CLOSED} event; a binary-owner teardown is
   * surfaced to every unfinished task, including the closer, as a terminal event instead (a thrown
   * failure — or {@code CLOSED} if the peer closed the connection concurrently).
   */
  @Override
  void close();
}
