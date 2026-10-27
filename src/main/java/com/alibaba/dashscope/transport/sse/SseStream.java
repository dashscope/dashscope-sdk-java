// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport.sse;

import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.PublicErrorCode;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.transport.EncryptionConfig;
import com.alibaba.dashscope.transport.EncryptionUtils;
import com.alibaba.dashscope.transport.JsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import okhttp3.Response;

/**
 * Blocking, pull-model SSE parser (design §6). Single-use: yields each {@code data:} frame as a
 * parsed {@link JsonNode}; {@code [DONE]} or EOF ends the stream. No background threads — the
 * caller's thread drives reads. Frame syntax: data lines join with \n, blank line dispatches,
 * {@code event:}/{@code id:}/{@code retry:} and comment lines are ignored.
 */
public final class SseStream implements AutoCloseable, Iterable<JsonNode> {
  private static final String DONE = "[DONE]";

  private final Response response;
  private final EncryptionConfig encryptionConfig;
  private boolean closed = false;
  private boolean iteratorTaken = false;

  public SseStream(Response response) {
    this(response, null);
  }

  public SseStream(Response response, EncryptionConfig encryptionConfig) {
    this.response = response;
    this.encryptionConfig = encryptionConfig;
  }

  /** Response headers of the opening HTTP exchange (request id lookup etc.). */
  public okhttp3.Headers responseHeaders() {
    return response.headers();
  }

  @Override
  public Iterator<JsonNode> iterator() {
    if (iteratorTaken) {
      throw new IllegalStateException("SseStream is single-use");
    }
    iteratorTaken = true;
    return new FrameIterator();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      response.close();
    }
  }

  private final class FrameIterator implements Iterator<JsonNode> {
    private String nextData;
    private boolean pendingLoaded = false;
    private boolean finished = false;
    private final StringBuilder dataBuffer = new StringBuilder();

    @Override
    public boolean hasNext() {
      if (!pendingLoaded && !finished) {
        loadNext();
      }
      return nextData != null;
    }

    @Override
    public JsonNode next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      String payload = nextData;
      nextData = null;
      pendingLoaded = false;
      JsonNode frame = JsonSupport.readTree(payload);
      try {
        return EncryptionUtils.decryptOutput(frame, encryptionConfig);
      } catch (RuntimeException e) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : response.headers().names()) {
          headers.put(name, response.header(name));
        }
        throw Errors.fromCatalogEntry(
            PublicErrorCode.INTERNAL_ERROR,
            "failed to decrypt SSE output: " + e.getMessage(),
            JsonSupport.requestId(frame, headers),
            JsonSupport.toPlainValue(frame),
            headers,
            e);
      }
    }

    private void loadNext() {
      while (!finished) {
        String line;
        try {
          line = response.body().source().readUtf8Line();
        } catch (IOException e) {
          finished = true;
          closeQuietly();
          throw toException(e);
        }
        if (line == null) {
          finished = true;
          closeQuietly();
          String trailing = dataBuffer.toString();
          dataBuffer.setLength(0);
          if (!trailing.isEmpty() && !DONE.equals(trailing.trim())) {
            nextData = trailing;
            pendingLoaded = true;
          }
          return;
        }
        if (line.isEmpty()) {
          // blank line: dispatch frame
          String frame = dataBuffer.toString();
          dataBuffer.setLength(0);
          if (frame.isEmpty()) {
            continue;
          }
          if (DONE.equals(frame.trim())) {
            finished = true;
            closeQuietly();
            return;
          }
          nextData = frame;
          pendingLoaded = true;
          return;
        }
        if (line.startsWith(":")) {
          continue;
        }
        if (line.startsWith("data:")) {
          String value = line.substring(5);
          if (value.startsWith(" ")) {
            value = value.substring(1);
          }
          if (dataBuffer.length() > 0) {
            dataBuffer.append('\n');
          }
          dataBuffer.append(value);
        }
        // event:/id:/retry: fields are intentionally ignored
      }
    }

    private void closeQuietly() {
      close();
    }
  }

  private static RuntimeException toException(IOException e) {
    if (e instanceof InterruptedIOException) {
      return Errors.timeout(SdkErrorCode.SDK_TIMEOUT, "reading SSE stream: " + e.getMessage(), e);
    }
    return Errors.connection(
        SdkErrorCode.SDK_CONNECTION_ERROR, "reading SSE stream: " + e.getMessage(), e);
  }
}
