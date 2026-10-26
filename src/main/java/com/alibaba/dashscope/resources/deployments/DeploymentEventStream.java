// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.deployments;

import com.alibaba.dashscope.resources.jobs.JobEventSupport;
import com.alibaba.dashscope.transport.sse.JobEventStream;
import com.alibaba.dashscope.types.deployments.DeploymentEvent;
import java.util.Iterator;

/**
 * Typed stream over {@code /deployments/{id}/stream} frames: an {@code event:error} frame raises
 * (raiseOnError) or surfaces as the final error-carrying event, ending the stream.
 */
public final class DeploymentEventStream implements Iterable<DeploymentEvent>, AutoCloseable {
  private final JobEventStream stream;
  private final boolean raiseOnError;
  private boolean iteratorTaken = false;

  DeploymentEventStream(JobEventStream stream, boolean raiseOnError) {
    this.stream = stream;
    this.raiseOnError = raiseOnError;
  }

  @Override
  public Iterator<DeploymentEvent> iterator() {
    if (iteratorTaken) {
      throw new IllegalStateException("DeploymentEventStream is single-use");
    }
    iteratorTaken = true;
    Iterator<JobEventStream.Frame> frames = stream.iterator();
    return new Iterator<DeploymentEvent>() {
      @Override
      public boolean hasNext() {
        return frames.hasNext();
      }

      @Override
      public DeploymentEvent next() {
        JobEventStream.Frame frame = frames.next();
        if (!frame.isError()) {
          return new DeploymentEvent(JobEventSupport.tryJson(frame.getData()), false);
        }
        if (raiseOnError) {
          throw JobEventSupport.toException(frame);
        }
        return new DeploymentEvent(JobEventSupport.tryJson(frame.getData()), true);
      }
    };
  }

  @Override
  public void close() {
    stream.close();
  }
}
