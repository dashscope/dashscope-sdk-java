// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.finetunes;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One SSE frame from {@code fineTunes().streamEvents} (Python v2 {@code FineTuneEvent} parity).
 * {@code output} is the frame payload parsed as JSON when possible (a plain string arrives as a
 * textual node); {@code isError} marks the terminal {@code event:error} frame.
 */
public final class FineTuneEvent {
  private final JsonNode output;
  private final boolean error;

  public FineTuneEvent(JsonNode output, boolean error) {
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
