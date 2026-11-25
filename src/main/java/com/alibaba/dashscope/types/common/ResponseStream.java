// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.common;

import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.sse.SseStream;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Typed streaming response (design §3): blocking pull iterator over SSE frames mapped to {@code T}.
 * Single-use, {@link AutoCloseable} — always close (try-with-resources) so the underlying
 * connection is released. In-stream error frames throw the mapped exception, or — with {@code
 * raiseOnError(false)} — surface as a final error-carrying item: iteration stops at the error frame
 * and any frames the server sends after it are dropped.
 */
public final class ResponseStream<T> implements Iterable<T>, AutoCloseable {
  private final SseStream stream;
  private final Function<JsonNode, T> mapper;
  private final boolean raiseOnError;
  private boolean iteratorTaken = false;

  public ResponseStream(SseStream stream, Function<JsonNode, T> mapper, boolean raiseOnError) {
    this.stream = stream;
    this.mapper = mapper;
    this.raiseOnError = raiseOnError;
  }

  @Override
  public Iterator<T> iterator() {
    if (iteratorTaken) {
      throw new IllegalStateException("ResponseStream is single-use");
    }
    iteratorTaken = true;
    return new ResponseIterator();
  }

  @Override
  public void close() {
    stream.close();
  }

  private final class ResponseIterator implements Iterator<T> {
    private final Iterator<JsonNode> frames = stream.iterator();
    private T next;
    private boolean done;

    @Override
    public boolean hasNext() {
      while (next == null && !done) {
        if (!frames.hasNext()) {
          done = true;
          break;
        }
        JsonNode frame = frames.next();
        if (JsonSupport.isErrorFrame(frame)) {
          Map<String, String> headers = headersOfStream();
          T errorItem = raiseOnError ? null : mapper.apply(frame);
          if (errorItem == null) {
            // raiseOnError(true), or a mapper that cannot represent the error as an item
            // (e.g. raw TTS audio chunks): throwing is the only way to surface it — never
            // drop the error silently.
            throw Errors.fromBusinessCode(
                JsonSupport.errorCode(frame),
                JsonSupport.errorMessage(frame),
                JsonSupport.requestId(frame, headers),
                JsonSupport.toPlainValue(frame),
                headers);
          }
          // raiseOnError(false): the error-carrying item is terminal — frames the server
          // sends after the error must not be delivered.
          next = errorItem;
          done = true;
          break;
        }
        // A null mapping means the frame carries no item for this resource (e.g. a TTS
        // usage-only SSE frame); skip it.
        next = mapper.apply(frame);
      }
      return next != null;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new java.util.NoSuchElementException();
      }
      T out = next;
      next = null;
      return out;
    }
  }

  private Map<String, String> headersOfStream() {
    Map<String, String> out = new LinkedHashMap<>();
    for (String name : stream.responseHeaders().names()) {
      out.put(name, stream.responseHeaders().get(name));
    }
    return out;
  }
}
