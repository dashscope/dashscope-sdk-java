// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.deployments;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One SSE frame from {@code deployments().streamEvents} (Python v2 {@code DeploymentEvent}
 * parity). {@code output} is the frame payload parsed as JSON when possible (a plain string
 * arrives as a textual node); {@code isError} marks the terminal {@code event:error} frame.
 */
public final class DeploymentEvent {
  private final JsonNode output;
  private final boolean error;

  public DeploymentEvent(JsonNode output, boolean error) {
    this.output = output;
    this.error = error;
  }

  public JsonNode getOutput() {
    return output;
  }

  public boolean isError() {
    return error;
  }
}
