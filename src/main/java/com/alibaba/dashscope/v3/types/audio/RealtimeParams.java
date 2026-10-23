// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

/**
 * Parameters for {@code audio.realtime().connect} (audio design §1.3). The realtime protocol is
 * pure JSON text frames (audio base64-encoded, no task_id), so every session owns a dedicated
 * socket — no {@code WsConnection} sharing.
 */
public final class RealtimeParams {
  private final String model;
  /**
   * Explicit realtime endpoint override (2.x {@code OmniRealtimeParam.url} parity). When unset, the
   * URL is derived from the resolved HTTP base url; the generic ws base-url override does NOT apply
   * to realtime (audio design §2).
   */
  private final String url;

  private RealtimeParams(Builder b) {
    this.model = b.model;
    this.url = b.url;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public String getUrl() {
    return url;
  }

  public static final class Builder {
    private String model;
    private String url;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public RealtimeParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      return new RealtimeParams(this);
    }
  }
}
