// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.v3.resources;

import com.alibaba.dashscope.v3.types.common.ResponseStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Bridges pull-based {@link ResponseStream} into {@link Flow.Publisher}.
 *
 * <p>{@link SubmissionPublisher} alone never propagates subscriber cancellation, so a cancelled
 * subscriber would leave the producer thread draining the upstream stream to EOF. This bridge
 * tracks live subscribers and stops reading (closing the underlying stream) once every
 * subscriber has cancelled.
 */
public final class FlowPublishers {

  private FlowPublishers() {}

  public static <T> Flow.Publisher<T> fromStream(
      Supplier<ResponseStream<T>> streamSupplier, Executor executor) {
    return bridge(streamSupplier, executor);
  }

  /** Same bridge for the job-event streams (fine-tunes/deployments), which are not SSE-framed. */
  public static <T, S extends Iterable<T> & AutoCloseable> Flow.Publisher<T> fromIterable(
      Supplier<S> streamSupplier, Executor executor) {
    return bridge(streamSupplier, executor);
  }

  private static <T, S extends Iterable<T> & AutoCloseable> Flow.Publisher<T> bridge(
      Supplier<S> streamSupplier, Executor executor) {
    SubmissionPublisher<T> inner =
        new SubmissionPublisher<>(executor, Flow.defaultBufferSize());
    AtomicInteger liveSubscribers = new AtomicInteger();
    AtomicBoolean everSubscribed = new AtomicBoolean();
    Flow.Publisher<T> publisher =
        subscriber ->
            inner.subscribe(
                new Flow.Subscriber<T>() {
                  {
                    liveSubscribers.incrementAndGet();
                    everSubscribed.set(true);
                  }

                  @Override
                  public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(
                        new Flow.Subscription() {
                          @Override
                          public void request(long n) {
                            subscription.request(n);
                          }

                          @Override
                          public void cancel() {
                            liveSubscribers.decrementAndGet();
                            subscription.cancel();
                          }
                        });
                  }

                  @Override
                  public void onNext(T item) {
                    subscriber.onNext(item);
                  }

                  @Override
                  public void onError(Throwable throwable) {
                    liveSubscribers.decrementAndGet();
                    subscriber.onError(throwable);
                  }

                  @Override
                  public void onComplete() {
                    liveSubscribers.decrementAndGet();
                    subscriber.onComplete();
                  }
                });
    executor.execute(
        () -> {
          S stream;
          try {
            stream = streamSupplier.get();
          } catch (Throwable e) {
            inner.closeExceptionally(e);
            return;
          }
          // The resource variable must be explicitly typed AutoCloseable: with
          // --release 11, try-with-resources over the intersection type
          // S extends Iterable<T> & AutoCloseable emits a call to Iterable.close(),
          // which does not exist and fails at runtime with NoSuchMethodError, leaking
          // the underlying stream.
          try (AutoCloseable closeGuard = stream) {
            for (T item : stream) {
              if (cancelled(everSubscribed, liveSubscribers)) {
                inner.close();
                return;
              }
              // submit() blocks on a full buffer and is NOT released by subscriber
              // cancellation, so offer with a bounded wait instead: the timeout callback
              // keeps retrying while back-pressured and gives up once every subscriber
              // is gone.
              while (inner.offer(
                      item,
                      100,
                      TimeUnit.MILLISECONDS,
                      (sub, ignored) -> !cancelled(everSubscribed, liveSubscribers))
                  < 0) {
                if (cancelled(everSubscribed, liveSubscribers)) {
                  inner.close();
                  return;
                }
              }
            }
            inner.close();
          } catch (Throwable e) {
            inner.closeExceptionally(e);
          }
        });
    return publisher;
  }

  private static boolean cancelled(AtomicBoolean everSubscribed, AtomicInteger liveSubscribers) {
    return everSubscribed.get() && liveSubscribers.get() == 0;
  }
}
