// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.finetunes;

import com.alibaba.dashscope.resources.jobs.JobEventSupport;
import com.alibaba.dashscope.transport.sse.JobEventStream;
import com.alibaba.dashscope.types.finetunes.FineTuneEvent;
import java.util.Iterator;

/**
 * Typed stream over {@code /fine-tunes/{id}/stream} frames: an {@code event:error} frame raises
 * (raiseOnError) or surfaces as the final error-carrying event, ending the stream.
 */
public final class FineTuneEventStream implements Iterable<FineTuneEvent>, AutoCloseable {
  private final JobEventStream stream;
  private final boolean raiseOnError;
  private boolean iteratorTaken = false;

  FineTuneEventStream(JobEventStream stream, boolean raiseOnError) {
    this.stream = stream;
    this.raiseOnError = raiseOnError;
  }

  @Override
  public Iterator<FineTuneEvent> iterator() {
    if (iteratorTaken) {
      throw new IllegalStateException("FineTuneEventStream is single-use");
    }
    iteratorTaken = true;
    Iterator<JobEventStream.Frame> frames = stream.iterator();
    return new Iterator<FineTuneEvent>() {
      @Override
      public boolean hasNext() {
        return frames.hasNext();
      }

      @Override
      public FineTuneEvent next() {
        JobEventStream.Frame frame = frames.next();
        if (!frame.isError()) {
          return new FineTuneEvent(JobEventSupport.tryJson(frame.getData()), false);
        }
        if (raiseOnError) {
          throw JobEventSupport.toException(frame);
        }
        return new FineTuneEvent(JobEventSupport.tryJson(frame.getData()), true);
      }
    };
  }

  @Override
  public void close() {
    stream.close();
  }
}
