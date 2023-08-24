// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.base;

import io.reactivex.Flowable;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

/** The model service base class */
@Data
@SuperBuilder
public abstract class FullDuplexParamBase {
  /** The model to use. */
  @lombok.NonNull private String model;

  /** The apiKey. */
  private String apiKey;
  /** Open security check */
  @Builder.Default private boolean securityCheck = false;

  public String getApiKey() {
    return apiKey;
  }

  /** The extra parameters. */
  @Singular protected Map<String, Object> parameters;

  public String getModel() {
    return model;
  }
  /**
   * The service parameters.
   *
   * @return The key/value parameters
   */
  public abstract Map<String, Object> getParameters();

  /**
   * Get the resources
   *
   * @return
   */
  public abstract Object getResources();

  /**
   * Get the stream input data, only for websocket stream in and duplex mode. For binary data
   * support: byte[] and ByteBuffer Otherwise should be String or Json serializable Object. For
   * websocket stream in and duplex service must implement.
   *
   * @return The stream input data.
   */
  public abstract Flowable<Object> getStreamingData();

  public void setModel(String model) {
    this.model = model;
    parameters.put("model", model);
  }
}
