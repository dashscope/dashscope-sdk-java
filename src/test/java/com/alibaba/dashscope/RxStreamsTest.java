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
import org.junit.jupiter.api.Test;

class RxStreamsTest {

  private static final class GatedStream implements Iterable<String>, AutoCloseable {
    final AtomicBoolean closed = new AtomicBoolean();
    final CountDownLatch firstItemProduced = new CountDownLatch(1);
    final CountDownLatch gate = new CountDownLatch(1);

    @Override
    public Iterator<String> iterator() {
      return new Iterator<String>() {
        boolean first = true;

        @Override
        public boolean hasNext() {
          if (first) {
            first = false;
            firstItemProduced.countDown();
            return true;
          }
          // Block before the second item: with BackpressureStrategy.BUFFER the producer would
          // otherwise race to EOF in milliseconds, so only the cancellation path closing the
          // stream (which opens the gate) can let this iteration finish.
          try {
            gate.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return false;
        }

        @Override
        public String next() {
          return "item-0";
        }
      };
    }

    @Override
    public void close() {
      closed.set(true);
      gate.countDown();
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
    GatedStream stream = new GatedStream();
    ExecutorService executor = Executors.newCachedThreadPool();
    try {
      TestSubscriber<String> subscriber = new TestSubscriber<>(1);
      RxStreams.fromIterable(() -> stream, executor).subscribe(subscriber);
      // Land the cancellation after the producer has pulled the first item, i.e. while it is
      // iterating the upstream, not before the stream opens.
      assertTrue(stream.firstItemProduced.await(5, TimeUnit.SECONDS));

      subscriber.cancel();

      // The producer is blocked on the gate, so cancellation must close the upstream stream
      // (opening the gate) instead of leaving the producer hanging on it.
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
