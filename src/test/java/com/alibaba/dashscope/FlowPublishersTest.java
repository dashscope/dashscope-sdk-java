// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.v3.resources.FlowPublishers;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class FlowPublishersTest {

  private static final class TestStream implements Iterable<String>, AutoCloseable {
    final AtomicBoolean closed = new AtomicBoolean();

    @Override
    public Iterator<String> iterator() {
      return IntStream.range(0, 10000).mapToObj(i -> "item-" + i).iterator();
    }

    @Override
    public void close() {
      closed.set(true);
    }
  }

  @Test
  void subscriberCancellationStopsProducerAndClosesStream() throws Exception {
    TestStream stream = new TestStream();
    ExecutorService executor = Executors.newCachedThreadPool();
    try {
      Flow.Publisher<String> publisher = FlowPublishers.fromIterable(() -> stream, executor);
      CountDownLatch cancelled = new CountDownLatch(1);
      publisher.subscribe(
          new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription s) {
              subscription = s;
              s.request(1);
            }

            @Override
            public void onNext(String item) {
              subscription.cancel();
              cancelled.countDown();
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onComplete() {}
          });

      assertTrue(cancelled.await(5, TimeUnit.SECONDS));
      // The producer must notice the cancellation and close the upstream stream instead of
      // draining all 10000 items to EOF.
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (!stream.closed.get() && System.nanoTime() < deadline) {
        Thread.sleep(10);
      }
      assertTrue(stream.closed.get());
    } finally {
      executor.shutdownNow();
    }
  }
}
