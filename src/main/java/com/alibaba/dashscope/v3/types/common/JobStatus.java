// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.common;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a state transition that reports nothing but the new state (Python v2 parity). Shared by
 * {@code fineTunes().cancel} / {@code fineTunes().delete} / {@code deployments().delete}, whose
 * responses are identical on the wire.
 */
public final class JobStatus extends ResponseBase {
  @JsonProperty("status")
  private String status;

  public String getStatus() {
    return status;
  }
}
