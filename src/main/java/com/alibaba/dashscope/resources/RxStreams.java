// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.resources;

import com.alibaba.dashscope.types.common.ResponseStream;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Bridges pull-based {@link ResponseStream} into RxJava {@link Flowable} (2.x streaming parity).
 *
 * <p>The upstream stream opens on the first subscription and is shared by all live subscribers;
 * once every subscriber has cancelled, the producer stops reading and closes the underlying stream
 * (a cancelled subscriber must not leave the producer draining to EOF). Re-subscribing after full
 * cancellation opens a new upstream. Slow subscribers are served from an unbounded buffer ({@link
 * BackpressureStrategy#BUFFER}, same as the 2.x SSE bridge).
 */
public final class RxStreams {

  private RxStreams() {}

  public static <T> Flowable<T> fromStream(
      Supplier<ResponseStream<T>> streamSupplier, Executor executor) {
    return bridge(streamSupplier, executor);
  }

  /** Same bridge for the job-event streams (fine-tunes/deployments), which are not SSE-framed. */
  public static <T, S extends Iterable<T> & AutoCloseable> Flowable<T> fromIterable(
      Supplier<S> streamSupplier, Executor executor) {
    return bridge(streamSupplier, executor);
  }

  private static <T, S extends Iterable<T> & AutoCloseable> Flowable<T> bridge(
      Supplier<S> streamSupplier, Executor executor) {
    return Flowable.<T>create(
            emitter ->
                executor.execute(
                    () -> {
                      S stream;
                      try {
                        stream = streamSupplier.get();
                      } catch (Throwable e) {
                        emitter.onError(e);
                        return;
                      }
                      // The resource variable must be explicitly typed AutoCloseable:
                      // try-with-resources over the intersection type
                      // S extends Iterable<T> & AutoCloseable emits a call to Iterable.close(),
                      // which does not exist and fails at runtime with NoSuchMethodError.
                      try (AutoCloseable closeGuard = stream) {
                        for (T item : stream) {
                          if (emitter.isCancelled()) {
                            return;
                          }
                          emitter.onNext(item);
                        }
                        emitter.onComplete();
                      } catch (Throwable e) {
                        if (!emitter.isCancelled()) {
                          emitter.onError(e);
                        }
                      }
                    }),
            BackpressureStrategy.BUFFER)
        .publish()
        .refCount();
  }
}
