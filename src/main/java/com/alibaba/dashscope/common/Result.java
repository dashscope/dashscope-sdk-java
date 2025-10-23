// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.google.gson.JsonElement;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class Result {

  /** The request id. */
  private String requestId;

  /** The usage information */
  private JsonElement usage;

  /** The headers of response */
  private Map<String, String> headers;

  /** The HTTP status code from server response */
  private Integer statusCode;

  /** The error code from server response */
  private String code;

  /** The message from server response */
  private String message;

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
