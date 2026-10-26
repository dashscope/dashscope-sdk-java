// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.transport.sse;

import com.alibaba.dashscope.v3.errors.Errors;
import com.alibaba.dashscope.v3.errors.SdkErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import okhttp3.Response;

/**
 * Blocking, pull-model parser for the job-event SSE framing (fine-tunes/deployments streams, 1.x
 * StreamEventMixin lineage): an {@code event:error} line flips the error flag, {@code status:}
 * carries the mapped HTTP status (optional), and each {@code data:} line is one frame; an error
 * frame ends the stream. Unlike {@link SseStream}, data lines are not joined across blank lines.
 * Single-use, no background threads.
 */
public final class JobEventStream implements AutoCloseable, Iterable<JobEventStream.Frame> {
  private final Response response;
  private boolean closed = false;
  private boolean iteratorTaken = false;

  public JobEventStream(Response response) {
    this.response = response;
  }

  /** One {@code data:} line plus the stream state accumulated when it arrived. */
  public static final class Frame {
    private final boolean error;
    private final Integer statusCode;
    private final String data;

    Frame(boolean error, Integer statusCode, String data) {
      this.error = error;
      this.statusCode = statusCode;
      this.data = data;
    }

    public boolean isError() {
      return error;
    }

    /** The frame's {@code status:} line; null when the wire omitted it (business failure). */
    public Integer getStatusCode() {
      return statusCode;
    }

    public String getData() {
      return data;
    }
  }

  @Override
  public Iterator<Frame> iterator() {
    if (iteratorTaken) {
      throw new IllegalStateException("JobEventStream is single-use");
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

  private final class FrameIterator implements Iterator<Frame> {
    private final BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8));
    private Frame next;
    private boolean finished = false;
    private boolean error = false;
    private Integer statusCode;

    @Override
    public boolean hasNext() {
      if (next == null && !finished) {
        loadNext();
      }
      return next != null;
    }

    @Override
    public Frame next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      Frame out = next;
      next = null;
      return out;
    }

    private void loadNext() {
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.isEmpty()) {
            continue;
          }
          if (line.startsWith("event:error")) {
            error = true;
          } else if (line.startsWith("status:")) {
            statusCode = Integer.valueOf(line.substring("status:".length()).trim());
          } else if (line.startsWith("data:")) {
            next = new Frame(error, statusCode, line.substring("data:".length()).trim());
            if (error) {
              finished = true;
            }
            return;
          }
        }
        finished = true;
      } catch (InterruptedIOException e) {
        throw Errors.timeout(SdkErrorCode.SDK_TIMEOUT, "stream read: " + e.getMessage(), e);
      } catch (IOException e) {
        throw Errors.connection(
            SdkErrorCode.SDK_CONNECTION_ERROR, "stream read: " + e.getMessage(), e);
      }
    }
  }
}
