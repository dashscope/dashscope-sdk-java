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
 * Typed streaming response (design §3): blocking pull iterator over SSE frames mapped to
 * {@code T}. Single-use, {@link AutoCloseable} — always close (try-with-resources) so the
 * underlying connection is released. In-stream error frames throw the mapped exception, or — with
 * {@code raiseOnError(false)} — surface as a final error-carrying item.
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

    @Override
    public boolean hasNext() {
      return frames.hasNext();
    }

    @Override
    public T next() {
      JsonNode frame = frames.next();
      if (raiseOnError && JsonSupport.isErrorFrame(frame)) {
        Map<String, String> headers = headersOfStream();
        throw Errors.fromBusinessCode(
            JsonSupport.errorCode(frame),
            JsonSupport.errorMessage(frame),
            JsonSupport.requestId(frame, headers),
            JsonSupport.toPlainValue(frame),
            headers);
      }
      return mapper.apply(frame);
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
