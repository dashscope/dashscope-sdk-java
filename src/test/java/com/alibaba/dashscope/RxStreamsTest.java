// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.resources.RxStreams;
import io.reactivex.subscribers.TestSubscriber;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RxStreamsTest {

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

  private static final class BlockingStream implements Iterable<String>, AutoCloseable {
    final AtomicBoolean closed = new AtomicBoolean();
    final CountDownLatch readStarted = new CountDownLatch(1);
    final CountDownLatch released = new CountDownLatch(1);

    @Override
    public Iterator<String> iterator() {
      return new Iterator<String>() {
        @Override
        public boolean hasNext() {
          readStarted.countDown();
          try {
            released.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return false;
        }

        @Override
        public String next() {
          throw new NoSuchElementException();
        }
      };
    }

    @Override
    public void close() {
      closed.set(true);
      released.countDown();
    }
  }

  @Test
  void subscriberCancellationStopsProducerAndClosesStream() throws Exception {
    TestStream stream = new TestStream();
    ExecutorService executor = Executors.newCachedThreadPool();
    try {
      TestSubscriber<String> subscriber = new TestSubscriber<>(1);
      RxStreams.fromIterable(() -> stream, executor).subscribe(subscriber);
      subscriber.await(5, TimeUnit.SECONDS);
      subscriber.cancel();

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

  @Test
  void cancellationClosesStreamWhileIteratorIsBlocked() throws Exception {
    BlockingStream stream = new BlockingStream();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      TestSubscriber<String> subscriber = new TestSubscriber<>(1);
      RxStreams.fromIterable(() -> stream, executor).subscribe(subscriber);
      assertTrue(stream.readStarted.await(5, TimeUnit.SECONDS));

      subscriber.cancel();

      assertTrue(stream.released.await(1, TimeUnit.SECONDS));
      assertTrue(stream.closed.get());
    } finally {
      executor.shutdownNow();
    }
  }
}
