// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.resources;

import com.alibaba.dashscope.types.common.ResponseStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;

/** Bridges pull-based {@link ResponseStream} into {@link Flow.Publisher}. */
public final class FlowPublishers {

  private FlowPublishers() {}

  public static <T> Flow.Publisher<T> fromStream(
      Supplier<ResponseStream<T>> streamSupplier, Executor executor) {
    SubmissionPublisher<T> publisher = new SubmissionPublisher<>(executor, Flow.defaultBufferSize());
    executor.execute(
        () -> {
          try (ResponseStream<T> stream = streamSupplier.get()) {
            for (T item : stream) {
              publisher.submit(item);
            }
            publisher.close();
          } catch (Throwable e) {
            publisher.closeExceptionally(e);
          }
        });
    return publisher;
  }

  /** Same bridge for the job-event streams (fine-tunes/deployments), which are not SSE-framed. */
  public static <T, S extends Iterable<T> & AutoCloseable> Flow.Publisher<T> fromIterable(
      Supplier<S> streamSupplier, Executor executor) {
    SubmissionPublisher<T> publisher = new SubmissionPublisher<>(executor, Flow.defaultBufferSize());
    executor.execute(
        () -> {
          try (S stream = streamSupplier.get()) {
            for (T item : stream) {
              publisher.submit(item);
            }
            publisher.close();
          } catch (Throwable e) {
            publisher.closeExceptionally(e);
          }
        });
    return publisher;
  }
}
