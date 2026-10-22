// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.models;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One model entry (Python v2 {@code Model} parity). Returned on its own by {@code get} and nested
 * in {@link ModelList#getModels()}; the envelope is filled in on the standalone result and stays
 * empty on the nested copies, because the server reports it once per response rather than once
 * per item.
 */
public final class Model extends ResponseBase {
  @JsonProperty("model")
  private String model;

  @JsonProperty("source")
  private String source;

  @JsonProperty("provider")
  private String provider;

  // The wire field is "opensource"; the public name is spelled properly.
  @JsonProperty("opensource")
  @JsonAlias("open_source")
  private Boolean openSource;

  public String getModel() {
    return model;
  }

  public String getSource() {
    return source;
  }

  public String getProvider() {
    return provider;
  }

  public Boolean getOpenSource() {
    return openSource;
  }
}
