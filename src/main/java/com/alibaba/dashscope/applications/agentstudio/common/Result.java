// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.common;

import com.alibaba.dashscope.applications.agentstudio.exception.ApiException;
import com.alibaba.dashscope.applications.agentstudio.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.applications.agentstudio.protocol.NetworkResponse;
import com.alibaba.dashscope.applications.agentstudio.protocol.Protocol;
import com.google.gson.JsonElement;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Base type for HTTP results carrying response metadata. */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class Result {

  /** The request id. */
  private String requestId;

  /** The usage information */
  private JsonElement usage;

  /** The headers of response */
  private Map<String, Object> headers;

  /** The HTTP status code from server response */
  private Integer statusCode;

  /** The error code from server response */
  private String code;

  /** The message from server response */
  private String message;

  public String getRequestIdOrUnknown() {
    return requestId == null || requestId.isEmpty() ? "unknown" : requestId;
  }

  /**
   * Load data from the server output.
   *
   * @param protocol The protocol, please check the `Protocol` enum.
   * @param response The response.
   * @param <T> Object extends `Result`
   * @return The result.
   * @throws ApiException Failed, possibly due to a data error.
   */
  protected abstract <T extends Result> T fromResponse(Protocol protocol, NetworkResponse response)
      throws ApiException;

  public abstract <T extends Result> T fromResponse(
      Protocol protocol, NetworkResponse response, boolean isFlattenResult) throws ApiException;

  public abstract <T extends Result> T fromResponse(
      Protocol protocol, NetworkResponse response, boolean isFlattenResult, HalfDuplexRequest req)
      throws ApiException;
}
